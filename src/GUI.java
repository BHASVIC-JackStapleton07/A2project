import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GUI extends JPanel {
    private Simulator simulator; //reference simulator
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
                        density = Math.max(0, Math.min(100, density)); //clamps between 1 and 0

                        //map density to greyscale
                        int colourValue = (int) (density * 255);
                        g.setColor(new Color(colourValue, colourValue, colourValue)); //returns greyscale colour

                        //draw cell at proper position
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
        Timer timer = new Timer(16, new ActionListener() { //Define 60fps timer
            public void actionPerformed(ActionEvent e) {
                simulator.stepSimulation();
                gui.update();
            }
        });
        timer.start();
    }
}