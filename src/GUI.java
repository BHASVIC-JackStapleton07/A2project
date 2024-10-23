import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GUI extends JPanel {
    private Simulator simulator; //reference simulator
    private final int CELL_SIZE = 10; //cell size in pixels
    private JPanel gridPanel; //grid renderer

    public GUI(Simulator simulator) {
        this.simulator = simulator;
        setLayout(new BorderLayout());
        createGridPanel();
        add(gridPanel, BorderLayout.CENTER); //put gridpanel at centre
        setPreferredSize(new Dimension(800, 600));

        //setup main jframe
        JFrame frame = new JFrame("Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);
        frame.setLayout(new BorderLayout());
        frame.add(this, BorderLayout.CENTER);
        frame.setVisible(true);

        //timer
        new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //simulator.stepSimulation;
                update();
            }
        }).start();
    }

    private void createGridPanel() {
        gridPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                renderFluid(g);
            }
        };
    }

    //render each cell
    private void renderFluid(Graphics g) {
        //reference grid
        Grid grid = simulator.getGrid();

        // loop through each cell
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                Cell cell = grid.getCell(x, y);

                // colour changes with density
                int colourDensity = (int) cell.density * 255;
                g.setColor(new Color(colourDensity, colourDensity, colourDensity)); //set colour (blue only for now)
                //draw a square for each cell
                g.fillRect(x*CELL_SIZE, y*CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        //
        int width = grid.getWidth() * CELL_SIZE;
        int height = grid.getHeight() * CELL_SIZE;
        gridPanel.setPreferredSize(new Dimension(width, height));
        gridPanel.setBackground(Color.black);
    }

    public void setup() {
        JFrame frame = new JFrame("Simulator");
        frame.setBounds(0, 0, 800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public void update() {
        gridPanel.repaint();
    }


}
