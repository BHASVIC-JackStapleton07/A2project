public class Simulator {
    // References
    private Grid grid;
    private Physics physics = new Physics();
    // Variables
   int gridHeight; int gridWidth;
   int maxIterations = 20;

    public Simulator() {
        grid = new Grid(100, 100);
        gridHeight = grid.getHeight();
        gridWidth = grid.getWidth();

        // Test cell
        Cell testCell = grid.getCell(1, 1);
        testCell.density = 80;
    }

    public void stepSimulation() {
        applyAdvection();
        applyDiffusion();
        maintainZeroDivergence();
    }

    private void applyAdvection() {

    }

    private void applyDiffusion() {
        solveDensities();
    }

    private void maintainZeroDivergence() {

    }

    private void solveDensities() {
        // Loop through each cell
        for (int i = 0; i < gridWidth; i++) {
            for (int j = 0; j < gridHeight; j++) {
                // Get cell
                Cell cell = grid.getCell(i, j);
                // Update previous values
                cell.updatePreviousState();

                // Calculate new density
                double currentDensity = cell.density;
                double newDensity = 0;

                // Calculate surrounding density
                double surroundingDensity = calculateSurroundingDensity(j, i);

                // Apply Gauss-Seidel method
                newDensity = physics.gaussSeidelSolver(currentDensity, surroundingDensity, maxIterations);

                //Apply density
                cell.density = newDensity;
            }
        }
    }

    private double calculateSurroundingDensity(int width, int height) {
        int num = 0;
        double densityTotal = 0;
        double surroundingDensity;
        if (width > 0) {
            densityTotal += grid.getCell(height, width-1).density;
            num++;
        }
        if (width < gridWidth-2) {
            densityTotal += grid.getCell(height, width+1).density;
            num++;
        }
        if (height > 0) {
            densityTotal += grid.getCell(height-1, width).density;
            num++;
        }
        if (height < gridHeight-2) {
            densityTotal += grid.getCell(height+1, width).density;
            num++;
        }
        surroundingDensity = densityTotal / num;
        return surroundingDensity;
    }

    // getter
    public Grid getGrid() {
        return grid;
    }
}

