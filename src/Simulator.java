import java.lang.Math;
public class Simulator {
    // References
    private final Grid grid;
    // Variables
   int gridHeight; int gridWidth;
   int maxIterations = 15;
   public int delay = 16; // GUI timestep
    double timestep = 1; // Simulator timestep
    double diffusionConstant = 15;

    // Constructor
    public Simulator() {
        grid = new Grid(100, 100);
        gridHeight = grid.getHeight();
        gridWidth = grid.getWidth();

        // Test cells
        //grid.getCell(gridWidth-2, gridHeight-2).density = 1000;
        //grid.getCell(1, 1).density = 100;
        //grid.getCell(gridHeight/2, gridHeight/2).density = 1000;
        grid.getCell(1, 98).density = 1000;
    }

    // Main procedures
    public void stepSimulation() {
        applyDiffusion();
        applyAdvection();
        maintainZeroDivergence();
        System.out.printf("Density = %f\n", grid.getCell(0, 98).density);
    }

    private void applyAdvection() {
        // Create temporary array
        double[][] next = new double[gridHeight][gridWidth];
        // Loop through each cell
        for (int i = 0; i < gridHeight; i++) {
            for (int j = 0; j < gridWidth; j++) {
                // Get cell
                Cell cell = grid.getCell(i, j);

                // Source location (ensure in bounds)
                double fx = j - (cell.velocityX * timestep);
                double fy = i - (cell.velocityY * timestep);
                fx = Math.max(0, Math.min(gridWidth - 2, fx));
                fy = Math.max(0, Math.min(gridHeight - 2, fy));

                // Integer values
                int ix = (int) Math.floor(fx);
                int iy = (int) Math.floor(fy);

                // Fractional values
                double jx = fx - ix;
                double jy = fy - iy;

                // Round 1 lerps
                double z1 = lerp(grid.getCell(ix, iy).density, grid.getCell(ix+1, iy).density, jx);
                double z2 = lerp(grid.getCell(ix, iy+1).density, grid.getCell(ix+1, iy+1).density, jx);

                // Final lerp
                next[i][j] = lerp(z1, z2, jy);
            }
        }
        // Apply next values to current density
        for (int i = 0; i < gridHeight; i++) {
            for (int j = 0; j < gridWidth; j++) {
                Cell cell = grid.getCell(i, j);
                cell.density = next[j][i];
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
        // Gauss-Seidel Iteration
        for (int n = 0; n < maxIterations; n++) {
            // Loop through each cell
            for (int i = 0; i < gridHeight; i++) {
                for (int j = 0; j < gridWidth; j++) {
                    // Get cell
                    Cell cell = grid.getCell(i, j);
                    double surroundingDensity = calculateSurroundingAttributes(i, j, 1);

                    // Update previous values
                    cell.updatePreviousState();
                    // Assign calculated value to temporary storage
                    cell.nextDensity = (cell.density + surroundingDensity * diffusionConstant) / (1 + diffusionConstant);
                }
            }
            // Apply nextDensity values to current density
            for (int i = 0; i < gridHeight; i++) {
                for (int j = 0; j < gridWidth; j++) {
                    Cell cell = grid.getCell(i, j);
                    cell.density = cell.nextDensity;
                }
            }
        }
    }

    private void solveVelocities() {
        // Gauss-Seidel solver
        for (int n = 0; n < maxIterations; n++) {
            // Loop through each cell
            for (int i = 0; i < gridHeight; i++) {
                for (int j = 0; j < gridWidth; j++) {
                    // Get cell
                    Cell cell = grid.getCell(i, j);
                    double surroundingVelocityX = calculateSurroundingAttributes(i, j, 2);
                    double surroundingVelocityY = calculateSurroundingAttributes(i, j, 3);

                    // Update previous values
                    cell.updatePreviousState();
                    cell.nextVelocityX = (cell.velocityX + surroundingVelocityX * diffusionConstant) / (1 + diffusionConstant);
                    cell.nextVelocityY = (cell.velocityY + surroundingVelocityY * diffusionConstant) / (1 + diffusionConstant);
                }
            }
            // Apply nextVelocity values to current velocities
            for (int i = 0; i < gridHeight; i++) {
                for (int j = 0; j < gridWidth; j++) {
                    Cell cell = grid.getCell(i, j);
                    cell.velocityX = cell.nextVelocityX;
                    cell.velocityY = cell.nextVelocityY;
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