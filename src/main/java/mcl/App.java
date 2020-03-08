package mcl;

import mcl.Objects.*;

public class App {
    public static void main(String[] args) {
        Map map = new Map(100, 100, 1, new Particle(-50, -50));
        map.boxMap(map.getBinaryMap());
        
        System.out.println("Hello World!");
    }
}
