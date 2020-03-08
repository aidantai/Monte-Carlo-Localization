package mcl.Objects;

public class Robot {

    private static Particle bestParticle = new Particle();
    private static Particle weightedAverage = new Particle();

    public static Particle getBestParticle() {
        return bestParticle;
    }

    public static Particle getWeightedAverage() {
        return weightedAverage;
    }

    public static void updateBestParticle(Particle particle) {
        bestParticle = particle;
    }

    public static void updateWeightedAverage(Particle particle) {
        weightedAverage = particle;
    }
}