package org.pack;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mouse state. Listeners are attached to the drawing surface rather than the frame, so the
 * coordinates are already relative to the game area and need no title-bar/bar offset fixing.
 */
public class ML extends MouseAdapter {

    private final AtomicBoolean clickPending = new AtomicBoolean();
    private volatile int x;
    private volatile int y;

    @Override
    public void mousePressed(MouseEvent event) {
        moveTo(event);
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        moveTo(event);
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        moveTo(event);
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        moveTo(event);
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (event.getButton() == MouseEvent.BUTTON1) {
            clickPending.set(true);
        }
        moveTo(event);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /** True once per click, then latches the click back off. */
    public boolean consumePress() {
        return clickPending.getAndSet(false);
    }

    public void flush() {
        clickPending.set(false);
    }

    private void moveTo(MouseEvent event) {
        x = event.getX();
        y = event.getY();
    }
}
