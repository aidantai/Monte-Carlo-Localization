package mcl.Objects;

public class Robot {

    private static Particle bestParticle = new Particle();
    private static Particle weightedAverage = new Particle();
    private static Particle truePos = new Particle(); // For simulation

    public static Particle getBestParticle() {
        return bestParticle;
    }

    public static Particle getWeightedAverage() {
        return weightedAverage;
    }

    public static Particle getTruePos() {
        return truePos;
    }

    public static void updateBestParticle(Particle particle) {
        bestParticle = particle;
    }

    public static void updateWeightedAverage(Particle particle) {
        weightedAverage = particle;
    }

    public static void updateTruePos(Particle particle) {
        truePos = particle;
    }

    public static void translate(double deltaX, double deltaY, double deltaTheta) {
        truePos.translate(deltaX, deltaY, deltaTheta);
    }
}