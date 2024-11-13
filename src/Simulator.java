public class Simulator {
    // References
    private Grid grid;
    private Physics physics = new Physics();
    // Variables
   int gridHeight; int gridWidth;
   int maxIterations = 20;
   public int delay = 16; // Timestep
    double diffusionConstant = 12;

    public Simulator() {
        grid = new Grid(100, 100);
        gridHeight = grid.getHeight();
        gridWidth = grid.getWidth();

        // Test cell
        Cell testCell = grid.getCell(1, 1);
        Cell testCell2 = grid.getCell(98, 98);
        Cell testCell3 = grid.getCell(50, 50);
        testCell.density = 500;
        testCell2.density = 500;
        testCell3.density = 500;
    }

    public void stepSimulation() {
        applyAdvection();
        applyDiffusion();
        maintainZeroDivergence();
    }

    private void applyAdvection() {

    }

    private void applyDiffusion() {
        solveDenstities();
    }

    private void maintainZeroDivergence() {

    }

    private void solveDenstities() {
        for (int n = 0; n < maxIterations; n++) {
            for (int i = 0; i < gridHeight; i++) {
                for (int j = 0; j < gridWidth; j++) {
                    Cell cell = grid.getCell(i, j);
                    double surroundingDensity = calculateSurroundingDensity(i, j);

                    cell.updatePreviousState();
                    cell.density = (cell.density + surroundingDensity * diffusionConstant) / (1 + diffusionConstant);
                }
            }
        }
    }


    private double calculateSurroundingDensity(int height, int width) {
        int num = 0;
        double densityTotal = 0;
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
        double surroundingDensity = densityTotal / num;
        return surroundingDensity;
    }

    // getter
    public Grid getGrid() {
        return grid;
    }
}

