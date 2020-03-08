package mcl.Gui;

import mcl.Objects.*;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.util.*;

@SuppressWarnings("serial")
public class Panel extends JPanel {

    private ArrayList<Particle> particles; // List of particles to draw (particles are relative to map)
    private ArrayList<Particle> points; // List of points to draw. Points take in exact positions, no conversion
    private OGM map; 

    private int robotSize = 6; // Radius of robot's circle
    private boolean drawGrid = true; 

    
    public Panel() {
        this.particles = new ArrayList<Particle>();
        this.points = new ArrayList<Particle>();
        this.map = new OGM();
    }

    public Panel(OGM map) {
        this.particles = new ArrayList<Particle>();
        this.points = new ArrayList<Particle>();
        this.map = map;
    }

    public void setParticles(ArrayList<Particle> particles) {
        this.particles = particles;
    }

    public void setPoints(ArrayList<Particle> points) {
        this.points = points;
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;
        g2d.clearRect(0, 0, (int)this.getSize().getWidth(), (int)this.getSize().getHeight());

        if (drawGrid) { // I made drawGrid a boolean because sometimes the resolution is so small the gridlines fill every pixel on the screen
            g2d.setColor(Color.BLACK); // also, the grid can be hard on the eyes, but if the cells have an occupancy of 0.5,
            drawGrid(g2d);             // then fillCells covers the whole panel with 50% transparent white and makes it grey
        }

        fillCells(g2d);

        for (Particle particle : this.particles) {
            // I was hoping to make higher weight particles blue, but since all weights are basically between 0 and 0.02, they're all red
            g2d.setColor(new Color((float)(1-particle.getWeight()), 0.0f, (float)(particle.getWeight()), 1.0f));
            this.drawParticle(particle, g2d);
        }

        g2d.setColor(Color.GREEN);
        for (Particle point : this.points) {
            this.plotPoint(point, g2d);
        }

        g2d.setColor(Color.YELLOW);
        this.drawRobot(g2d, Robot.getBestParticle());
        g2d.setColor(Color.BLUE);
        this.drawRobot(g2d, Robot.getWeightedAverage());
    }

    private void drawGrid(Graphics2D g) {
        int cellHeight = (int)(this.getSize().getHeight() / map.getRows());
        int cellWidth = (int)(this.getSize().getWidth() / map.getCols());

        for (int row = 0; row <= map.getRows(); row++) {
            g.drawLine(0, row * cellHeight, (int)this.getSize().getWidth(), row * cellHeight);

        }
        for (int col = 0; col <= map.getCols(); col++) {
            g.drawLine(col * cellWidth, 0, col * cellWidth, (int)this.getSize().getHeight());
        }
    }

    private void fillCells(Graphics2D g) {
        int cellHeight = (int)(this.getSize().getHeight() / map.getRows());
        int cellWidth = (int)(this.getSize().getWidth() / map.getCols());

        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                float transparency = (float)map.getOccupancyMap()[col][row];
                float occupied = (float)(1-map.getBinaryMap()[col][row]);

                // black = occupied, white = empty, and transparency is determined by its probability of being occupied
                // this means that a cell with 0.1 probability that still ended up being occupied would be tinted grey, while
                // a cell that has a 0.9 probability but ended up being empty would be white
                g.setColor(new Color(occupied, occupied, occupied, transparency));
                g.fillRect(col * cellWidth, row * cellHeight, cellWidth, cellHeight);
            }
        }
    }

    // Basically just draws an oval of robot size at the particle's position
    private void drawRobot(Graphics2D g, Particle robotPos) {
        g.fillOval(
            (int)( (robotPos.getX()-map.getOrigin().getX()) * (int)this.getSize().getWidth()/map.getWidth() - robotSize/2 ),
            (int)( (map.getHeight()-(robotPos.getY() - map.getOrigin().getY())) * (int)this.getSize().getHeight()/map.getHeight() - robotSize/2 ), 
            robotSize, robotSize);
    }

    private void drawParticle(Particle p, Graphics2D g) {
        g.fillOval(
            (int)( (p.getX()-map.getOrigin().getX()) * (int)(this.getSize().getWidth()/map.getWidth())), 
            (int)( (map.getHeight()-(p.getY() - map.getOrigin().getY())) * (int)(this.getSize().getHeight()/map.getHeight())),
            2, 2);
    }

    private void plotPoint(Particle p, Graphics2D g) {
        g.fillOval((int)p.getX(), (int)p.getY(), 3, 3);
    }

}