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
    double diffusionConstant = 15;

    // Constructor
    public Simulator() {
        grid = new Grid(100, 100);
        gridHeight = grid.getHeight();
        gridWidth = grid.getWidth();

        // Test cells
        grid.getCell(0, 0).density = 1000;
    }

    // Main procedures
    public void stepSimulation() {
        applyAdvection();
        applyDiffusion();
        maintainZeroDivergence();
    }


    private void applyAdvection() {
        // Next value array
        double[][] newDensities = new double[gridHeight][gridWidth];

        // Loop through each cell
        for (int i = 0; i < gridHeight; i++) {
            for (int j = 0; j < gridWidth; j++) {
                // Get cell
                Cell cell = grid.getCell(i, j);

                // Calculate and clamp source position
                double fx = j - (cell.velocityX * timestep);
                double fy = i - (cell.velocityY * timestep);
                fx = Math.max(0.5, Math.min(gridWidth - 1.5, fx));
                fy = Math.max(0.5, Math.min(gridHeight - 1.5, fy));

                // Integer and fractional components
                int ix = (int) Math.floor(fx);
                int iy = (int) Math.floor(fy);
                double jx = fx - ix;
                double jy = fy - iy;

                // X neighbour indices and densities
                int ix1 = Math.min(ix + 1, gridWidth - 1);
                double bottomLeft = grid.getCell(ix, iy).density;
                double bottomRight = grid.getCell(ix1, iy).density;

                // Y neighbour indices and densities
                int iy1 = Math.max(iy - 1, 0);
                double topLeft = grid.getCell(ix, iy1).density;
                double topRight = grid.getCell(ix1, iy1).density;

                // 1st lerps
                double z1 = lerp(bottomLeft, bottomRight, jx);
                double z2 = lerp(topLeft, topRight, jx);

                // Final lerp
                newDensities[j][i] = lerp(z1, z2, jy);
            }
        }

        // Apply new densities
        for (int i = 0; i < gridHeight; i++) {
            for (int j = 0; j < gridWidth; j++) {
                grid.getCell(i, j).density = newDensities[i][j];
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
        double[][] newDensities = new double[gridHeight][gridWidth];

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
                    newDensities[j][i] = (cell.density + surroundingDensity * diffusionConstant) / (1 + diffusionConstant);
                }
            }
            // Apply nextDensity values to current density
            for (int i = 0; i < gridHeight; i++) {
                for (int j = 0; j < gridWidth; j++) {
                    grid.getCell(i, j).density = newDensities[j][i];
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
            for (int i = 0; i < gridHeight; i++) {
                for (int j = 0; j < gridWidth; j++) {
                    // Get cell
                    Cell cell = grid.getCell(i, j);

                    // Calculate surrounding velocities
                    double surroundingVelocityX = calculateSurroundingAttributes(i, j, 2);
                    double surroundingVelocityY = calculateSurroundingAttributes(i, j, 3);

                    // Update previous values
                    cell.updatePreviousState();

                    // Calculate new velocities
                    newVelocityX[j][i] = (cell.velocityX + surroundingVelocityX * diffusionConstant) / (1 + diffusionConstant);
                    newVelocityY[j][i] = (cell.velocityY + surroundingVelocityY * diffusionConstant) / (1 + diffusionConstant);
                }
            }
            // Apply new values to current velocities
            for (int i = 0; i < gridHeight; i++) {
                for (int j = 0; j < gridWidth; j++) {
                    grid.getCell(i, j).velocityX = newVelocityX[j][i];
                    grid.getCell(i, j).velocityY = newVelocityY[j][i];
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