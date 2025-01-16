import javax.swing.*;
import java.awt.*;

public class GUI extends JPanel {
    private final Simulator simulator; //reference simulator
    private final int CELL_SIZE = 5; //cell size in pixels
    private JPanel gridPanel; //grid renderer

    // beautiful constructor
    public GUI(Simulator simulator) {
        this.simulator = simulator; //initialise simulator
        JPanel center = new JPanel(new FlowLayout());
        createGridPanel(); //crate grid panel
        center.add(gridPanel);
        add(center);
    }

    //create gridPanel
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
                            double xVel = Math.max(Math.min(cell.velocityX, 50), -50);
                            double yVel = Math.max(Math.min(cell.velocityY, 50), -50);

                            // Skip still cells
                            if (cell.velocityX != 0 || cell.velocityY != 0) {
                                g.setColor(Color.RED); // Set colour
                                g.drawLine(x * CELL_SIZE + CELL_SIZE / 2, // Draw line with variable length
                                        y * CELL_SIZE + CELL_SIZE / 2,
                                        (int) (x * CELL_SIZE + CELL_SIZE / 2 + xVel / 5),
                                        (int) (y * CELL_SIZE + CELL_SIZE / 2 + yVel / 5));
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

    //update method
    public void update() {
        gridPanel.repaint();
    }

    public static void createAndShowGUI(Simulator simulator) {
        //create main jframe
        JFrame frame = new JFrame("Fluid Sim");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);

        //create gui and add simulator
        GUI gui = new GUI(simulator);
        frame.add(gui);
        frame.setLocationRelativeTo(null); //set frame to centre screen
        frame.setVisible(true); //set visible

        //routinely update
        //Define 60fps timer
        Timer timer = new Timer(simulator.delay, e -> {
            simulator.stepSimulation();
            gui.update();
        });
        timer.start();
    }

    private double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }
}