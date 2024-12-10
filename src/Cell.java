public class Cell {
    // velocity
    public double velocityX;
    public double velocityY;

    // pressure
    public double pressure;

    // density
    public double density;

    // previous values
    public double prevVelocityX;
    public double prevVelocityY;
    public double prevDensity;

    // forces
    public double forceX;
    public double forceY;

    // temperature
    public double temperature;

    // boundary (0 if boundary, 1 if fluid)
    public int boundary;

    //Constructor
    public Cell() {
        this.velocityX = 100.0;
        this.velocityY = 100.0;
        this.pressure = 0.0;
        this.density = 0.0;
        this.prevVelocityX = 0.0;
        this.prevVelocityY = 0.0;
        this.prevDensity = 0.0;
        this.forceX = 0.0;
        this.forceY = 0.0;
        this.temperature = 0.0;
        this.boundary = 1;
    }

    // reset force
    public void resetForces() {
        this.forceX = 0.0;
        this.forceY = 0.0;
    }

    // update previous
    public void updatePreviousState() {
        this.prevVelocityX = this.velocityX;
        this.prevVelocityY = this.velocityY;
        this.prevDensity = this.density;
    }
}

