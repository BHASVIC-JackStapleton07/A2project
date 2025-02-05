import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GUI extends JPanel {
    // References
    private final Simulator simulator;

    // Variables
    private final int CELL_SIZE; // Cell size in pixels
    private static int frames = 0;
    private static long oldTime = System.nanoTime();
    private static double fps = 0;
    String baseHexCode = "#169BFF";
    int colourOption = 1;
    private boolean isMousePressed = false;

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

        // Mouse listener
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Get mouse press
                isMousePressed = true;
                applyMouseEffect(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Get mouse release
                isMousePressed = false;
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // Get mouse drag
                applyMouseEffect(e);
            }
        });

        // Mouse input timer
        Timer mouseTimer = new Timer(10, (ActionEvent e) -> {
            if (isMousePressed) {
                // Apply mouse effect if pressed
                Point mousePos = getMousePosition();
                if (mousePos != null) {
                    int x = mousePos.x / CELL_SIZE;
                    int y = mousePos.y / CELL_SIZE;

                    // If in bounds
                    if (x >= 0 && x < simulator.getGrid().getWidth() &&
                            y >= 0 && y < simulator.getGrid().getHeight()) {

                        int button = MouseEvent.BUTTON1; // Default to left click
                        simulator.addMouseInfluence(x, y, button);
                    }
                }
            }
            repaint();
        });
        mouseTimer.start();

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
        controlsText.setText("""
                Controls:
                Left Click: Repel
                Right Click: Attract
                R: Reset
                Space: Pause/Play
                E: Increase Speed
                Q: Decrease Speed
                F: Fill Random
                V: Toggle Arrows
                """);
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
                playButton.setText("▶");
            } else {
                playButton.setText("||");
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

        // Menu bar
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Settings"));
        // Gravity settings
        JLabel gravityLabel = new JLabel("Gravity:");
        JSlider gravitySlider = new JSlider(JSlider.HORIZONTAL, -10, 10, 0);
        gravitySlider.setMajorTickSpacing(2); gravitySlider.setPaintTicks(true); gravitySlider.setPaintLabels(true);
        gravitySlider.addChangeListener(e -> {
            int value = gravitySlider.getValue();
            simulator.gravity = value;
        });
        // Viscosity settings
        JLabel viscosityLabel = new JLabel("Viscosity:");
        JSlider viscositySlider = new JSlider(JSlider.HORIZONTAL, 0, 10, 0);
        viscositySlider.setMajorTickSpacing(2); viscositySlider.setPaintTicks(true); viscositySlider.setPaintLabels(true);
        viscositySlider.addChangeListener(e -> {
            int value = viscositySlider.getValue();
            simulator.viscosity = value;
        });
        // Over-relaxation settings
        JLabel overRelaxationLabel = new JLabel("Over-Relaxation:");
        JSlider overRelaxationSlider = new JSlider(JSlider.HORIZONTAL, 10, 19, 15);
        overRelaxationSlider.setMajorTickSpacing(2); overRelaxationSlider.setPaintTicks(true); overRelaxationSlider.setPaintLabels(true);
        overRelaxationSlider.addChangeListener(e -> {
            int value = overRelaxationSlider.getValue();
            simulator.overrelaxation = (double) value / 10;
        });
        // Arrow spacing settings
        JLabel arrowSpacingLabel = new JLabel("Arrow Spacing:");
        JSlider arrowSpacingSlider = new JSlider(JSlider.HORIZONTAL, 1, 20, 6);
        arrowSpacingSlider.setMajorTickSpacing(2); arrowSpacingSlider.setPaintTicks(true); arrowSpacingSlider.setPaintLabels(true);
        arrowSpacingSlider.setEnabled(simulator.showVectorArrows);
        arrowSpacingSlider.addChangeListener(e -> {
            int value = arrowSpacingSlider.getValue();
            simulator.arrowSpacing = value;
        });
        // Arrow show settings
        JCheckBox showArrowsCheckBox = new JCheckBox("Show Velocity Arrows");
        showArrowsCheckBox.setSelected(simulator.showVectorArrows);
        showArrowsCheckBox.addActionListener(e -> {
            simulator.showVectorArrows = showArrowsCheckBox.isSelected();
            arrowSpacingSlider.setEnabled(showArrowsCheckBox.isSelected());
        });
        // Colour Rendering settings
        JLabel colourLabel = new JLabel("Colour Rendering:");
        String[] colours = {"Red", "Orange", "Yellow", "Green", "Blue", "Light Blue", "Purple", "Pink", "White"};
        JComboBox<String> colourComboBox = new JComboBox<String>(colours);
        colourComboBox.setSelectedItem("Light Blue");
        colourComboBox.addActionListener(e -> {
            String colour = (String) colourComboBox.getSelectedItem();
            switch (colour) {
                case "Red":
                    baseHexCode = "#FF0000";
                    break;
                case "Orange":
                    baseHexCode = "#FFA500";
                    break;
                case "Yellow":
                    baseHexCode = "#FFFF00";
                    break;
                case "Green":
                    baseHexCode = "#00FF00";
                    break;
                case "Blue":
                    baseHexCode = "#0000FF";
                    break;
                case "Purple":
                    baseHexCode = "#800080";
                    break;
                case "Pink":
                    baseHexCode = "#FFC0CB";
                    break;
                case "White":
                    baseHexCode = "#FFFFFF";
                    break;
                case "Light Blue":
                    baseHexCode = "#169BFF";
                    break;
                default:
                    baseHexCode = "#FFFFFF";
            }
        });
        // Rendering Option settings
        JLabel colourOptionLabel = new JLabel("Rendering Option:");
        String[] options = {"Density", "Velocity", "Pressure"};
        JComboBox<String> optionComboBox = new JComboBox<String>(options);
        optionComboBox.setSelectedItem("Density");
        optionComboBox.addActionListener(e -> {
            String option = (String) optionComboBox.getSelectedItem();
            switch (option) {
                case "Density":
                    colourOption = 1;
                    break;
                case "Velocity":
                    colourOption = 2;
                    break;
                case "Pressure":
                    colourOption = 3;
                    break;
                default:
                    colourOption = 1;
            }
        });
        // Leak density settings
        JCheckBox leakDensityCheckBox = new JCheckBox("Leak Density");
        leakDensityCheckBox.setSelected(simulator.leakDensity);
        leakDensityCheckBox.addActionListener(e -> {
            simulator.leakDensity = leakDensityCheckBox.isSelected();
        });

        // Tap settings
        // Tap 1
        JLabel tap1Label = new JLabel("Tap 1 Settings:");
        JLabel tap1XLabel = new JLabel("X:");
        JSlider tap1XSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 1);
        tap1XSlider.setMajorTickSpacing(10); tap1XSlider.setPaintTicks(true); tap1XSlider.setPaintLabels(true);
        tap1XSlider.addChangeListener(e -> {
            int x = tap1XSlider.getValue();
            simulator.tap1X = x;
        });
        JLabel tap1YLabel = new JLabel("Y:");
        JSlider tap1YSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
        tap1YSlider.setMajorTickSpacing(10); tap1YSlider.setPaintTicks(true); tap1YSlider.setPaintLabels(true);
        tap1YSlider.addChangeListener(e -> {
            int y = tap1YSlider.getValue();
            simulator.tap1Y = y;
        });
        JLabel tap1SpeedLabel = new JLabel("Speed:");
        JSlider tap1SpeedSlider = new JSlider(JSlider.HORIZONTAL, 0, 75, 50);
        tap1SpeedSlider.setMajorTickSpacing(15); tap1SpeedSlider.setPaintTicks(true); tap1SpeedSlider.setPaintLabels(true);
        tap1SpeedSlider.addChangeListener(e -> {
            int speed = tap1SpeedSlider.getValue();
            simulator.tap1Speed = speed;
        });
        JLabel tap1DirectionLabel = new JLabel("Direction:");
        Character[] choices = {'r', 'd', 'u', 'l'};
        JComboBox<Character> tap1DirectionComboBox = new JComboBox<Character>(choices);
        tap1DirectionComboBox.addActionListener(e -> {
            char direction = (char) tap1DirectionComboBox.getSelectedItem();
            simulator.tap1Dir = direction;
        });
        JLabel tap1OnButton = new JLabel("On:");
        JCheckBox tap1OnCheckBox = new JCheckBox();
        tap1OnCheckBox.setSelected(simulator.tap1On);
        tap1OnCheckBox.addActionListener(e -> {
            simulator.tap1On = tap1OnCheckBox.isSelected();
            tap1XSlider.setEnabled(tap1OnCheckBox.isSelected());
            tap1YSlider.setEnabled(tap1OnCheckBox.isSelected());
            tap1SpeedSlider.setEnabled(tap1OnCheckBox.isSelected());
            tap1DirectionComboBox.setEnabled(tap1OnCheckBox.isSelected());
        });
        // Tap 2 Settings
        JLabel tap2Label = new JLabel("Tap 2 Settings:");
        JLabel tap2XLabel = new JLabel("X:");
        JSlider tap2XSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
        tap2XSlider.setMajorTickSpacing(10); tap2XSlider.setPaintTicks(true); tap2XSlider.setPaintLabels(true);
        tap2XSlider.addChangeListener(e -> {
            int x = tap2XSlider.getValue();
            simulator.tap2X = x;
        });
        JLabel tap2YLabel = new JLabel("Y:");
        JSlider tap2YSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
        tap2YSlider.setMajorTickSpacing(10); tap2YSlider.setPaintTicks(true); tap2YSlider.setPaintLabels(true);
        tap2YSlider.addChangeListener(e -> {
            int y = tap2YSlider.getValue();
            simulator.tap2Y = y;
        });
        JLabel tap2SpeedLabel = new JLabel("Speed:");
        JSlider tap2SpeedSlider = new JSlider(JSlider.HORIZONTAL, 0, 75, 50);
        tap2SpeedSlider.setMajorTickSpacing(15); tap2SpeedSlider.setPaintTicks(true); tap2SpeedSlider.setPaintLabels(true);
        tap2SpeedSlider.addChangeListener(e -> {
            int speed = tap2SpeedSlider.getValue();
            simulator.tap2Speed = speed;
        });
        JLabel tap2DirectionLabel = new JLabel("Direction:");
        JComboBox<Character> tap2DirectionComboBox = new JComboBox<Character>(choices);
        tap2DirectionComboBox.addActionListener(e -> {
            char direction = (char) tap2DirectionComboBox.getSelectedItem();
            simulator.tap2Dir = direction;
        });
        JLabel tap2OnButton = new JLabel("On:");
        JCheckBox tap2OnCheckBox = new JCheckBox();
        tap2OnCheckBox.setSelected(simulator.tap2On);
        tap2OnCheckBox.addActionListener(e -> {
            simulator.tap2On = tap2OnCheckBox.isSelected();
            tap2XSlider.setEnabled(tap2OnCheckBox.isSelected());
            tap2YSlider.setEnabled(tap2OnCheckBox.isSelected());
            tap2SpeedSlider.setEnabled(tap2OnCheckBox.isSelected());
            tap2DirectionComboBox.setEnabled(tap2OnCheckBox.isSelected());
        });
        tap2XSlider.setEnabled(simulator.tap2On); tap2YSlider.setEnabled(simulator.tap2On);
        tap2SpeedSlider.setEnabled(simulator.tap2On); tap2DirectionComboBox.setEnabled(simulator.tap2On);
        tap2DirectionComboBox.setSelectedItem(simulator.tap2Dir);

        // Scroll pane
        JScrollPane settingsScrollPane = new JScrollPane(settingsPanel);
        settingsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        settingsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        settingsScrollPane.setPreferredSize(new Dimension(300, 800));

        // Add components to panel
        settingsPanel.add(gravityLabel); settingsPanel.add(gravitySlider);
        settingsPanel.add(Box.createVerticalStrut(10));

        settingsPanel.add(viscosityLabel); settingsPanel.add(viscositySlider);
        settingsPanel.add(Box.createVerticalStrut(10));

        settingsPanel.add(overRelaxationLabel); settingsPanel.add(overRelaxationSlider);
        settingsPanel.add(Box.createVerticalStrut(10));

        settingsPanel.add(leakDensityCheckBox);
        settingsPanel.add(Box.createVerticalStrut(10));

        settingsPanel.add(colourLabel); settingsPanel.add(colourComboBox);
        settingsPanel.add(Box.createVerticalStrut(10));

        settingsPanel.add(colourOptionLabel); settingsPanel.add(optionComboBox);
        settingsPanel.add(Box.createVerticalStrut(10));

        settingsPanel.add(showArrowsCheckBox);
        settingsPanel.add(Box.createVerticalStrut(10));

        settingsPanel.add(arrowSpacingLabel); settingsPanel.add(arrowSpacingSlider);
        settingsPanel.add(Box.createVerticalStrut(10));

        settingsPanel.add(tap1OnButton); settingsPanel.add(tap1OnCheckBox);
        settingsPanel.add(tap1Label); settingsPanel.add(tap1XLabel); settingsPanel.add(tap1XSlider);
        settingsPanel.add(tap1YLabel); settingsPanel.add(tap1YSlider);
        settingsPanel.add(tap1SpeedLabel); settingsPanel.add(tap1SpeedSlider);
        settingsPanel.add(tap1DirectionLabel); settingsPanel.add(tap1DirectionComboBox);
        settingsPanel.add(Box.createVerticalStrut(10));

        settingsPanel.add(tap2OnButton); settingsPanel.add(tap2OnCheckBox);
        settingsPanel.add(tap2Label); settingsPanel.add(tap2XLabel); settingsPanel.add(tap2XSlider);
        settingsPanel.add(tap2YLabel); settingsPanel.add(tap2YSlider);
        settingsPanel.add(tap2SpeedLabel); settingsPanel.add(tap2SpeedSlider);
        settingsPanel.add(tap2DirectionLabel); settingsPanel.add(tap2DirectionComboBox);
        settingsPanel.add(Box.createVerticalStrut(10));

        frame.add(settingsScrollPane, BorderLayout.EAST);
        frame.setVisible(true);

        // Key bindings
        if (true) {
            AbstractAction toggleSimulationAction = new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    // Pause/Play simulation
                    simulator.toggleSimulation();
                    // Switch button text
                    if (simulator.isPaused) {
                        playButton.setText("▶");
                    } else {
                        playButton.setText("||");
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
                    showArrowsCheckBox.setSelected(!showArrowsCheckBox.isSelected());
                }
            };
            AbstractAction resetAction = new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    // Pause simulation
                    if (!simulator.isPaused) {
                        simulator.toggleSimulation();
                        playButton.setText("▶");
                    }
                    // Reset all density and velocity values
                    simulator.resetDensity();
                }
            };
            AbstractAction fillAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Fill grid with random densities
                    simulator.fillRandom();
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
            inputMap.put(KeyStroke.getKeyStroke("R"), "reset");
            actionMap.put("reset", resetAction);
            inputMap.put(KeyStroke.getKeyStroke("F"), "fill");
            actionMap.put("fill", fillAction);
        }

        // Focus on main panel
        mainPanel.setFocusable(true); mainPanel.requestFocusInWindow();
        playButton.setFocusable(false); slowButton.setFocusable(false); fastButton.setFocusable(false);

        // Dark colour theme
        if (true) {
            frame.setBackground(Color.darkGray);
            center.setBackground(Color.darkGray);
            mainPanel.setBackground(Color.darkGray);
            gridPanel.setBackground(Color.darkGray);
            controlPanel.setBackground(Color.darkGray);
            buttonPanel.setBackground(Color.darkGray);
            controlsText.setBackground(Color.lightGray);
            scrollPane.setBackground(Color.darkGray);
        }
    }

    // Apply mouse effect
    private void applyMouseEffect(MouseEvent e) {
        // Get mouse position
        int x = e.getX() / CELL_SIZE;
        int y = e.getY() / CELL_SIZE;

        // If in bounds
        if (x >= 0 && x < simulator.getGrid().getWidth() &&
                y >= 0 && y < simulator.getGrid().getHeight()) {

            int button = e.getButton();
            if (button == MouseEvent.BUTTON1) {
                // Left click
                simulator.addMouseInfluence(x, y, MouseEvent.BUTTON1);
            } else if (button == MouseEvent.BUTTON3) {
                // Right click
                simulator.addMouseInfluence(x, y, MouseEvent.BUTTON3);
            }

            // Update GUI
            repaint();
        }
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

                        // Options for visualisation
                        double value = switch (colourOption) {
                            case 1 -> // Density
                                    0.4 * cell.density;
                            case 2 -> // Velocity
                                    0.05 * Math.sqrt(cell.velocityX * cell.velocityX + cell.velocityY * cell.velocityY);
                            case 3 -> // Pressure
                                    0.001 * cell.pressure;
                            default -> 0;
                        };

                        Color cellColour = interpolateColour(baseHexCode, value);
                        g.setColor(cellColour); // Set colour

                        // Draw cell at correct position and size
                        g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);


                        // Draw velocity arrows
                        if (simulator.showVectorArrows
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
                // Average pressure counter
                double avgPressure = simulator.totalPressure / countFluidCells();

                // Draw Counters
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 16));
                g.drawString(String.format("FPS: %.2f", fps), 10, 20);
                g.drawString(String.format("Pressure: %.2f", avgPressure), 10, 40);

            }
        };

        // Set preferred size
        int gridWidth = simulator.getGrid().getWidth() * CELL_SIZE;
        int gridHeight = simulator.getGrid().getHeight() * CELL_SIZE;
        gridPanel.setPreferredSize(new Dimension(gridWidth, gridHeight));
    }

    // Interpolate colour based on attribute
    private Color interpolateColour(String baseHexCode, double value) {
        //1: density, 2: velocity, 3: pressure
        Color baseColour = Color.decode(baseHexCode); // Base colour
        // Clamp value
        value = Math.max(0, Math.min(1, value));
        // Scale RGB based on value
        int red = (int) (baseColour.getRed() * value);
        int green = (int) (baseColour.getGreen() * value);
        int blue = (int) (baseColour.getBlue() * value);

        return new Color(red, green, blue);
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
        frame.setSize(1000, 800);

        // Create GUI and add Simulator
        GUI gui = new GUI(simulator);
        frame.add(gui);
        frame.setLocationRelativeTo(null); // Set location to centre
        frame.setVisible(true); // Set visible
    }

    // Count fluid cells
    public int countFluidCells() {
        int count = 0;
        for (int x = 0; x < simulator.gridWidth; x++) {
            for (int y = 0; y < simulator.gridHeight; y++) {
                if (simulator.getGrid().getCell(x, y).state == 1) {
                    count++;
                }
            }
        }
        return count;
    }
}
