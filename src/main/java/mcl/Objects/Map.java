package mcl.Objects;

import java.util.ArrayList;

public class Map {

    private double width, height, resolution; // This would be in the real-world units of the scanner (i.e. cm, inches, etc.)
    private int rows, cols; // Resolution is the width and height of each cell
    private Particle origin;
    private int[][] binaryMap;
    private double[][] logOdds; // Thrun uses the logOdds of the occupancies for the algorithm
    private double[][] occupancyMap; // But the actual probability of being occupied is given by occupancyMap

    // Thrun's occupancy grid mapper gives each cell a probability of being occupied (a decimal between 0 and 1)
    // Unfortunately, the raycasting in this implementation of MCL requires a binary occupancy (its occupied or its not)
    // The best solution I could find was using Math.random() and running each cell's probability to assign them 1 or 0,
    // then using that binary map for MCL. There are most likely better solutions to this, but the algorithm itself works fine.

    // Parameters specific to the sensor for the occupancy grid mapping
    private double maxrange = 4000; // 
    private double alpha = 1;
    private double beta = 0.127;
    private double pm = 0.6; // The amount of confidence needed to decide that a cell is occupied or free. 60% or more is occ. 40% or less is free.
    private double lnaught = Math.log(1); // assume prior occupancy is just 0.5. The log odds would be 0.
    private double locc = Math.log(pm/(1-pm)); 
    private double lfree = Math.log((1-pm)/pm);

    public Map() {
        this.width = 100;
        this.height = 100;
        this.resolution = 1;

        this.rows = (int)Math.ceil(height/resolution);  
        this.cols = (int)Math.ceil(width/resolution);   
        this.binaryMap = new int[cols][rows];              
        this.origin = new Particle(width/2, height/2);

        this.logOdds = new double[cols][rows];
        this.occupancyMap = new double[cols][rows];

        boxMap(binaryMap);
        fillArray(occupancyMap, 0.5);
    }

    public Map(double width, double height, double resolution, Particle origin) {
        this.width = width;
        this.height = height;
        this.resolution = resolution;

        this.rows = (int)Math.ceil(height/resolution);  // This can be problematic because of truncation. The truncation becomes
        this.cols = (int)Math.ceil(width/resolution);   // particularly prominent when using the gui. Make sure that the
        this.binaryMap = new int[cols][rows];           // height and width divided by the resolution fits the aspect ratio
        this.origin = origin;                           // Example: Map(1920, 1080, 10)
        
        this.logOdds = new double[cols][rows];
        this.occupancyMap = new double[cols][rows];
    }

    public Map(double width, double height, int rows, int cols, Particle origin) { 
        this.width = width;
        this.height = height;
        this.resolution = Math.min(width/cols, height/rows); 
        // If the ratio between rows and cols is different from that of width and height,
        // then the cells won't be squares, and the resolution can't be represented by one number.
        // Make sure it's square, or the raycasting won't work.

        this.rows = rows;
        this.cols = cols;
        this.binaryMap = new int[cols][rows];
        this.origin = origin;
        
        this.logOdds = new double[cols][rows];
        this.occupancyMap = new double[cols][rows];
    }

    public void boxMap(int[][] matrix) { // Sets the borders of the matrix to 1. Was a useful function for testing
        for (int x = 0; x < cols; x++) {
            matrix[x][0] = 1;
            matrix[x][rows-1] = 1;
        }
        for (int y = 0; y < rows; y++) {
            matrix[0][y] = 1;
            matrix[cols-1][y] = 1;
        }
    }

    public void fillArray(double[][] matrix, double value) { // Fills occupancy array with the value
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                matrix[i][j] = value;
            }
        }
    }

    public void setMatrix() { // Sets the binaryMap based of the probability of the occupancyMap
        for (int i = 0; i < this.cols; i++) {
            for (int j = 0; j < this.rows; j++) {
                double rand = Math.random();
                if (rand <= this.occupancyMap[i][j]) {
                    this.binaryMap[i][j] = 1;
                } else {
                    this.binaryMap[i][j] = 0;
                }
            }
        }
    }

    public boolean isOccupied(double x, double y) { // returns whether the cell at which (x, y) is located is occupied
        double gx = (x - this.origin.getX()) * this.cols / this.width; // converts to "matrix units"
        double gy = (this.height - (y - this.origin.getY())) * this.rows / this.height;
        int col = Math.min(Math.max((int)gx, 0), this.cols-1);
        int row = Math.min(Math.max((int)gy, 0), this.rows-1);
        return this.binaryMap[col][row] == 1;
    }

    public double getOccupancy(double x, double y) { // returns the probability that the cell at (x, y) is occupied
        double gx = (x - this.origin.getX()) * this.cols / this.width; 
        double gy = (this.height - (y - this.origin.getY())) * this.rows / this.height;
        int col = Math.min(Math.max((int)gx, 0), this.cols-1);
        int row = Math.min(Math.max((int)gy, 0), this.rows-1);
        return this.occupancyMap[col][row];
    }

    public Particle centerOfCell(int row, int col) { // returns the center of a cell in cartesian coordinates
        double x = this.origin.getX() + (col + 1/2) * this.width / this.cols;
        double y = this.origin.getY() + this.height - (row + 1/2) * this.height / this.rows;
        return new Particle(x, y);
    }

    // Thrun's inverse_sensor_model begins here: 
    public double inverse_range_sensor_model(int row, int col, Particle robotPos, ArrayList<Scan> scans) {

        Particle centerOfCell = centerOfCell(row, col);
        double deltaX = centerOfCell.getX() - robotPos.getX();
        double deltaY = centerOfCell.getY() - robotPos.getY();

        double r = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
        double phi = Math.atan2(deltaY, deltaX) - Math.toRadians(robotPos.getTheta());

        // atan2 returns a theta from -pi to pi, so the theta should be converted to that range as well.
        if (robotPos.getTheta() > 180) {
            phi = Math.atan2(deltaY, deltaX) - Math.toRadians(robotPos.getTheta() - 360);
        }

        double deltaThetas[] = new double[scans.size()];
        for (int i = 0; i < scans.size(); i++) { 
            double scan = scans.get(i).getAngle();
            if (scan > 180) {
                scan -= 360; // Converts from 0 to 360 to -180 to 180
            }
            scan = Math.toRadians(scan); // Converts to -pi to pi
            deltaThetas[i] = Math.abs(phi - scan);
        }
        int k = argmin(deltaThetas); // Finds what angle from the robot points to the given map cell
        Scan closestScan = scans.get(k);

        if (deltaThetas[k] > beta/2 || r > Math.min(maxrange, closestScan.getDistance() + alpha/2)) {
            return lnaught;
        } else if (closestScan.getDistance() < maxrange && (Math.abs(r - closestScan.getDistance()) < alpha/2)) {
            return locc;
        } else if (r <= closestScan.getDistance()) {
            return lfree;
        } else {
            return lnaught;
        }
    }

    public void occupancy_grid_mapping(ArrayList<Scan> scans, Particle robotPos) {
        double newLogOdds[][] = new double[this.cols][this.rows];

        for (int col = 0; col < this.cols; col++) {
            for (int row = 0; row < this.rows; row++) {
                newLogOdds[col][row] = logOdds[col][row] + inverse_range_sensor_model(row, col, robotPos, scans) - lnaught;
            }
        }

        logOdds = newLogOdds;
        occupancyMap = logOddsToProbability(logOdds);
    }

    public double[][] logOddsToProbability(double[][] logOdds) {
        double probabilities[][] = new double[logOdds.length][logOdds[0].length];
        for (int i = 0; i < logOdds.length; i++) {
            for (int j = 0; j < logOdds[i].length; j++) {
                probabilities[i][j] = 1 - (1 / (1+Math.exp(logOdds[i][j])));
            }
        }
        return probabilities;
    }

    // I looked for libraries with argmin and couldn't find any except for stanford core nlp. 
    // Which is kinda odd, because it's a language processing library.
    // Instead, I just implemented it here.
    public int argmin(double[] array) { 
        double minimumValue = Double.MAX_VALUE;
        int argminIndex = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] < minimumValue) {
                minimumValue = array[i];
                argminIndex = i;
            }
        }
        return argminIndex;
    }

    public double getWidth() {
        return this.width;
    }

    public double getHeight() {
        return this.height;
    }

    public double getResolution() {
        return this.resolution;
    }

    public Particle getOrigin() {
        return this.origin;
    }

    public int getRows() {
        return this.rows;
    }

    public int getCols() {
        return this.cols;
    }

    public int[][] getBinaryMap() {
        return this.binaryMap;
    }

    public double[][] getOccupancyMap() {
        return this.occupancyMap;
    }

    public void setOrigin(Particle origin) {
        this.origin = origin;
    }

}