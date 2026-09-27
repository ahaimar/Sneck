package org.pack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RectTest {

    @Test
    void centres() {
        Rect rect = new Rect(10, 20, 100, 50);

        assertEquals(60.0, rect.centerX());
        assertEquals(45.0, rect.centerY());
    }

    @Test
    void containsIsInclusiveOfTheEdges() {
        Rect rect = new Rect(10, 10, 100, 50);

        assertTrue(rect.contains(10, 10));
        assertTrue(rect.contains(110, 60));
        assertTrue(rect.contains(60, 35));
        assertFalse(rect.contains(9.9, 35));
        assertFalse(rect.contains(60, 60.1));
    }
}
