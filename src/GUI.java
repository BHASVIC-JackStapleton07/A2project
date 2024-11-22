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

                //render each cell
                for (int x = 0; x < grid.getWidth(); x++) {
                    for (int y = 0; y < grid.getHeight(); y++) {
                        Cell cell = grid.getCell(x, y);

                        //visualize cell density
                        float density = (float) cell.density; //get density
                        density = Math.max(0, Math.min(1, density)); //clamps between 1 and 0

                        // find pink to white gradient
                        //int red = 255; // Constant for white and pink
                        //int green = (int) Math.max(0, Math.min(255, lerp(255, 192, density)));
                        //int blue = (int) Math.max(0, Math.min(255, lerp(255, 203, density)));


                        g.setColor(new Color(density, density, density)); //returns white to pink colour

                        // draw cell at proper position
                        g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
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