package org.pack;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ML extends MouseAdapter {

    private boolean isPressed = false;
    private int x = 0, y = 0;

    @Override
    public void mousePressed(MouseEvent event) {
        isPressed = true;
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        isPressed = false;
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        x = event.getX();
        y = event.getY();
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        x = event.getX();
        y = event.getY();
    }

    public int getX() { return x; }

    public int getY() { return y; }

    public boolean isPressed() { return isPressed; }
}
