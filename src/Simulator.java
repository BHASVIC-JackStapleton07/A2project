public class Simulator {
    // References
    private final Grid grid;
    // Variables
   int gridHeight;
   int gridWidth;

   // Constants
   int maxIterations = 15;
   public int delay = 1*16; // GUI timestep
    double timestep = 0.05; // Simulator timestep
    double diffusionConstant = 1;
    double gravity = 5;
    double overrelaxation = 1.0;

    // Visual
    public boolean showVectorArrows;
    public float maxDensity = 10;

    // Constructor
    public Simulator() {
        grid = new Grid(100, 100);
        // Define dimensions
        gridHeight = grid.getHeight();
        gridWidth = grid.getWidth();
        // Set boundary
        setBoundaries();
        // Debugging
        showVectorArrows = true;

        // Test cells
        grid.getCell(50, 50).density = 0.1;
    }

    // Main procedures
    public void stepSimulation() {
        // Physics
        maintainZeroDivergence();
        extrapolate();
        advectVelocity();
        advectDensity();

        // Boundaries

        // Interaction
        addTap(1, 50, 'r', 75);
        //addTap(98, 50, 'l', 75);

        // Debug
        //debugDivergence();
    }

    public void addGravity() {
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                Cell cell = grid.getCell(x, y);

                if (cell.state == 1) {
                    cell.velocityY += gravity * timestep;
                }
            }
        }
    }

    public void maintainZeroDivergence() {
        double cp = 0.1 / timestep;

        for (int n = 0; n < 20; n++) {
            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x < gridWidth; x++) {
                    Cell cell = grid.getCell(x, y);
                    if (cell.state == 1) {
                        double sx0 = grid.getCell(x-1,y).state;
                        double sx1 = grid.getCell(x+1,y).state;
                        double sy0 = grid.getCell(x,y-1).state;
                        double sy1 = grid.getCell(x,y+1).state;
                        double s = sx0 + sx1 + sy0 + sy1;

                        if (s != 0) {
                            double div = grid.getCell(x+1,y).velocityX - cell.velocityX
                                    + grid.getCell(x,y+1).velocityY - cell.velocityY;
                            double p = -div / s;
                            p *= overrelaxation;
                            cell.pressure += cp * p;

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
        for (int x = 0; x < gridWidth; x++) {
            grid.getCell(x, 0).velocityX = grid.getCell(x,1).velocityX;
            grid.getCell(x, gridHeight-1).velocityX = grid.getCell(x,gridHeight-2).velocityX;
        }
        for (int y = 0; y < gridHeight; y++) {
            grid.getCell(0, y).velocityY = grid.getCell(1,y).velocityY;
            grid.getCell(gridWidth-1, y).velocityY = grid.getCell(gridWidth-2,y).velocityY;
        }
    }

    public void advectVelocity() {
        double[][] newVelocityX = new double[gridWidth][gridHeight];
        double[][] newVelocityY = new double[gridWidth][gridHeight];
        for (int x = 1; x < gridWidth; x++) {
            for (int y = 1; y < gridHeight; y++) {
                Cell cell = grid.getCell(x, y);
                if (cell.state != 0 && grid.getCell(x-1,y).state != 0 && y < gridHeight - 1) {
                    double xVal = x;
                    double yVal = y + 0.5;
                    double u = cell.velocityX;
                    double v = avgV(x, y);

                    xVal -= timestep * u;
                    yVal -= timestep * v;
                    u = sampleField(xVal, yVal, 0);
                    newVelocityX[x][y] = u;
                }

                if (cell.state != 0.0 && grid.getCell(x, y-1).state != 0.0 && x < gridWidth - 1) {
                    double xVal = x + 0.5;
                    double yVal = y;
                    double u = avgU(x, y);
                    double v = cell.velocityY;

                    xVal -= timestep * u;
                    yVal -= timestep * v;
                    v = sampleField(xVal, yVal, 1);
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
        double[][] newDensity = new double[gridWidth][gridHeight];
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                Cell cell = grid.getCell(x, y);
                if (cell.state != 0) {
                    double u = (cell.velocityX + grid.getCell(x+1,y).velocityX) * 0.5;
                    double v = (cell.velocityY + grid.getCell(x,y+1).velocityY) * 0.5;
                    double xVal = x + 0.5 - timestep * u;
                    double yVal = y + 0.5 - timestep * v;

                    newDensity[x][y] = sampleField(xVal, yVal, 2);
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

    public void addTap(int x, int y, char dir, double speed) {
        grid.getCell(x,y).density += 10;
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

    //  Other procedures

    private double sampleField(double x, double y, int choice) {
        double h1 = 1.0;
        double h2 = 0.5;

        // Clamp x and y to grid
        x = Math.max(Math.min(x, gridWidth-2), 1);
        y = Math.max(Math.min(y, gridHeight-2), 1);

        double dx = 0.0;
        double dy = 0.0;

        //Adjust dx and dy based on field
        //0: velocityX, 1: velocityY, 2: density
        if (choice == 0) {
            dy = h2;
        } else if (choice == 1) {
            dx = h2;
        } else if (choice == 2) {
            dx = h2; dy = h2;
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

        //0: velocityX, 1: velocityY, 2: density
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
                grid.getCell(x+1,y+1).velocityX + grid.getCell(x+1,y).velocityX) * 0.25;
    }

    public double avgV(int x, int y) {
        return (grid.getCell(x-1,y).velocityY + grid.getCell(x,y).velocityY +
                grid.getCell(x-1,y+1).velocityY + grid.getCell(x,y+1).velocityY) * 0.25;
    }

    // Boundaries
    private void setBoundaries() {
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                if (x == 0 || x == gridWidth-1 || y == 0 || y == gridHeight-1) {
                    grid.getCell(x,y).state = 0;
                    grid.getCell(x,y).velocityY = 0;
                    grid.getCell(x,y).velocityX = 0;
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


    // getter
    public Grid getGrid() {
        return grid;
    }
}