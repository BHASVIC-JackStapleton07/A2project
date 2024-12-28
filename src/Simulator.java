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
    double overrelaxation = 1.9;

    // Visual
    public boolean showVectorArrows;

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
        grid.getCell(50, 50).density = 1000;
    }

    // Main procedures
    public void stepSimulation() {
        // Physics
        solveIncompressibility();
        extrapolate();
        advectVel();
        advectSmoke();

        // Debug
        debugDivergence();
    }

    public void addGravity() {
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                Cell cell = grid.getCell(x, y);

                if (cell.boundary == 1) {
                    cell.velocityY += gravity * timestep;
                }
            }
        }
    }

    public void solveIncompressibility() {
        double cp = 0.1 / timestep;

        for (int n = 0; n < 20; n++) {
            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x < gridWidth; x++) {
                    Cell cell = grid.getCell(x, y);
                    if (cell.boundary == 1) {
                        double sx0 = grid.getCell(x-1,y).boundary;
                        double sx1 = grid.getCell(x+1,y).boundary;
                        double sy0 = grid.getCell(x,y-1).boundary;
                        double sy1 = grid.getCell(x,y+1).boundary;
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

    public void advectVel() {
        for (int x = 1; x < grid)
    }

    //  Other procedures

    private double sampleField(int choice, double x, double y) {
        double h1 = 1;
        double h2 = 0.5;

        x = Math.max(Math.min(x, gridWidth), 1);
        y = Math.max(Math.min(y, gridHeight), 1);

        double dx = 0.0, dy = 0.0;
        return dx;
    }

    public double avgU(int x, int y) {
        return (grid.getCell(x,y-1).velocityX + grid.getCell(x,y).velocityX +
                grid.getCell(x+1,y+1).velocityX + grid.getCell(x+1,y).velocityX) * 0.25;
    }

    public double avgV(int x, int y) {
        return (grid.getCell(x-1,y).velocityY + grid.getCell(x,y).velocityY +
                grid.getCell(x-1,y+1).velocityY + grid.getCell(x,y+1).velocityY) * 0.25;
    }

    private double calculateSurroundingValue(int x, int y, int value) {
        double total = 0; // Total density
        // 0: Density, 1: VelocityX, 2: VelocityY

        // Top
        if (y > 0 && grid.getCell(x, y-1).boundary != 0) { // If cell exists/ is fluid
            if (value == 0) {
                total += grid.getCell(x, y-1).density;
            } else if (value == 1) {
                total += grid.getCell(x, y-1).velocityX;
            } else if (value == 2) {
                total += grid.getCell(x, y-1).velocityY;
            }
        }

        // Bottom
        if (y < gridHeight - 1 && grid.getCell(x, y+1).boundary != 0) { // If cell exists/ is fluid
            if (value == 0) {
                total += grid.getCell(x, y+1).density;
            } else if (value == 1) {
                total += grid.getCell(x, y+1).velocityX;
            } else if (value == 2) {
                total += grid.getCell(x, y+1).velocityY;
            }
        }

        // Left
        if (x > 0 && grid.getCell(x-1, y).boundary != 0) { // If cell exists/ is fluid
            if (value == 0) {
                total += grid.getCell(x-1, y).density;
            } else if (value == 1) {
                total += grid.getCell(x-1, y).velocityX;
            } else if (value == 2) {
                total += grid.getCell(x-1, y).velocityY;
            }
        }

        // Right
        if (x < gridWidth - 1 && grid.getCell(x+1, y).boundary != 0) { // If cell exists/ is fluid
            if (value == 0) {
                total += grid.getCell(x+1, y).density;
            } else if (value == 1) {
                total += grid.getCell(x+1, y).velocityX;
            } else if (value == 2) {
                total += grid.getCell(x+1, y).velocityY;
            }
        }

        // Return average surrounding value
        return total;
    }

    private double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }

    // Boundaries
    private void setBoundaries() {
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                if (x == 0 || x == gridWidth-1 || y == 0 || y == gridHeight-1) {
                    grid.getCell(x,y).boundary = 0;
                    grid.getCell(x,y).velocityY = 0;
                    grid.getCell(x,y).velocityX = 0;
                }
            }
        }
    }

    public void applyBoundaryConditions() {
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                Cell cell = grid.getCell(x,y);
                if (cell.boundary == 0) {
                    cell.velocityX = 0;
                    cell.velocityY = 0;

                    //Adjust fluid neighbours
                    if (x > 0 && grid.getCell(x-1,y).boundary == 1) {
                        grid.getCell(x-1,y).velocityX = 0.0;
                    }
                    if (x < gridWidth-1 && grid.getCell(x+1,y).boundary == 1) {
                        grid.getCell(x+1,y).velocityX = 0.0;
                    }
                    if (y > 0 && grid.getCell(x,y-1).boundary == 1) {
                        grid.getCell(x,y-1).velocityY = 0.0;
                    }
                    if (y < gridHeight-1 && grid.getCell(x,y+1).boundary == 1) {
                        grid.getCell(x,y+1).velocityY = 0.0;
                    }
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
                if (cell.boundary == 1) {
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
                if (cell.boundary == 1) {
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