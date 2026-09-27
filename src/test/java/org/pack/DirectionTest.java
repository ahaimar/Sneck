package org.pack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectionTest {

    @Test
    void stepsAreUnitLength() {
        for (Direction direction : Direction.values()) {
            assertEquals(1, Math.abs(direction.dx()) + Math.abs(direction.dy()),
                    direction + " must move exactly one cell");
        }
    }

    @Test
    void oppositesAreMutual() {
        for (Direction direction : Direction.values()) {
            assertEquals(direction, direction.opposite().opposite());
            assertNotEquals(direction, direction.opposite());
        }
    }

    @Test
    void axisIsReported() {
        assertTrue(Direction.LEFT.isHorizontal());
        assertTrue(Direction.RIGHT.isHorizontal());
        assertFalse(Direction.UP.isHorizontal());
        assertTrue(Direction.UP.isVertical());
        assertTrue(Direction.DOWN.isVertical());
        assertFalse(Direction.LEFT.isVertical());
    }
}
