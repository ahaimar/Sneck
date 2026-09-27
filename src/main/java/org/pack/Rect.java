package org.pack;

public class Rect {

    public double x, y, width, height;

    public Rect(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double centerX() {
        return x + width / 2.0;
    }

    public double centerY() {
        return y + height / 2.0;
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
}
