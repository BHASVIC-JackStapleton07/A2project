import java.lang.Math;
public class Simulator {
    // References
    private final Grid grid;
    // Variables
   int gridHeight;
   int gridWidth;
   int maxIterations = 15;
   public int delay = 1*16; // GUI timestep
    double timestep = 0.05; // Simulator timestep
    double diffusionConstant = 1;

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
    }

    // Main procedures
    public void stepSimulation() {
        // Physics
        addTapSource();
        applyAdvection();
        applyDiffusion();
        //maintainZeroDivergence();
        applyBoundaryConditions();

        // Debug
        debugTotalDensity();
    }

    private void addTapSource() {
        // Define the amount of density and velocity added by the tap
        double densitySource = 100;  // Amount of density to add

        // Get the cell at the tap's location
        Cell tapCell = grid.getCell(50, 50);

        // Add density and velocity to the cell
        tapCell.density += densitySource;
        tapCell.velocityY = 18; // Blowing downwards
    }


    // Advection
    private void applyAdvection() {
        // Loop through all cells
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                // Get cell
                Cell cell = grid.getCell(x, y);

                // Skip boundaries
                if (cell.boundary == 0) {
                    continue;
                }

                // Find source positions
                double fx = x - cell.velocityX * timestep;
                double fy = y - cell.velocityY * timestep;

                // Clamp f-values (with a half allowance)
                fx = Math.max(0.5, Math.min(gridWidth - 1.5, fx));
                fy = Math.max(0.5, Math.min(gridHeight - 1.5, fy));

                // Calculate Integer values
                int ix = (int) Math.floor(fx);
                int iy = (int) Math.floor(fy);

                // Fractional weights
                double jx = fx - ix;  // Horizontal fraction
                double jy = fy - iy;  // Vertical fraction
                double kx = 1.0 - jx;
                double ky = 1.0 - jy;

                // Bilinear interpolation

                cell.density = ky * (kx * grid.getCell(ix, iy).density + jx * grid.getCell(ix + 1, iy).density)
                                + jy * (kx * grid.getCell(ix, iy + 1).density + jx * grid.getCell(ix + 1, iy + 1).density);

                cell.velocityX = ky * (kx * grid.getCell(ix, iy).velocityX + jx * grid.getCell(ix + 1, iy).velocityX)
                        + jy * (kx * grid.getCell(ix, iy + 1).velocityX + jx * grid.getCell(ix + 1, iy + 1).velocityX);

                cell.velocityY = ky * (kx * grid.getCell(ix, iy).velocityY + jx * grid.getCell(ix + 1, iy).velocityY)
                        + jy * (kx * grid.getCell(ix, iy + 1).velocityY + jx * grid.getCell(ix + 1, iy + 1).velocityY);

            }
        }
    }

    // Diffusion
    private void applyDiffusion() {
        // Gauss-Seidel iteration
        for (int n = 0; n < maxIterations; n++) {
            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x < gridWidth; x++) {
                    // Get cell
                    Cell cell = grid.getCell(x, y);

                    // Skip boundary
                    if (cell.boundary == 0) {
                        continue;
                    }

                    // Calculate surrounding values
                    double surroundingDensity = calculateSurroundingValue(x, y, 0);
                    double surroundingVelocityX = calculateSurroundingValue(x, y, 1);
                    double surroundingVelocityY = calculateSurroundingValue(x, y, 2);

                    // Diffuse values
                    //cell.density = (surroundingDensity + cell.density * diffusionConstant) / (1 + 4 * diffusionConstant);
                    cell.velocityX = (surroundingVelocityX + cell.velocityX * diffusionConstant) / (1 + 4 * diffusionConstant);
                    cell.velocityY = (surroundingVelocityY + cell.velocityY * diffusionConstant) / (1 + 4 * diffusionConstant);
                }
            }
        }
    }

    // Zero-Divergence
    private void maintainZeroDivergence() {
        solvePressurePoissonEquation();
        updateVelocityField();
    }

    private double calculateDivergence(int x, int y) {
        Cell cell = grid.getCell(x, y); // Get cell

        // Approximation of divergence
        double divergence =
                (grid.getCell(x + 1, y).velocityX - grid.getCell(x - 1, y).velocityX) / 2.0
                        + (grid.getCell(x, y + 1).velocityY - grid.getCell(x, y - 1).velocityY) / 2.0;

        return divergence;
    }

    private void solvePressurePoissonEquation() {
        double tolerance = 1e-4; // Convergence tolerance

        for (int n = 0; n < maxIterations; n++) {
            double maxChange = 0.0;

            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x < gridWidth; x++) {
                    Cell cell = grid.getCell(x,y); // Get cell

                    // Skip boundaries
                    if (cell.boundary == 0) {
                        continue;
                    }

                    // Compute new pressure value
                    double newPressure = (grid.getCell(x+1,y).pressure + grid.getCell(x-1, y).pressure
                                        + grid.getCell(x,y+1).pressure + grid.getCell(x,y-1).pressure
                                        - calculateDivergence(x,y)) / 4.0;

                    // Track the maximum change in pressure for convergence
                    maxChange = Math.max(maxChange, Math.abs(newPressure - cell.pressure));

                    // Update pressure
                    cell.pressure = newPressure;
                }
            }

            // Break if converged to appropriate tolerance
            if (maxChange < tolerance) {
                break;
            }
        }
    }

    private void updateVelocityField() {
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                Cell cell = grid.getCell(x,y);

                // Skip boundaries
                if (cell.boundary == 0) {
                    continue;
                }

                // Subtract pressure gradient to remove divergence
                cell.velocityX -= (grid.getCell(x + 1, y).pressure - grid.getCell(x - 1, y).pressure) / 2.0;
                cell.velocityY -= (grid.getCell(x, y + 1).pressure - grid.getCell(x, y - 1).pressure) / 2.0;

            }
        }
    }

    // Boundaries
    private void applyBoundaryConditions() {
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                Cell cell = grid.getCell(x, y);

                if (cell.boundary == 0) {
                    cell.velocityX = 0.0;
                    cell.velocityY = 0.0;
                    cell.density = 0.0;  // For simplicity; adjust based on specific boundary rules
                }
            }
        }
    }


    //  Other procedures
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
                if (cell.boundary == 1) {
                    totalDensity += cell.density;
                }
            }
        }
        System.out.println("Density: " + totalDensity);
    }


    // getter
    public Grid getGrid() {
        return grid;
    }
}