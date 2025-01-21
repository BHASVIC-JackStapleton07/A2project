public class Cell {
    // Velocity components
    public double velocityX;
    public double velocityY;

    // Pressure
    public double pressure;

    // Density
    public double density;

    // Forces (unused)
    public double forceX;
    public double forceY;

    // Temperature (unused)
    public double temperature;

    // State. 0 = solid, 1 = fluid
    public int state;

    // Constructor
    public Cell() {
        this.velocityX = 0.0;
        this.velocityY = 0.0;
        this.pressure = 0.0;
        this.density = 0.0;
        this.forceX = 0.0;
        this.forceY = 0.0;
        this.temperature = 0.0;
        this.state = 1;
    }
}

