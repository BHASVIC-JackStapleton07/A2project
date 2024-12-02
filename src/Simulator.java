import java.lang.Math;
public class Simulator {
    // References
    private final Grid grid;
    // Variables
   int gridHeight;
   int gridWidth;
   int maxIterations = 15;
   public int delay = 16; // GUI timestep
    double timestep = 0.1; // Simulator timestep
    double diffusionConstant = 12;

    // Constructor
    public Simulator() {
        grid = new Grid(100, 100);
        gridHeight = grid.getHeight();
        gridWidth = grid.getWidth();

        // Test cells
        grid.getCell(50, 50).density = 1000;
    }

    // Main procedures
    public void stepSimulation() {
        applyAdvection();
        applyDiffusion();
        maintainZeroDivergence();
    }

    private void applyAdvection() {
        // Next densities array
        double[][] newDensities = new double[gridHeight][gridWidth];

        // Loop through each cell
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                // Get cell reference
                Cell cell = grid.getCell(x, y);

                // Calculate source location
                double fx = x - (cell.velocityX * timestep);
                double fy = y - (cell.velocityY * timestep);

                // Calculate integer and fractional parts
                int ix = (int) Math.floor(fx);
                int iy = (int) Math.floor(fy);
                double jx = fx - ix;
                double jy = fy - iy;

                // Calculate neighbour densities
                double topLeft = (ix >= 0 && iy >= 0 && ix < gridWidth && iy < gridHeight)
                        ? grid.getCell(ix, iy).density : 0;
                double topRight = (ix + 1 >= 0 && iy >= 0 && ix + 1 < gridWidth && iy < gridHeight)
                        ? grid.getCell(ix + 1, iy).density : 0;
                double bottomLeft = (ix >= 0 && iy + 1 >= 0 && ix < gridWidth && iy + 1 < gridHeight)
                        ? grid.getCell(ix, iy + 1).density : 0;
                double bottomRight = (ix + 1 >= 0 && iy + 1 >= 0 && ix + 1 < gridWidth && iy + 1 < gridHeight)
                        ? grid.getCell(ix + 1, iy + 1).density : 0;

                // First round of lerps
                double z1 = lerp(topLeft, topRight, jx);
                double z2 = lerp(bottomLeft, bottomRight, jx);
                // Final lerp
                newDensities[y][x] = lerp(z1, z2, jy);
            }
        }
        // Apply densities
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                grid.getCell(x, y).density = newDensities[y][x];
            }
        }
    }

    private void applyDiffusion() {
        solveDensities();
        solveVelocities();
    }

    private void maintainZeroDivergence() {

    }


    // Diffusion
    private void solveDensities() {
        // Next value array
        double[][] newDensities = new double[gridWidth][gridHeight];

        // Gauss-Seidel Iteration
        for (int n = 0; n < maxIterations; n++) {
            // Loop through each cell
            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x < gridWidth; x++) {
                    // Get cell
                    Cell cell = grid.getCell(x, y);
                    double surroundingDensity = calculateSurroundingAttributes(x, y, 1);

                    // Update previous values
                    cell.updatePreviousState();
                    // Assign calculated value to temporary storage
                    newDensities[y][x] = (cell.density + surroundingDensity * diffusionConstant) / (1 + diffusionConstant);
                }
            }
            // Apply nextDensity values to current density
            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x < gridWidth; x++) {
                    grid.getCell(x, y).density = newDensities[y][x];
                }
            }
        }
    }

    private void solveVelocities() {
        // Next value arrays
        double[][] newVelocityX = new double[gridHeight][gridWidth];
        double[][] newVelocityY = new double[gridHeight][gridWidth];

        // Gauss-Seidel solver
        for (int n = 0; n < maxIterations; n++) {
            // Loop through each cell
            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x < gridWidth; x++) {
                    // Get cell
                    Cell cell = grid.getCell(x, y);

                    // Calculate surrounding velocities
                    double surroundingVelocityX = calculateSurroundingAttributes(x, y, 2);
                    double surroundingVelocityY = calculateSurroundingAttributes(x, y, 3);

                    // Update previous values
                    cell.updatePreviousState();

                    // Calculate new velocities
                    newVelocityX[y][x] = (cell.velocityX + surroundingVelocityX * diffusionConstant) / (1 + diffusionConstant);
                    newVelocityY[y][x] = (cell.velocityY + surroundingVelocityY * diffusionConstant) / (1 + diffusionConstant);
                }
            }
            // Apply new values to current velocities
            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x < gridWidth; x++) {
                    grid.getCell(x, y).velocityX = newVelocityX[y][x];
                    grid.getCell(x, y).velocityY = newVelocityY[y][x];
                }
            }
        }
    }


    // Methods
    private double calculateSurroundingAttributes(int width, int height, int attribute) {
        int num = 0;
        double valueTotal = 0;

        //1: density. 2: velocityX. 3: velocityY
        if (height > 0) {
            if (attribute == 1) {
                valueTotal += grid.getCell(width, height-1).density;
            } else if (attribute == 2) {
                valueTotal += grid.getCell(width, height-1).velocityX;
            } else{
                valueTotal += grid.getCell(width, height-1).velocityY;
            }
            num++;
        }

        if (height < gridWidth-1) {
            if (attribute == 1) {
                valueTotal += grid.getCell(width, height+1).density;
            } else if (attribute == 2) {
                valueTotal += grid.getCell(width, height+1).velocityX;
            } else{
                valueTotal += grid.getCell(width, height+1).velocityY;
            }
            num++;
        }

        if (width > 0) {
            if (attribute == 1) {
                valueTotal += grid.getCell(width-1, height).density;
            } else if (attribute == 2) {
                valueTotal += grid.getCell(width-1, height).velocityX;
            } else{
                valueTotal += grid.getCell(width-1, height).velocityY;
            }
            num++;
        }

        if (width < gridHeight-1) {
            if (attribute == 1) {
                valueTotal += grid.getCell(width+1, height).density;
            } else if (attribute == 2) {
                valueTotal += grid.getCell(width+1, height).velocityX;
            } else{
                valueTotal += grid.getCell(width+1, height).velocityY;
            }
            num++;
        }

        return valueTotal / num;
    }

    private double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }

    // getter
    public Grid getGrid() {
        return grid;
    }
}