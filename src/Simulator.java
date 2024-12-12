import java.lang.Math;
public class Simulator {
    // References
    private final Grid grid;
    // Variables
   int gridHeight;
   int gridWidth;
   int maxIterations = 15;
   public int delay = 16; // GUI timestep
    double timestep = 0.01; // Simulator timestep
    double diffusionConstantd = 1;
    double diffusionConstantv = 10;

    // Constructor
    public Simulator() {
        grid = new Grid(100, 100);
        // Define dimensions
        gridHeight = grid.getHeight();
        gridWidth = grid.getWidth();
        // Set boundary
        setBoundaries();

        // Test cells
        grid.getCell(20, 20).density = 1000;
        //grid.getCell(50, 51).velocityY = -15;
        //grid.getCell(50, 49).velocityY = 15;

        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                if (grid.getCell(x, y).boundary == 0) { continue;}
                grid.getCell(x,y).velocityY = 50;
                grid.getCell(x,y).velocityX = 50;
            }
        }
    }

    // Main procedures
    public void stepSimulation() {
        keepBoundaryConditions();

        maintainZeroDivergence();
        applyAdvection();
        applyDiffusion();

        //debugVelocities();
        debugTotalDensity();
    }

    private void applyAdvection() {
        // Next densities array
        double[][] newDensities = new double[gridHeight][gridWidth];

        // Loop through each cell
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                // Get cell reference
                Cell cell = grid.getCell(x, y);

                //Skip boundaries
                if (grid.getCell(x, y).boundary == 0) { continue; }

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
            for (int x = 0; x < gridWidth; x++) {
                for (int y = 0; y < gridHeight; y++) {

                    //Skip boundaries
                    if (grid.getCell(x, y).boundary == 0) { continue; }

                    // Calculate divergence
                    double divergence = grid.getCell(x+1, y).velocityX -
                            grid.getCell(x, y).velocityX + grid.getCell(x, y+1).velocityY -
                            grid.getCell(x, y).velocityY;

                    // Free cell count (4 is max)
                    int freeCells = grid.getCell(x+1, y).boundary + grid.getCell(x-1, y).boundary + grid.getCell(x, y+1).boundary +
                            grid.getCell(x, y-1).boundary;
                    grid.getCell(x, y).velocityX += (double) 1 / freeCells * divergence;
                    grid.getCell(x+1,y).velocityX -= (double) 1 / freeCells * divergence;
                    grid.getCell(x,y).velocityY += (double) 1 / freeCells * divergence;
                    grid.getCell(x,y+1).velocityY -= (double) 1 / freeCells * divergence;


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

                    //Skip boundaries
                    if (grid.getCell(x, y).boundary == 0) { continue; }

                    // Calculate surrounding density
                    double surroundingDensity = calculateSurroundingAttributes(x, y, 1);

                    // Update previous values
                    cell.updatePreviousState();
                    // Assign calculated value to temporary storage
                    newDensities[y][x] = (cell.density + surroundingDensity * diffusionConstantd) / (1 + diffusionConstantd);
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

                    //Skip boundaries
                    if (grid.getCell(x, y).boundary == 0) { continue; }

                    // Calculate surrounding velocities
                    double surroundingVelocityX = calculateSurroundingAttributes(x, y, 2);
                    double surroundingVelocityY = calculateSurroundingAttributes(x, y, 3);

                    // Update previous values
                    cell.updatePreviousState();

                    // Calculate new velocities
                    newVelocityX[y][x] = (cell.velocityX + surroundingVelocityX * diffusionConstantv) / (1 + diffusionConstantv);
                    newVelocityY[y][x] = (cell.velocityY + surroundingVelocityY * diffusionConstantv) / (1 + diffusionConstantv);
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

    // Boundaries
    private void applyReflectiveBoundaryDensity() {
        // Left and right boundaries
        for (int y = 0; y < gridHeight; y++) {
            // Left boundary: mirror the density from x = 1
            grid.getCell(0, y).density = grid.getCell(1, y).density;

            // Right boundary: mirror the density from x = gridWidth - 2
            grid.getCell(gridWidth - 1, y).density = grid.getCell(gridWidth - 2, y).density;
        }

        // Top and bottom boundaries
        for (int x = 0; x < gridWidth; x++) {
            // Top boundary: mirror the density from y = 1
            grid.getCell(x, 0).density = grid.getCell(x, 1).density;

            // Bottom boundary: mirror the density from y = gridHeight - 2
            grid.getCell(x, gridHeight - 1).density = grid.getCell(x, gridHeight - 2).density;
        }
    }
    private void keepBoundaryConditions() {
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                if (grid.getCell(x,y).boundary == 1) { continue; }
                grid.getCell(x,y).velocityX = 0;
                grid.getCell(x,y).velocityY = 0;
                grid.getCell(x,y).density = 2000;
            }
        }
    }


    // Methods
    private double calculateSurroundingAttributes(int width, int height, int attribute) {
        int num = 0;
        double valueTotal = 0;

        //1: density. 2: velocityX. 3: velocityY
        // Bottom
        if (grid.getCell(width, height-1).boundary == 1) {
            if (attribute == 1) {
                valueTotal += grid.getCell(width, height-1).density;
            } else if (attribute == 2) {
                valueTotal += grid.getCell(width, height-1).velocityX;
            } else{
                valueTotal += grid.getCell(width, height-1).velocityY;
            }
            num++;
        }

        // Top
        if (grid.getCell(width, height+1).boundary == 1) {
            if (attribute == 1) {
                valueTotal += grid.getCell(width, height+1).density;
            } else if (attribute == 2) {
                valueTotal += grid.getCell(width, height+1).velocityX;
            } else{
                valueTotal += grid.getCell(width, height+1).velocityY;
            }
            num++;
        }

        // Left
        if (grid.getCell(width-1,height).boundary == 1) {
            if (attribute == 1) {
                valueTotal += grid.getCell(width-1, height).density;
            } else if (attribute == 2) {
                valueTotal += grid.getCell(width-1, height).velocityX;
            } else{
                valueTotal += grid.getCell(width-1, height).velocityY;
            }
            num++;
        }

        // Right
        if (grid.getCell(width+1,height).boundary == 1) {
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
                if (cell.boundary == 0) { continue; }
                totalDensity += cell.density;
            }
        }
        System.out.println("Density: " + totalDensity);
    }


    // getter
    public Grid getGrid() {
        return grid;
    }
}