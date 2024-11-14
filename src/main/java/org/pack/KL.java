package org.pack;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class KL extends KeyAdapter {

    private boolean[] keyPressed = new boolean[256];

    @Override
    public void keyPressed(KeyEvent event) {
        if (event.getKeyCode() >= 0 && event.getKeyCode() < keyPressed.length) {
            keyPressed[event.getKeyCode()] = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        if (event.getKeyCode() >= 0 && event.getKeyCode() < keyPressed.length) {
            keyPressed[event.getKeyCode()] = false;
        }
    }

    public boolean isKeyPressed(int keyCode) {
        if (keyCode >= 0 && keyCode < keyPressed.length) {
            return keyPressed[keyCode];
        }
        return false;
    }
}
