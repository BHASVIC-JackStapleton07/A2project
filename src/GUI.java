import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class GUI extends JPanel {
    // References
    private final Simulator simulator;

    // Variables
    private final int CELL_SIZE; // Cell size in pixels
    private static int frames = 0;
    private static long oldTime = System.nanoTime();
    private static double fps = 0;

    // Components
    static JFrame frame = new JFrame("Fluid Sim"); // Frame for program
    private JPanel gridPanel; // Grid renderer
    private JPanel mainPanel; // Main display panel
    private JButton playButton;
    private JButton slowButton;
    private JButton fastButton;

    // Beautiful constructor
    public GUI(Simulator simulator) {
        // Initialise simulator
        this.simulator = simulator;
        CELL_SIZE = simulator.gridHeight / 15;

        // Layout
        JPanel center = new JPanel(new FlowLayout());

        // Create and add grid panel
        createGridPanel();
        center.add(gridPanel);
        add(center);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(slowButton); buttonPanel.add(playButton); buttonPanel.add(fastButton);

        // Create controls text box
        JTextArea controlsText = new JTextArea(5, 20);
        controlsText.setText("Controls:\n"
                + "Left Click: Repel\n"
                + "Right Click: Attract\n"
                + "Space: Pause/Play\n"
                + "E: Increase Speed\n"
                + "Q: Decrease Speed\n"
                + "Tab: Menu\n"
                + "V: Toggle Arrows\n");
        controlsText.setEditable(false);
        controlsText.setLineWrap(true);
        controlsText.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(controlsText);

        // Create control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        controlPanel.add(scrollPane, BorderLayout.WEST); controlPanel.add(buttonPanel, BorderLayout.NORTH);
        frame.add(controlPanel, BorderLayout.SOUTH);

        // Create update timer
        Timer timer = new Timer(32, e -> {
            // Step simulation and update GUI
            simulator.stepSimulation();
            update();
        });
        timer.start();

        // Button functionalities
        playButton.addActionListener(e -> {
            // Pause/Play simulation
            simulator.toggleSimulation();
            // Switch button text
            if (simulator.isPaused) {
                playButton.setText("Play");
            } else {
                playButton.setText("Pause");
            }
        });
        slowButton.addActionListener(e -> {
            // Disable if too slow
            fastButton.setEnabled(simulator.delay > 2);
            slowButton.setEnabled(simulator.delay < 320);
            // Double the delay and set it
            simulator.delay *= 2;
            timer.setDelay(simulator.delay);
        });
        fastButton.addActionListener(e -> {
            // Disable if too fast
            fastButton.setEnabled(simulator.delay > 2);
            slowButton.setEnabled(simulator.delay < 320);
            // Halve the delay and set it
            simulator.delay /= 2;
            timer.setDelay(simulator.delay);
        });

        // Key bindings
        AbstractAction toggleSimulationAction = new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                // Pause/Play simulation
                simulator.toggleSimulation();
                // Switch button text
                if (simulator.isPaused) {
                    playButton.setText("Play");
                } else {
                    playButton.setText("Pause");
                }
            }
        };
        AbstractAction slowDownAction = new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                // Disable if too slow
                fastButton.setEnabled(simulator.delay > 2);
                slowButton.setEnabled(simulator.delay < 320);
                // Double the delay and set it
                if (simulator.delay < 320) {
                    simulator.delay *= 2;
                    timer.setDelay(simulator.delay);
                }
            }
        };
        AbstractAction speedUpAction = new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                // Disable if too fast
                fastButton.setEnabled(simulator.delay > 2);
                slowButton.setEnabled(simulator.delay < 320);
                // Halve the delay and set it
                if (simulator.delay > 2) {
                    simulator.delay /= 2;
                    timer.setDelay(simulator.delay);
                }
            }
        };
        AbstractAction toggleArrowsAction = new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                simulator.showVectorArrows = !simulator.showVectorArrows;
            }
        };
        // Define key bindings
        InputMap inputMap = center.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = center.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke("SPACE"), "toggleSimulation");
        actionMap.put("toggleSimulation", toggleSimulationAction);
        inputMap.put(KeyStroke.getKeyStroke("E"), "speedUp");
        actionMap.put("speedUp", speedUpAction);
        inputMap.put(KeyStroke.getKeyStroke("Q"), "slowDown");
        actionMap.put("slowDown", slowDownAction);
        inputMap.put(KeyStroke.getKeyStroke("V"), "toggleArrows");
        actionMap.put("toggleArrows", toggleArrowsAction);

        // Focys on main panel
        mainPanel.setFocusable(true); mainPanel.requestFocusInWindow();
        playButton.setFocusable(false); slowButton.setFocusable(false); fastButton.setFocusable(false);

        // Dark colour theme
        frame.setBackground(Color.darkGray);
        center.setBackground(Color.darkGray);
        mainPanel.setBackground(Color.darkGray);
        gridPanel.setBackground(Color.darkGray);
        controlPanel.setBackground(Color.darkGray);
        buttonPanel.setBackground(Color.darkGray);
        controlsText.setBackground(Color.lightGray);
        scrollPane.setBackground(Color.darkGray);
    }

    // Create panels
    private void createGridPanel() {
        gridPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // Call super method
                super.paintComponent(g);

                // Get grid
                Grid grid = simulator.getGrid();

                // Render each cell
                for (int x = 0; x < grid.getWidth(); x++) {
                    for (int y = 0; y < grid.getHeight(); y++) {
                        Cell cell = grid.getCell(x, y);

                        // Visualize cell density
                        float max = simulator.maxDensity;
                        float densityValue = (float) cell.density; // Get density value
                        densityValue = Math.max(0, Math.min(max, densityValue)); // Clamp value
                        densityValue  = densityValue / max; // Normalize value

                        g.setColor(new Color(densityValue, densityValue, densityValue)); // Set colour

                        // Draw cell at correct position and size
                        g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);


                        // Draw velocity arrows
                        if (simulator.showVectorArrows == true
                                && x % simulator.arrowSpacing == 0
                                && y % simulator.arrowSpacing == 0) { // Arrow spacing
                            // Skip boundaries
                            if (cell.state == 0) { continue; }

                            // Clamp velocities
                            double xVel = Math.max(Math.min(cell.velocityX, 30), -30);
                            double yVel = Math.max(Math.min(cell.velocityY, 30), -30);

                            // Skip still cells
                            if (cell.velocityX != 0 && cell.velocityY != 0) {
                                // Calculate start and end coordinates
                                int startX = x * CELL_SIZE + CELL_SIZE / 2;
                                int startY = y * CELL_SIZE + CELL_SIZE / 2;
                                int endX = (int) (startX + simulator.arrowSpacing * xVel / 3);
                                int endY = (int) (startY + simulator.arrowSpacing * yVel / 3);

                                g.setColor(Color.RED); // Set colour

                                // Draw arrow
                                drawArrow((Graphics2D) g, startX, startY, endX, endY);
                            }
                        }
                    }
                }

                // FPS calculation
                frames++;
                long currentTime = System.nanoTime();
                double dt = (currentTime - oldTime) / 1e9;
                if (dt >= 0.5) { // Update FPS every second
                    fps = frames / dt;
                    frames = 0; // Reset frame counter
                    oldTime = currentTime; // Reset time counter
                }
                // Draw FPS
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 16));
                g.drawString(String.format("FPS: %.2f", fps), 10, 20);
            }
        };

        // Set preferred size
        int gridWidth = simulator.getGrid().getWidth() * CELL_SIZE;
        int gridHeight = simulator.getGrid().getHeight() * CELL_SIZE;
        gridPanel.setPreferredSize(new Dimension(gridWidth, gridHeight));
    }

    private void drawArrow(Graphics2D g, int xStart, int yStart, int xEnd, int yEnd) {
        // Enable Anti-Aliasing
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Increase thickness
        g2d.setStroke(new BasicStroke(1.5f));

        // Draw line
        g2d.drawLine(xStart, yStart, xEnd, yEnd);

        // Draw arrowhead
        Polygon polygon = new Polygon();

        // Calculating arrow dimensions
        double size = CELL_SIZE;
        double dx = xStart - xEnd;
        double dy = yStart - yEnd;
        double angle = Math.atan2(dx, dy);

        // Plotting points
        polygon.addPoint(xEnd, yEnd);
        polygon.addPoint(xEnd + (int) (size + Math.sin(angle + Math.PI / 6)), yEnd + (int) (size * Math.cos(angle + Math.PI / 6)));
        polygon.addPoint(xEnd + (int) (size + Math.sin(angle - Math.PI / 6)), yEnd + (int) (size * Math.cos(angle - Math.PI / 6)));
        polygon.addPoint(xEnd, yEnd);

        // Fill polygon
        g2d.fillPolygon(polygon);
    }

    // Update method
    public void update() {
        gridPanel.repaint();
    }

    // Set up the GUI
    public static void createAndShowGUI(Simulator simulator) {
        // Create new JFrame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);

        // Create GUI and add Simulator
        GUI gui = new GUI(simulator);
        frame.add(gui);
        frame.setLocationRelativeTo(null); // Set location to centre
        frame.setVisible(true); // Set visible
    }
}
