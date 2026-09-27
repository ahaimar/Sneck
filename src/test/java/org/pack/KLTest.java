package org.pack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KLTest {

    private static final Component SOURCE = new JPanel();

    private static KeyEvent press(int keyCode) {
        return new KeyEvent(SOURCE, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    private static KeyEvent release(int keyCode) {
        return new KeyEvent(SOURCE, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    @Test
    void reportsHeldAndReleasedKeys() {
        KL keys = new KL();
        int code = KeyEvent.VK_UP;

        assertFalse(keys.isDown(code));
        keys.keyPressed(press(code));
        assertTrue(keys.isDown(code));
        keys.keyReleased(release(code));
        assertFalse(keys.isDown(code));
    }

    @Test
    @DisplayName("a press is reported once, so holding a key cannot re-trigger a turn")
    void pressIsEdgeTriggered() {
        KL keys = new KL();
        int code = KeyEvent.VK_LEFT;
        keys.keyPressed(press(code));

        assertTrue(keys.consumePress(code));
        assertFalse(keys.consumePress(code));
        assertFalse(keys.consumePress(code));
    }

    @Test
    void aSecondPhysicalPressFiresAgain() {
        KL keys = new KL();
        int code = KeyEvent.VK_RIGHT;
        keys.keyPressed(press(code));
        assertTrue(keys.consumePress(code));

        keys.keyReleased(release(code));
        keys.keyPressed(press(code));
        assertTrue(keys.consumePress(code));
        assertFalse(keys.consumePress(code));
    }

    @Test
    @DisplayName("flushing stops a scene's keys leaking into the next one")
    void flushClearsEverything() {
        KL keys = new KL();
        keys.keyPressed(press(KeyEvent.VK_ENTER));
        keys.keyPressed(press(KeyEvent.VK_SPACE));

        keys.flush();

        assertFalse(keys.isDown(KeyEvent.VK_ENTER));
        assertFalse(keys.isDown(KeyEvent.VK_SPACE));
        assertFalse(keys.consumePress(KeyEvent.VK_ENTER));
        assertFalse(keys.consumePress(KeyEvent.VK_SPACE));
    }

    @Test
    void outOfRangeKeyCodesAreIgnored() {
        KL keys = new KL();

        keys.keyPressed(press(-1));
        keys.keyPressed(press(9999));

        assertFalse(keys.isDown(-1));
        assertFalse(keys.isDown(9999));
        assertFalse(keys.consumePress(9999));
    }
}
