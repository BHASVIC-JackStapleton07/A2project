public class Physics {
    // Variables

    // Constructor
    public Physics() {

    }

    public double gaussSeidelSolver(double currentDensity, double surroundingDensity, int maxIterations) {
        double newX; double newY; double newZ; double newP; double newQ; double newDC;
        // Set unknown values to one
        double x = 1; double y = 1; double z = 1; double p = 1; double q = 1;
        double diffusionConstant = 1;
        // Iterate a set amount of times
        for (int i = 0; i < maxIterations; i++) {
            // Apply equations
            newDC = (x - currentDensity) / (surroundingDensity - currentDensity);
            newX = (x + (0.25 * diffusionConstant * (y + z + p + q))) / (1 + diffusionConstant);
            newY = (((q + diffusionConstant) * x) - (0.25 * diffusionConstant * (z + p + q)) - x) / (0.25 * diffusionConstant);
            newZ = (((q + diffusionConstant) * x) - (0.25 * diffusionConstant * (y + p + q)) - x) / (0.25 * diffusionConstant);
            newP = (((q + diffusionConstant) * x) - (0.25 * diffusionConstant * (y + z + q)) - x) / (0.25 * diffusionConstant);
            newQ = (((q + diffusionConstant) * x) - (0.25 * diffusionConstant * (y + z + p)) - x) / (0.25 * diffusionConstant);
            // Update values
            x = newX; y = newY; z = newZ; p = newP; q = newQ; diffusionConstant = newDC;
        }
        return x;
    }
}