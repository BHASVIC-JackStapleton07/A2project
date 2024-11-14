import java.lang.Math;
public class Simulator {
    // References
    private Grid grid;
    // Variables
   int gridHeight; int gridWidth;
   int maxIterations = 20;
   public int delay = 16; // GUI timestep
    double timestep = 1; // Simulator timestep
    double diffusionConstant = 5;

    public Simulator() {
        grid = new Grid(200, 200);
        gridHeight = grid.getHeight();
        gridWidth = grid.getWidth();

        // Test cell
        grid.getCell(100, 100).density = 4000;
    }

    public void stepSimulation() {
        applyDiffusion();
        applyAdvection();
        maintainZeroDivergence();
    }

    private void applyAdvection() {
        // Loop through each cell
        for (int i = 0; i < gridHeight; i++) {
            for (int j = 0; j < gridWidth; j++) {
                // Get cell
                Cell cell = grid.getCell(i, j);

                // Source location
                double fx = j - (cell.velocityX * timestep);
                double fy = i - (cell.velocityY * timestep);

                // Integer values (ensure in bounds)
                int ix = Math.max(0, Math.min(gridWidth - 2, (int) Math.floor(fx)));
                int iy = Math.max(0, Math.min(gridHeight - 2, (int) Math.floor(fy)));

                // Fractional values
                double jx = fx - ix;
                double jy = fy - iy;

                // Round 1 lerps
                double z1 = lerp(grid.getCell(ix, iy).density, grid.getCell(ix+1, iy).density, jx);
                double z2 = lerp(grid.getCell(ix, iy+1).density, grid.getCell(ix+1, iy+1).density, jx);

                // Final lerp
                cell.density = lerp(z1, z2, jy);
            }
        }
    }

    private void applyDiffusion() {
        solveDensities();
        solveVelocities();
    }

    private void maintainZeroDivergence() {

    }

    private void solveDensities() {
        for (int n = 0; n < maxIterations; n++) {
            // Avoid boundary collisions
            for (int i = 1; i < gridHeight-1; i++) {
                for (int j = 1; j < gridWidth-1; j++) {
                    Cell cell = grid.getCell(i, j);
                    double surroundingDensity = calculateSurroundingAttributes(i, j, 1);

                    cell.updatePreviousState();
                    cell.density = (cell.density + surroundingDensity * diffusionConstant) / (1 + diffusionConstant);
                }
            }
        }
    }

    private void solveVelocities() {
        for (int n = 0; n < maxIterations; n++) {
            // Avoid boundary collisions
            for (int i = 1; i < gridHeight-1; i++) {
                for (int j = 1; j < gridWidth-1; j++) {
                    Cell cell = grid.getCell(i, j);
                    double surroundingVelocityX = calculateSurroundingAttributes(i, j, 2);
                    double surroundingVelocityY = calculateSurroundingAttributes(i, j, 3);

                    cell.updatePreviousState();
                    cell.velocityX = (cell.velocityX + surroundingVelocityX * diffusionConstant) / (1 + diffusionConstant);
                    cell.velocityY = (cell.velocityY + surroundingVelocityY * diffusionConstant) / (1 + diffusionConstant);
                }
            }
        }
    }


    private double calculateSurroundingAttributes(int height, int width, int attribute) {
        int num = 0;
        double valueTotal = 0;

        //1: density. 2: velocityX. 3: velocityY
        if (width > 0) {
            if (attribute == 1) {
                valueTotal += grid.getCell(height, width-1).density;
            } else if (attribute == 2) {
                valueTotal += grid.getCell(height, width-1).velocityX;
            } else{
                valueTotal += grid.getCell(height, width-1).velocityY;
            }
            num++;
        }
        if (width < gridWidth-1) {
            if (attribute == 1) {
                valueTotal += grid.getCell(height, width+1).density;
            } else if (attribute == 2) {
                valueTotal += grid.getCell(height, width+1).velocityX;
            } else{
                valueTotal += grid.getCell(height, width+1).velocityY;
            }
            num++;
        }
        if (height > 0) {
            if (attribute == 1) {
                valueTotal += grid.getCell(height-1, width).density;
            } else if (attribute == 2) {
                valueTotal += grid.getCell(height-1, width).velocityX;
            } else{
                valueTotal += grid.getCell(height-1, width).velocityY;
            }
            num++;
        }
        if (height < gridHeight-1) {
            if (attribute == 1) {
                valueTotal += grid.getCell(height+1, width).density;
            } else if (attribute == 2) {
                valueTotal += grid.getCell(height+1, width).velocityX;
            } else{
                valueTotal += grid.getCell(height+1, width).velocityY;
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

