public class Simulator {
    private Grid grid;

    public Simulator() {
        grid = new Grid(100, 100);
    }

    public void stepSimulation() {
        applyAdvection();
        applyDiffusion();
        maintainZeroDivergence();
    }

    private void applyAdvection() {

    }

    private void applyDiffusion() {

    }

    private void maintainZeroDivergence() {

    }

    // getter
    public Grid getGrid() {
        return grid;
    }
}

