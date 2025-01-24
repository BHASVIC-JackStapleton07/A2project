

public class Simulator {
    // References
    private final Grid grid;

    // Variables
   int gridHeight;
   int gridWidth;

   // Constants
   public int delay = 8; // GUI timestep
    double timestep = 0.05; // Simulator timestep
    double gravity = 0;
    double overrelaxation = 1.5;
    double viscosity = 0.0;

    // Visual
    public boolean showVectorArrows;
    public double arrowSpacing = 6;
    public float maxDensity = 10;
    public double totalPressure;
    public boolean leakDensity = true;

    // Functionality
    public boolean isPaused = true;

    // Taps
    int tap1X = 1; int tap1Y = 50; int tap2Y = 1; int tap2X = 50;
    char tap1Dir = 'r'; char tap2Dir = 'd'; double tap1Speed = 50; double tap2Speed = 50;
    boolean tap1On = true; boolean tap2On = false;

    // Constructor
    public Simulator() {
        grid = new Grid(100, 100);
        // Define dimensions
        gridHeight = grid.getHeight();
        gridWidth = grid.getWidth();
        // Set boundary
        setBoundaries();
        // Debugging
        showVectorArrows = false;

        // Test cells
    }

    // Main procedures
    public void stepSimulation() {
        // If paused, don't play
        if (!isPaused) {
            // Interaction
            addTap(tap1X, tap1Y, tap1Dir, tap1Speed, tap1On);
            addTap(tap2X, tap2Y, tap2Dir, tap2Speed, tap2On);

            // Physics
            addGravity();
            maintainZeroDivergence();
            extrapolate();
            advectVelocity();
            advectDensity();

            if (leakDensity) {
                leakDensity();
            }

            // Debug
            //debugDivergence();
            //debugTotalDensity();
        }
    }
    public void addGravity() {
        // For each cell
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                Cell cell = grid.getCell(x, y);
                // Apply gravity to fluid cells
                if (cell.state == 1) {
                    cell.velocityY += gravity * timestep;
                }
            }
        }
    }

    public void maintainZeroDivergence() {
        // Coefficients
        double cp = 0.1 / timestep;
        // Reset total pressure
        totalPressure = 0;

        // Loop through all cells with Gauss-Seidel iteration
        for (int n = 0; n < 20; n++) {
            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x < gridWidth; x++) {
                    Cell cell = grid.getCell(x, y);
                    if (cell.state == 1) { // If fluid cell
                        // Get neighbour states
                        double sx0 = grid.getCell(x-1,y).state;
                        double sx1 = grid.getCell(x+1,y).state;
                        double sy0 = grid.getCell(x,y-1).state;
                        double sy1 = grid.getCell(x,y+1).state;
                        double s = sx0 + sx1 + sy0 + sy1; // Total states

                        if (s != 0) {
                            // Calculate divergence and pressure
                            double div = grid.getCell(x+1,y).velocityX - cell.velocityX
                                    + grid.getCell(x,y+1).velocityY - cell.velocityY;
                            double p = -div / s;
                            totalPressure += Math.abs(p);
                            // Apply overrelaxation
                            p *= overrelaxation;
                            cell.pressure += cp * p;
                            // Adjust velocities based on pressure
                            cell.velocityX -= sx0 * p;
                            grid.getCell(x+1,y).velocityX += sx1 * p;
                            cell.velocityY -= sy0 * p;
                            grid.getCell(x,y+1).velocityY += sy1 * p;
                        }
                    }
                }
            }
        }
    }

    public void extrapolate() {
        // Loop through all cells
        for (int x = 0; x < gridWidth; x++) {
            // Copy neighbour velocities
            grid.getCell(x, 0).velocityX = grid.getCell(x,1).velocityX;
            grid.getCell(x, gridHeight-1).velocityX = grid.getCell(x,gridHeight-2).velocityX;
        }
        for (int y = 0; y < gridHeight; y++) {
            // Copy neighbour velocities
            grid.getCell(0, y).velocityY = grid.getCell(1,y).velocityY;
            grid.getCell(gridWidth-1, y).velocityY = grid.getCell(gridWidth-2,y).velocityY;
        }
    }

    public void advectVelocity() {
        // Temporary velocity arrays
        double[][] newVelocityX = new double[gridWidth][gridHeight];
        double[][] newVelocityY = new double[gridWidth][gridHeight];
        // Loop through all cells
        for (int x = 1; x < gridWidth; x++) {
            for (int y = 1; y < gridHeight; y++) {
                Cell cell = grid.getCell(x, y);
                // If fluid cell, and left neighbour is fluid cell
                if (cell.state != 0 && grid.getCell(x-1,y).state != 0 && y < gridHeight - 1) {
                    // Calculate position and velocity
                    double xVal = x;
                    double yVal = y + 0.5;
                    double u = cell.velocityX;
                    double v = avgV(x, y);

                    // Calculate source position and new velocitu
                    xVal -= timestep * u;
                    yVal -= timestep * v;
                    u = lerpPoint(xVal, yVal, 0);
                    newVelocityX[x][y] = u;
                }
                // If fluid cell, and top neighbour is fluid cell
                if (cell.state != 0.0 && grid.getCell(x, y-1).state != 0.0 && x < gridWidth - 1) {
                    // Calculate position and velocity
                    double xVal = x + 0.5;
                    double yVal = y;
                    double u = avgU(x, y);
                    double v = cell.velocityY;
                    // Calculate source position and new velocity
                    xVal -= timestep * u;
                    yVal -= timestep * v;
                    v = lerpPoint(xVal, yVal, 1);
                    newVelocityY[x][y] = v;
                }
            }
        }

        // Update velocity fields
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                Cell cell = grid.getCell(x, y);
                cell.velocityX = newVelocityX[x][y];
                cell.velocityY = newVelocityY[x][y];
            }
        }
    }

    public void advectDensity() {
        // Temporary density array
        double[][] newDensity = new double[gridWidth][gridHeight];
        // Loop through all cells
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                Cell cell = grid.getCell(x, y);
                if (cell.state != 0) { // If fluid cell
                    // Calculate position and velocity
                    double u = (cell.velocityX + grid.getCell(x+1,y).velocityX) * 0.5;
                    double v = (cell.velocityY + grid.getCell(x,y+1).velocityY) * 0.5;
                    double xVal = x + 0.5 - timestep * u;
                    double yVal = y + 0.5 - timestep * v;
                    // Calculate source position and new density
                    newDensity[x][y] = lerpPoint(xVal, yVal, 2);
                }
            }
        }

        // Update density values
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                grid.getCell(x, y).density = newDensity[x][y];
            }
        }
    }

    public void addTap(int x, int y, char dir, double speed, boolean on) {
        if (on) {
            // Add density
            grid.getCell(x, y).density += 10;
            // Add velocity
            switch (dir) {
                case 'u':
                    grid.getCell(x, y).velocityY -= speed;
                    break;
                case 'd':
                    grid.getCell(x, y).velocityY += speed;
                    break;
                case 'l':
                    grid.getCell(x, y).velocityX -= speed;
                    break;
                case 'r':
                    grid.getCell(x, y).velocityX += speed;
                    break;
                default:
                    break;
            }
        }
    }

    //  Other procedures

    private double lerpPoint(double x, double y, int choice) {

        // Clamp x and y to grid
        x = Math.max(Math.min(x, gridWidth-2), 1);
        y = Math.max(Math.min(y, gridHeight-2), 1);

        // Declare x and y offsets
        double dx = 0.0;
        double dy = 0.0;

        // Adjust dx and dy based on field
        // 0: velocityX, 1: velocityY, 2: density
        if (choice == 0) {
            dy = 0.5;
        } else if (choice == 1) {
            dx = 0.5;
        } else if (choice == 2) {
            dx = 0.5; dy = 0.5;
        }

        // Find neighbours
        int x0 = Math.min((int) Math.floor(x - dx), gridWidth - 1);
        double tx = (x - dx) - x0;
        int x1 = Math.min(x0 + 1, gridWidth - 1);

        int y0 = Math.min((int) Math.floor(y - dy), gridHeight - 1);
        double ty = (y - dy) - y0;
        int y1 = Math.min(y0 + 1, gridHeight - 1);

        // Interpolation weights
        double sx = 1.0 - tx;
        double sy = 1.0 - ty;

        // Bilinear Interpolation
        double val =
                sx * sy * getFieldValue(x0, y0, choice)
                + tx * sy * getFieldValue(x1, y0, choice)
                + tx * ty * getFieldValue(x1, y1, choice)
                + sx * ty * getFieldValue(x0, y1, choice);

        return val;
    }

    private double getFieldValue(int x, int y, int choice) {
        Cell cell = grid.getCell(x, y);

        // 0: velocityX, 1: velocityY, 2: density
        if (choice == 0) {
            return cell.velocityX;
        } else if (choice == 1) {
            return cell.velocityY;
        } else {
            return cell.density;
        }
    }

    public double avgU(int x, int y) {
        return (grid.getCell(x,y-1).velocityX + grid.getCell(x,y).velocityX +
                grid.getCell(x+1,y-1).velocityX + grid.getCell(x+1,y).velocityX) * 0.25;
    }

    public double avgV(int x, int y) {
        return (grid.getCell(x-1,y).velocityY + grid.getCell(x,y).velocityY +
                grid.getCell(x-1,y+1).velocityY + grid.getCell(x,y+1).velocityY) * 0.25;
    }

    // Boundaries
    private void setBoundaries() {
        // Loop through all cells
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                // Set boundary if on edge
                if (x == 0 || x == gridWidth-1 || y == 0 || y == gridHeight-1) {
                    grid.getCell(x,y).state = 0;
                    grid.getCell(x,y).velocityY = 0;
                    grid.getCell(x,y).velocityX = 0;
                }
            }
        }
    }

    // Interaction
    public void toggleSimulation() {
        // Toggle pause
        isPaused = !isPaused;
    }

    public void resetDensity() {
        // Loop through all cells
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                // Reset density
                grid.getCell(x, y).density = 0;
                // Reset velocity
                grid.getCell(x, y).velocityX = 0;
                grid.getCell(x, y).velocityY = 0;
                // Reset pressure
                grid.getCell(x, y).pressure = 0;
            }
        }
    }

    private void leakDensity() {
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                Cell cell = grid.getCell(x, y);
                if (cell.state == 1) {
                    cell.density *= 0.99;
                }
            }
        }
    }

    public void fillRandom() {
        // Loop through all cells
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                if (grid.getCell(x,y).state == 1) {
                    grid.getCell(x, y).density = Math.random() * 5;
                }
            }
        }
    }


    // Debugging
    private void debugVelocities() {
        double velocitiesx = 0;
        double velocitiesy = 0;
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                Cell cell = grid.getCell(x, y);
                velocitiesx += cell.velocityX;
                velocitiesy += cell.velocityY;
            }
        }
        System.out.println("VelocitiesX: " + velocitiesx + ", VelocitiesY: " + velocitiesy);
    }

    private void debugTotalDensity() {
        double totalDensity = 0;
        // Loop through all cells
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                // Get cell
                Cell cell = grid.getCell(x, y);
                // Skip boundary
                if (cell.state == 1) {
                    totalDensity += cell.density;
                }
            }
        }
        System.out.println("Density: " + totalDensity);
    }

    private void debugDivergence() {
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                Cell cell = grid.getCell(x,y);
                if (cell.state == 1) {
                    double d = (grid.getCell(x+1,y).velocityX - cell.velocityX) +
                            (grid.getCell(x,y+1).velocityY - cell.velocityY);
                    System.out.println(d);
                }
            }
        }
    }


    // Getters
    public Grid getGrid() {
        return grid;
    }
}