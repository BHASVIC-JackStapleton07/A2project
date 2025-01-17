import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class GUI extends JPanel {
    // References
    private final Simulator simulator;

    // Variables
    private final int CELL_SIZE; // Cell size in pixels

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

        // Create control panel
        createControlPanel();

        // Button functionalities
        playButton.addActionListener(e -> {
            simulator.toggleSimulation();
            if (simulator.isPaused) {
                playButton.setText("Pause");
            } else {
                playButton.setText("Play");
            }
        });
        slowButton.addActionListener(e -> {
            if (simulator.delay >= 160) {
                slowButton.setEnabled(false);
            } else {
                slowButton.setEnabled(true);
            }
            simulator.delay += 4;
        });
        fastButton.addActionListener(e -> {
            if (simulator.delay > 2) {
                fastButton.setEnabled(false);
            } else {
                fastButton.setEnabled(true);
            }
            simulator.delay -= 2;
        });
    }

    // Create grid panel
    private void createGridPanel() {
        gridPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); //paint properly

                //obtain grid
                Grid grid = simulator.getGrid();

                // Render each cell
                for (int x = 0; x < grid.getWidth(); x++) {
                    for (int y = 0; y < grid.getHeight(); y++) {
                        Cell cell = grid.getCell(x, y);

                        // Visualize cell density
                        float max = simulator.maxDensity;
                        float densityValue = (float) cell.density; //get density
                        densityValue = Math.max(0, Math.min(max, densityValue)); //clamps between 1 and 0
                        densityValue  = densityValue / max;

                        g.setColor(new Color(densityValue, densityValue, densityValue));

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
            }
        };

        //calculate gridpanel size
        int gridWidth = simulator.getGrid().getWidth() * CELL_SIZE;
        int gridHeight = simulator.getGrid().getHeight() * CELL_SIZE;
        gridPanel.setPreferredSize(new Dimension(gridWidth, gridHeight));
    }

    // Create control panel
    private void createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        // Add buttons
        controlPanel.add(slowButton);
        controlPanel.add(playButton);
        controlPanel.add(fastButton);
        // Add control panel to the main frame
        frame.add(controlPanel, BorderLayout.SOUTH);
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
        frame.setLocationRelativeTo(null); //set frame to centre screen
        frame.setVisible(true); //set visible

        // Update
        // Define 60fps timer
        Timer timer = new Timer(simulator.delay, e -> {
            simulator.stepSimulation();
            gui.update();
        });
        timer.start();
    }
}
