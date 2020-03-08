package mcl.Gui;

import mcl.Objects.OGM;

import javax.swing.JFrame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

@SuppressWarnings("serial")
public class Frame extends JFrame implements KeyListener {

    public Panel panel;
    OGM map;

    public Frame() {
        this.map = new OGM();
        this.panel = new Panel();
        this.add(panel);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(800, 800);
        this.setResizable(false);
        this.setVisible(true);

    }

    public Frame(OGM map, int width, int height) {
        this.panel = new Panel(map);
        this.add(panel);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(width, height + 22); // 22 was the pixel height of the title bar for me, so the actual map would be width by height
        //this.setUndecorated(true); // If you want, you can remove the title bar here
        this.setResizable(true); // Also, you might have to calculate the height in pixels of your own title bar and change it here
        this.setVisible(true);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // switch (e.getKeyCode()) {
        //     case KeyEvent.VK_W:
        //         this.monty.update(0, 1, 0, this.monty.generateFakeSweepData(new Particle(Robot.getX(), Robot.getY()+1)));
        //     case KeyEvent.VK_A:
        //         this.monty.update(-1, 0, 0, this.monty.generateFakeSweepData(new Particle(Robot.getX()-1, Robot.getY())));
        //     case KeyEvent.VK_S:
        //         this.monty.update(0, -1, 0, this.monty.generateFakeSweepData(new Particle(Robot.getX(), Robot.getY()-1)));
        //     case KeyEvent.VK_D:
        //         this.monty.update(1, 0, 0, this.monty.generateFakeSweepData(new Particle(Robot.getX()+1, Robot.getY())));
        // }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    public int getFrameWidth() {
        return this.getWidth();
    }

    public int getFrameHeight() {
        return this.getHeight();
    }
}