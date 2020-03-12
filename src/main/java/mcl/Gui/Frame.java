package mcl.Gui;

import mcl.Objects.OGM;
import mcl.Objects.Robot;
import mcl.MCL;

import javax.swing.JFrame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

@SuppressWarnings("serial")
public class Frame extends JFrame implements KeyListener {

    public Panel panel;
    OGM map;
    MCL monty;

    double translation; // distance the WASD keys move the robot

    public Frame() {
        this.map = new OGM();
        this.monty = new MCL(map, 500);
        this.translation = map.getResolution();
        this.panel = new Panel();
        this.add(panel);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(800, 800);
        this.setResizable(false);
        this.addKeyListener(this);
        this.setVisible(true);

    }

    public Frame(OGM map, int width, int height) {
        this.map = map;
        this.monty = new MCL(map, 500);
        this.translation = map.getResolution();
        this.panel = new Panel(map);
        this.add(panel);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(width, height + 22); // 22 was the pixel height of the title bar for me, so the actual map would be width by height
        this.setResizable(true);          // You might have to calculate the height in pixels of your own title bar and change it here
        this.addKeyListener(this);
        this.setVisible(true);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // double deltaX = Math.cos(Math.toRadians(Robot.getTruePos().getTheta()))*this.translation;
        // double deltaY = Math.sin(Math.toRadians(Robot.getTruePos().getTheta()))*this.translation;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
                Robot.translate(0, this.translation, 0);
                monty.motionUpdate(0, this.translation, 0);
                this.repaint();
                break;
            case KeyEvent.VK_A:
                Robot.translate(-this.translation, 0, 0);
                monty.motionUpdate(-this.translation, 0, 0);
                this.repaint();     
                break;
            case KeyEvent.VK_S:
                Robot.translate(0, -this.translation, 0);
                monty.motionUpdate(0, -this.translation, 0);
                this.repaint();   
                break;
            case KeyEvent.VK_D:
                Robot.translate(this.translation, 0, 0);
                monty.motionUpdate(this.translation, 0, 0);
                this.repaint();   
                break;
            case KeyEvent.VK_LEFT:
                Robot.translate(0, 0, 15);
                monty.motionUpdate(0, 0, 15);
                this.repaint();   
                break;
            case KeyEvent.VK_RIGHT:
                Robot.translate(0, 0, -15);
                monty.motionUpdate(0, 0, -15);
                this.repaint();   
                break;
            case KeyEvent.VK_SPACE:
                Robot.translate(50, 50, 0);
                this.repaint();
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public int getFrameWidth() {
        return this.getWidth();
    }

    public int getFrameHeight() {
        return this.getHeight();
    }

    public void addMCLObject(MCL object) {
        this.monty = object;
    }
}