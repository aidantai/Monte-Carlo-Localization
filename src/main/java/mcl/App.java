package mcl;

import mcl.Objects.*;
import mcl.Gui.Frame;

public class App {
    public static void main(String[] args) {
        OGM map = new OGM(150, 100, 2, new Particle(-75, -50));
        map.setBoxMap();

        Frame frame = new Frame(map, 900, 600);

        MCL monty = new MCL(map, 300, frame);
        frame.addMCLObject(monty);
        monty.simulate(new Particle(0.5, 0.5), 20000);
    }
}
