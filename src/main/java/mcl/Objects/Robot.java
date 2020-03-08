package mcl.Objects;

public class Robot {

    private static Particle bestParticle = new Particle();
    private static Particle weightedAverage = new Particle();

    public Particle getBestParticle() {
        return bestParticle;
    }

    public Particle getWeightedAverage() {
        return weightedAverage;
    }

    public void updateBestParticle(Particle particle) {
        bestParticle = particle;
    }

    public void updateWeightedAverage(Particle particle) {
        weightedAverage = particle;
    }
}