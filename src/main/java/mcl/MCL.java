package mcl;

import mcl.Gui.*;
import mcl.Objects.*;

import java.util.Random;
import java.util.ArrayList;

import org.apache.commons.math3.distribution.NormalDistribution;

public class MCL {

    Random rand = new Random(); // Random.nextDouble() is slightly better than Math.random()
    OGM map;
    private Frame frame;

    private ArrayList<Particle> currentParticles = new ArrayList<Particle>();
    int numParticles;
    boolean usingGui = false;

    // Sensor model parameters
    static double stdevhit = 60;
    static double lambdashort = 1;
    static double zhit = 1.3;
    static double zshort = 0.01;
    static double zmax = 0.01;
    static double zrand = 0.01;

    // Motion model parameters
    static double a1 = 0.05; // spreads out in all directions.
    static double a2 = 0.1; // spreads out theta.
    static double a3 = 0.2; // spreads out in the translational direction
    static double a4 = 0; // spreads out a significant amount in translational direction
    static double a5 = 0;
    static double a6 = 0;
    // Note: Odometry model uses 4 parameters, while velocity model uses 6. I just made them share the same variables
    // Because I'm only using one

    public MCL(OGM map, int numParticles) {
        this.map = map;
        this.numParticles = numParticles;
        //this.currentParticles = generateRandomParticles(map, numParticles);
        this.currentParticles = generateUniformParticles(map);
    }

    public MCL(OGM map, int numParticles, Frame frame) {
        this.map = map;
        this.numParticles = numParticles;
        //this.currentParticles = generateRandomParticles(map, numParticles);
        this.currentParticles = generateUniformParticles(map);

        this.frame = frame;
        this.frame.setTitle("Monte Carlo Localization");
        this.usingGui = true;

    }

    // Warning: This could run indefinitely if the map has no vacant cells!!
    // Generates particles evenly throughout the map
    public ArrayList<Particle> generateRandomParticles(OGM map, int numGenerated) { 
        ArrayList<Particle> newParticles = new ArrayList<Particle>();
        for (int i = 0; i < numGenerated; i++) {
            double xrand = rand.nextDouble() * map.getWidth() + map.getOrigin().getX();
            double yrand = rand.nextDouble() * map.getHeight() + map.getOrigin().getY();
            double thetarand = rand.nextDouble() * 360;
            if (!map.isOccupied(xrand, yrand)) { 
                newParticles.add(new Particle(xrand, yrand, thetarand));
            } else {
                i--;
            }
        }
        return newParticles;
    }

    // Puts a particle the center of every empty cell
    public ArrayList<Particle> generateUniformParticles(OGM map) { 
        ArrayList<Particle> newParticles = new ArrayList<Particle>();
        for (int col = 0; col < map.getCols(); col++) {
            for (int row = 0; row < map.getRows(); row++) {
                if (map.getBinaryMap()[col][row] != 1) {
                    newParticles.add(map.centerOfCell(row, col));
                }
            }
        }
        currentParticles.addAll(newParticles); 
        return newParticles;
    }

    // Given a certain angle, finds the distance to the first obstruction along that angle
    // It basically steps along an angle until it hits something
    private double raycast(OGM map, Particle particle, double angle, double minRange, double maxRange) {
        double stepSize = map.getResolution();
        double phi = particle.getTheta() + angle;
        double range = minRange;

        while(range <= maxRange) {
            double x = particle.getX() + range * Math.cos(Math.toRadians(phi)); // x and y of the tip of the ray
            double y = particle.getY() + range * Math.sin(Math.toRadians(phi));

            if (x > map.getWidth() + map.getOrigin().getX() || x < map.getOrigin().getX()) {
                break;
            }
            if (y > map.getHeight() + map.getOrigin().getY() || y < map.getOrigin().getY()) {
                break;
            }

            if (map.isOccupied(x, y)) {
                break;
            }
            range += stepSize; // 
        }
        return range;
    }

    private ArrayList<Particle> errorSquaredWeigh(ArrayList<Particle> particles, ArrayList<Scan> scans) {
        for (Particle particle : particles) {
            double weight = 0;
            for (Scan scan : scans) {
                double raycast = raycast(map, particle, scan.getAngle(), Scan.MIN_RANGE, Scan.MAX_RANGE);
                double scanDistance = scan.getDistance();
                weight += Math.pow((scanDistance - raycast), 2); // Square the error (the difference between raycast and actual scan)
            }
            particle.setWeight(weight);
        }
        softmaxWeights(particles);
        return particles;
    }

    // Performs softmax equation on all particle weights. Makes it so the sum of all weights is 1.
    private void softmaxWeights(ArrayList<Particle> particles) {
        double totalExpWeight = 0;
        for (Particle particle : particles) {
            double expWeight = Math.exp(particle.getWeight());
            particle.setWeight(expWeight);
            totalExpWeight += expWeight;
        }
        for (Particle particle : particles) {
            particle.setWeight(particle.getWeight()/totalExpWeight);
        } 
    }

    // Thrun's measurement_model begins here
    private double phit(double scan, double raycast) {
        // Return 0 if out of scan's bounds
        // Thrun put scan < 0 but I replaced it with scan < Scan.MIN_RANGE because it should be impossible for a sensor to return below its range
        if (scan < Scan.MIN_RANGE || scan > Scan.MAX_RANGE) {
            return 0;
        }
        NormalDistribution nd = new NormalDistribution(raycast, stdevhit);
        double N = nd.density(scan);
        double eta = nd.cumulativeProbability(Scan.MAX_RANGE) - nd.cumulativeProbability(Scan.MIN_RANGE);
        return N*eta;
    }

    private double pshort(double scan, double raycast) {
        if (scan < 0 || scan > raycast) {
            return 0;
        }
        double normalizer = 1 - Math.exp(-lambdashort*raycast);
        return lambdashort*Math.exp(-lambdashort*scan)/normalizer;
    }

    // IMPORTANT: While it says pmax, this actually calculates the probability of a sensor failure. 
    // Oftentimes, a sonar returns its max range when it fails and doesn't recieve it's ping. However,
    // when I was using the Scanse Sweep, it actually returned a distance of 1 when it failed (when it scanned 40+ meters away)
    // If your sensor does something weird like that, I would advise figuring out what it returns when it fails and using that value
    private double pmax(double scan) {
        if (scan == Scan.MAX_RANGE) { 
            return 1;
        }
        return 0;
    }

    // This just adds random noise
    private double prand(double scan) { 
        if (scan < 0 || scan >= Scan.MAX_RANGE) return 1/Scan.MAX_RANGE;
        return 0;
    }

    // Thrun's weighing algorithm for a range scanner
    private void beam_range_finder_model(ArrayList<Scan> scans, Particle particle, OGM map) {
        double q = 1;
        for (Scan scan : scans) {
            double raycast = raycast(map, particle, scan.getAngle(), Scan.MIN_RANGE, Scan.MAX_RANGE);
            double p = zhit*phit(scan.getDistance(), raycast) + zshort*pshort(scan.getDistance(), raycast) + zmax*pmax(scan.getDistance()) + zrand*prand(scan.getDistance());
            q *= p;
        }
        particle.setWeight(q);
    }

    // Thrun's algorithm for setting zhit, zshort, zmax, and zhit
    public void learn_intrinsic_parameters(ArrayList<Scan> scans, Particle robotPos, OGM map) {
        double probHitSum = 0;
        double probShortSum = 0;
        double probMaxSum = 0;
        double probRandSum = 0;
        double hitErrorSquared = 0;
        double shortTimesScan = 0;

        double zSum = 0;
        for (Scan scan : scans) {
            double raycast = raycast(map, robotPos, scan.getAngle(), Scan.MIN_RANGE, Scan.MAX_RANGE);
            double psum = phit(scan.getDistance(), raycast) + pshort(scan.getDistance(), raycast) + pmax(scan.getDistance()) + prand(scan.getDistance());
            if (psum == 0) continue; // to avoid dividing by 0
            double eta = Math.pow(psum, -1);
            if (Double.isInfinite(eta)) continue; 
            // Every once in a while this algorithm returns NaN for the parameters because 
            // of the infinitessimally small weights. But it should work with normal data
            
            probHitSum += eta*phit(scan.getDistance(), raycast);
            probShortSum += eta*pshort(scan.getDistance(), raycast);
            probMaxSum += eta*pmax(scan.getDistance());
            probRandSum += eta*prand(scan.getDistance());

            hitErrorSquared += eta*phit(scan.getDistance(), raycast) * Math.pow(scan.getDistance() - raycast, 2);
            shortTimesScan += eta*pshort(scan.getDistance(), raycast) * scan.getDistance();
            
            zSum += scan.getDistance();
        }

        double zMag = Math.sqrt(zSum);

        zhit = probHitSum/zMag;
        zshort = probShortSum/zMag;
        zmax = probMaxSum/zMag;
        zrand = probRandSum/zMag;
        
        // to avoid dividing by 0
        if (probHitSum != 0) stdevhit = Math.sqrt(hitErrorSquared/probHitSum);
        if (shortTimesScan != 0) lambdashort = probShortSum/shortTimesScan;

        System.out.println(String.format("zhit: %s zshort: %s zmax: %s zrand: %s \nstdevhit: %s lambdashort: %s", zhit, zshort, zmax, zrand, stdevhit, lambdashort));
    }

    // Thrun's motion models begin here

    // Randomly picks a number from a normal distribution with a given variance
    private double sample(double variance) {
        return rand.nextGaussian() * Math.sqrt(variance);
    }

    // Samples from Thrun's motion_model_odometry
    private Particle sample_motion_model_odometry(Particle particle, double deltaX, double deltaY, double deltaTheta) {

        double x = particle.getX();
        double y = particle.getY();
        double theta = Math.toRadians(particle.getTheta()); 
        // I don't fully understand how Math.atan2 works, but its range is -pi to pi, and with the occupancy mapper,
        // theta had to be converted to -pi to pi. So hopefully this is right
        if (particle.getTheta() > 180) {
            theta = Math.toRadians(particle.getTheta() - 360);
        }

        double rot1 = Math.atan2(deltaY, deltaX) - theta;
        double trans = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
        double rot2 = deltaTheta - rot1;
        
        double rot1hat = rot1 - sample(a1*Math.pow(rot1, 2) + a2*Math.pow(trans, 2));
        double transhat = trans - sample(a3*Math.pow(trans, 2) + a4*Math.pow(rot1, 2) + a4*Math.pow(rot2, 2));
        double rot2hat = rot2 - sample(a1*Math.pow(rot2, 2) + a2*Math.pow(trans, 2));

        double newX = x + transhat*Math.cos(theta + rot1hat);
        double newY = y + transhat*Math.sin(theta + rot1hat);
        double newTheta = theta + rot1hat + rot2hat;

        // For some reason, particle = odometryModel doesn't actually modify the particle so we're translating instead.
        particle.translate(transhat*Math.cos(theta + rot1hat), transhat*Math.sin(theta + rot1hat), Math.toDegrees(rot1hat + rot2hat));
        return new Particle(newX, newY, Math.toDegrees(newTheta));
    }

    // Samples from Thrun's motion_model_velocity
    private Particle sample_motion_model_velocity(Particle particle, double velocity, double angularVelocity, double deltaT) {
        if (velocity == 0 && angularVelocity == 0) return particle;

        double x = particle.getX();
        double y = particle.getY();
        double theta = Math.toRadians(particle.getTheta());

        double vHat = velocity + sample(a1*Math.pow(velocity, 2) + a2*Math.pow(angularVelocity, 2));
        double omegaHat = angularVelocity + sample(a3*Math.pow(velocity, 2) + a4*Math.pow(angularVelocity, 2)); 
        double gammaHat = sample(a5*Math.pow(velocity, 2) + a6*Math.pow(angularVelocity, 2));
        
        double newX = x - (vHat*Math.sin(theta)/omegaHat) + (vHat*Math.sin(theta + omegaHat*Math.toRadians(deltaT)));
        double newY = y + (vHat*Math.cos(theta)/omegaHat) - (vHat*Math.cos(theta + omegaHat*Math.toRadians(deltaT)));
        double newTheta = theta + omegaHat*Math.toRadians(deltaT) + gammaHat*Math.toRadians(deltaT);

        double deltaX = (vHat*Math.sin(theta + omegaHat*Math.toRadians(deltaT))) - (vHat*Math.sin(theta)/omegaHat);
        double deltaY = (vHat*Math.cos(theta)/omegaHat) - (vHat*Math.cos(theta + omegaHat*Math.toRadians(deltaT)));
        double deltaTheta = omegaHat*Math.toRadians(deltaT) + gammaHat*Math.toRadians(deltaT);

        particle.translate(deltaX, deltaY, Math.toDegrees(deltaTheta));
        return new Particle(newX, newY, Math.toDegrees(newTheta), particle.getWeight());
    }

    // Stochastic Importance Sampling. I'm not sure if this is the best resampling algorithm to use, but it's the most common
    private ArrayList<Particle> resampleParticles(ArrayList<Particle> oldParticles, int numResampled) {
        int index = (int)Math.round(rand.nextDouble() * (oldParticles.size() - 1)); // finds a random index in the array of particles
        double beta = 0.0;
        double maxWeight = findBestParticle(oldParticles).getWeight();
        
        ArrayList<Particle> newParticles = new ArrayList<Particle>();
        newParticles.add(Robot.getBestParticle());
        newParticles.add(Robot.getWeightedAverage());
        for (int i = 0; i < numResampled - 2; i++) {
            beta += rand.nextDouble() * maxWeight;
            while (beta > oldParticles.get(index).getWeight()) {
                beta -= oldParticles.get(index).getWeight();
                index = (index + 1) % oldParticles.size(); // Loops back to beginning to avoid out of bounds exception
            }
            Particle resampledParticle = oldParticles.get(index);
            newParticles.add(new Particle(
                resampledParticle.getX() + rand.nextGaussian()*map.getResolution()/4, 
                resampledParticle.getY() + rand.nextGaussian()*map.getResolution()/4, 
                resampledParticle.getTheta() + rand.nextGaussian(), 
                resampledParticle.getWeight()));
            //newParticles.add(new Particle(resampledParticle.getX(), resampledParticle.getY(), resampledParticle.getTheta(), resampledParticle.getWeight()));
        }
        return newParticles;
    }

    // This is another resampler that I don't understand and which does not work in any way whatsoever, but here it is
    // public ArrayList<Particle> resample(ArrayList<Particle> oldParticles, int numResampled) {
    //     ArrayList<Particle> newParticles = new ArrayList<Particle>();
    //     double factor = 1 / oldParticles.size();
    //     double r = factor * rand.nextDouble();
    //     double c = oldParticles.get(0).getWeight();
    //     double u;

    //     int i = 0;
    //     for (int m = 0; m < numResampled; ++m) {
    //         u = r + factor*m;
    //         while( u > c) {
    //             if (++i >= oldParticles.size()) break;
    //             c += oldParticles.get(i).getWeight();
    //         }
    //         Particle resampledParticle = oldParticles.get(i);
    //         newParticles.add(new Particle(resampledParticle.getX(), resampledParticle.getY(), resampledParticle.getTheta(), factor));
    //     }

    //     return newParticles;
    // }

    private ArrayList<Particle> MCLAlgorithm(ArrayList<Particle> previousParticles, ArrayList<Scan> sensorData, double deltaX, double deltaY, double deltaTheta, int numParticles) {
        ArrayList<Particle> newParticles = new ArrayList<Particle>();
        motionUpdate(previousParticles, deltaX, deltaY, deltaTheta);
        sensorUpdate(previousParticles, sensorData);
        Robot.updateBestParticle(findBestParticle(previousParticles));
        Robot.updateWeightedAverage(findWeightedAverage(previousParticles));
        newParticles = resampleParticles(previousParticles, numParticles*2/3);
        newParticles.addAll(generateRandomParticles(map, numParticles/3));
        return newParticles;
    }

    public void sensorUpdate(ArrayList<Particle> particles, ArrayList<Scan> scans) {
        for (Particle particle : particles) {
            beam_range_finder_model(scans, particle, map);
        }
    }

    public void motionUpdate(double deltaX, double deltaY, double deltaTheta) {
        for (Particle particle : currentParticles) {
            sample_motion_model_odometry(particle, deltaX, deltaY, deltaTheta);
        }
    }

    public void motionUpdate(ArrayList<Particle> particles, double deltaX, double deltaY, double deltaTheta) {
        for (Particle particle : particles) {
            // sample_velocity_motion_model(particle, deltaForward, deltaTheta, deltaT));
            sample_motion_model_odometry(particle, deltaX, deltaY, deltaTheta);
        }
    }

    public void update(double deltaX, double deltaY, double deltaTheta, ArrayList<Scan> sensorData, int numParticles) {
        currentParticles = MCLAlgorithm(currentParticles, sensorData, deltaX, deltaY, deltaTheta, numParticles);

        if (usingGui) {
            frame.panel.setParticles(currentParticles);
            frame.panel.repaint();
            frame.setTitle(printEstimates());
        }
    }

    public String printEstimates() {
        return "bestParticle: " + findBestParticle(currentParticles).printCoords() + " weightedAve: " + findWeightedAverage(currentParticles).printCoords();
    }

    public void printParticles() {
        for (Particle particle : currentParticles) {
            System.out.println(particle);
        }
    }

    private Particle findWeightedAverage(ArrayList<Particle> particles) {
        double xAverage = 0;
        double yAverage = 0;
        double thetaAverage = 0;
        double weightSum = 0;
        for (Particle particle : particles) {
            xAverage += particle.getX() * particle.getWeight();
            yAverage += particle.getY() * particle.getWeight();
            thetaAverage += particle.getTheta() * particle.getWeight();
            weightSum += particle.getWeight();
        }
        xAverage /= weightSum;
        yAverage /= weightSum;
        thetaAverage /= weightSum;
        if (weightSum != 0) {
            return new Particle(xAverage, yAverage, thetaAverage, weightSum);
        } else {
            return Robot.getWeightedAverage();
        }
    }

    private Particle findBestParticle(ArrayList<Particle> particles) {
        Particle tempParticle = new Particle();
        double maxWeight = 0;
        for (Particle particle : particles) {
            if (particle.getWeight() > maxWeight) {
                maxWeight = particle.getWeight();
                tempParticle = particle;
            }
        }
        if (tempParticle.getWeight() != 0) {
            return tempParticle;
        } else {
            return Robot.getBestParticle();
        }
    }

    public ArrayList<Scan> generateFakeScans(Particle position) {
        ArrayList<Scan> fakeScans = new ArrayList<Scan>();
        for (int i = 1; i <= 100; i++) {
            double r = rand.nextDouble();
            double angle = 3.6*i;
            double distance = raycast(map, position, angle, Scan.MIN_RANGE, Scan.MAX_RANGE) - r;
            //if (r < 0.001) distance = Scan.MAX_RANGE;
            fakeScans.add(new Scan(distance, angle));
        }
        return fakeScans;
    }

    public void simulate(Particle truePos, int numSimulations) {
        Robot.updateTruePos(truePos);
        learn_intrinsic_parameters(generateFakeScans(Robot.getTruePos()), truePos, map);

        for (int simulation = 0; simulation < numSimulations; simulation++) {
            System.out.println(simulation);
            update(0, 0, 0, generateFakeScans(Robot.getTruePos()), numParticles);
            System.out.println(printEstimates());
        }
    }
}