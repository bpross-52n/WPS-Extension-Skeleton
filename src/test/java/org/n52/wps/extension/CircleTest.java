package org.n52.wps.extension;

public class CircleTest {

    public static void main(String[] args) {
        
        double magnitude = 7;
        
        double width = 0.005;
        
        for (double counter = magnitude; counter >= 4; counter--) {
            
            width = width * 2;
            
            System.out.println("Magnitude: " + counter + ", width " + width);
        }

    }

}
