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
        // Define dimensions
        gridHeight = grid.getHeight();
        gridWidth = grid.getWidth();
        // Set boundary
        setBoundaries();

        // Test cells
        grid.getCell(50, 50).density = 1000;
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                if (x == 50) {
                    if (y == 51) {
                        grid.getCell(x, y).velocityY = 10;
                    } else if (y == 50) {
                        grid.getCell(x, y).velocityY = -10;
                    }
                }
            }
        }

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
        // Gauss-Seidel iteration
        for (int n = 0; n < maxIterations; n++) {
            // Loop through all fluid cells (not edges)
            for (int x = 1; x < gridWidth-1; x++) {
                for (int y = 1; y < gridHeight-1; y++) {

                    // Calculate divergence
                    double divergence = grid.getCell(x+1, y).velocityX -
                            grid.getCell(x, y).velocityX + grid.getCell(x, y+1).velocityY -
                            grid.getCell(x, y).velocityY;
                    if (n == maxIterations-1) { System.out.println(divergence); }

                    // Free cell count (4 is max)
                    int freeCells = grid.getCell(x+1,y).boundary + grid.getCell(x-1,y).boundary
                            + grid.getCell(x, y+1).boundary + grid.getCell(x, y-1).boundary;

                    // Calculate new velocities
                    grid.getCell(x,y).velocityX += divergence * grid.getCell(x-1,y).boundary / freeCells;
                    grid.getCell(x+1,y).velocityX -= divergence * grid.getCell(x+1,y).boundary / freeCells;
                    grid.getCell(x,y).velocityY += divergence * grid.getCell(x,y-1).boundary / freeCells;
                    grid.getCell(x,y+1).velocityY -= divergence * grid.getCell(x,y+1).boundary / freeCells;
                }
            }
        }
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

    private void setBoundaries() {
        // Loop through all cells
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                // Get Cell
                Cell cell = grid.getCell(x, y);
                // If edge cell, set to boundary
                if (x == 0 || x == 99 || y == 0 || y == 99) {
                    cell.boundary = 0;
                }
            }
        }
    }

    // getter
    public Grid getGrid() {
        return grid;
    }
}