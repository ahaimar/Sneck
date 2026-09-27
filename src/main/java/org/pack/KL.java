package org.pack;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Keyboard state shared between the AWT event thread (which writes) and the game loop thread
 * (which polls), so every field is atomic.
 *
 * <p>{@link #consumePress(int)} is edge triggered: it reports a key once per physical press,
 * which is what the snake needs. A key that is simply held down must not be able to re-trigger
 * a turn every frame, and auto-repeat is ignored so a held key cannot re-fire either.
 */
public class KL extends KeyAdapter {

    private static final int KEY_COUNT = 256;

    private final AtomicIntegerArray down = new AtomicIntegerArray(KEY_COUNT);
    private final AtomicIntegerArray pressed = new AtomicIntegerArray(KEY_COUNT);

    @Override
    public void keyPressed(KeyEvent event) {
        int code = event.getKeyCode();
        if (!isTrackable(code)) {
            return;
        }
        // A held key keeps producing KEY_PRESSED events. Only the one that transitions the key
        // from up to down is a real press, so auto-repeat can never re-fire a turn.
        if (down.compareAndSet(code, 0, 1)) {
            pressed.set(code, 1);
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        int code = event.getKeyCode();
        if (isTrackable(code)) {
            down.set(code, 0);
        }
    }

    public boolean isDown(int keyCode) {
        return isTrackable(keyCode) && down.get(keyCode) == 1;
    }

    /** True once per physical press, then latches the key back off. */
    public boolean consumePress(int keyCode) {
        return isTrackable(keyCode) && pressed.getAndSet(keyCode, 0) == 1;
    }

    /** Forgets every key, so state carried over from a previous scene cannot leak into the next. */
    public void flush() {
        for (int i = 0; i < KEY_COUNT; i++) {
            down.set(i, 0);
            pressed.set(i, 0);
        }
    }

    private static boolean isTrackable(int keyCode) {
        return keyCode >= 0 && keyCode < KEY_COUNT;
    }
}
