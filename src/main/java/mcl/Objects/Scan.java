package mcl.Objects;


// This scan object is just an example. You'll need to implement your own device and use it's own scan object.
// Our Scanse Sweep had SweepSample objects with an angle, distance, and signalStrength.
public class Scan {
    
    private double distance; // distance in cm.

    private double angle; // angle in degrees.

    public Scan(int distance, int angle) {
        this.distance = distance;
        this.angle = angle;
    }

    public double getDistance() {
        return this.distance;
    }
    
    public double getAngle() {
        return this.angle;
    }

}