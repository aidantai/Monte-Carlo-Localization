package mcl;

import mcl.Objects.*;
import mcl.Gui.Frame;

public class App {
    public static void main(String[] args) {
        OGM map = new OGM(100, 100, 1, new Particle(-50, -50));
        map.boxMap(map.getBinaryMap());

        Frame frame = new Frame(map, 600, 600);
        
        System.out.println("Hello World!");
    }
}
