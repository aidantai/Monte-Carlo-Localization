package mcl.Objects;

public class Map {

    private double width, height, resolution; // This would be in the real-world units of the scanner (i.e. cm, inches, etc.)
    private int rows, cols; // Resolution is the width and height of each cell.
    private Particle origin;
    private int[][] binaryMap;
    private double[][] logOdds;
    private double[][] occupancyMap;

    // Thrun's occupancy grid mapper gives each cell a probability of being occupied (a decimal between 0 and 1)
    // Unfortunately, the raycasting in this implementation of MCL requires a binary occupancy (its occupied or its not)
    // The best solution I could find was using Math.random() and running each cell's probability to assign them 1 or 0,
    // then using that binary map for MCL. There are most likely better solutions to this, but the algorithm itself works fine.

    public Map() {
        this.width = 100;
        this.height = 100;
        this.resolution = 1;

        this.rows = (int)Math.ceil(height/resolution);  
        this.cols = (int)Math.ceil(width/resolution);   
        this.binaryMap = new int[cols][rows];              
        this.origin = new Particle(0, 0);   

        this.logOdds = new double[cols][rows];
        this.occupancyMap = new double[cols][rows];

        boxMap(binaryMap);
        fillArray(occupancyMap, 0.5);
    }

    public Map(double width, double height, double resolution, Particle particle) {
        this.width = width;
        this.height = height;
        this.resolution = resolution;

        this.rows = (int)Math.ceil(height/resolution);  // This can be problematic because of truncation. The truncation becomes
        this.cols = (int)Math.ceil(width/resolution);   // particularly prominent when using the gui. Make sure that the
        this.binaryMap = new int[cols][rows];           // height and width divided by the resolution fits the aspect ratio
        this.origin = new Particle(0, 0);               // Example: Map(1920, 1080, 10)
        
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


}