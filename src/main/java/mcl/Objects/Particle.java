package mcl.Objects;

public class Particle {

    private double x, y, theta, weight;
    
    public Particle() {
        this.x = 0;
        this.y = 0;
        this.theta = 0;
        this.weight = 0;
    }

    public Particle(double x, double y) {
        this.x = x;
        this.y = y;
        this.theta = 0;
        this.weight = 0;
    }

    public Particle(double x, double y, double theta) {
        this.x = x;
        this.y = y;
        this.theta = theta;
        this.weight = 0;
    }

    public Particle(double x, double y, double theta, double weight) {
        this.x = x;
        this.y = y;
        this.theta = theta;
        this.weight = weight;
    }

    
    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getTheta() {
        return this.theta;
    }

    public double getWeight() {
        return this.weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void translate(double deltax, double deltay, double deltatheta) {
        this.x += deltax;
        this.y += deltay;
        this.theta += deltatheta;
        this.theta = (this.theta % 360);
    }

    // Notation looks like 3d, but its 2d with angle. (x, y, Θ)
    public String toString() {
        return String.format("(%s, %s, %s) Weight: %s", this.x, this.y, this.theta, this.weight);
    }
    
    public String printCoords() {
        return String.format("(%s, %s, %s)", this.x, this.y, this.theta);
    }

}