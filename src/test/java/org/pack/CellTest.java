package org.pack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CellTest {

    @Test
    void equalityIsByValue() {
        assertEquals(new Cell(3, 4), new Cell(3, 4));
        assertNotEquals(new Cell(3, 4), new Cell(4, 3));
    }

    @Test
    void translateMovesOneCell() {
        Cell origin = new Cell(5, 5);

        assertEquals(new Cell(6, 5), origin.translate(Direction.RIGHT));
        assertEquals(new Cell(4, 5), origin.translate(Direction.LEFT));
        assertEquals(new Cell(5, 4), origin.translate(Direction.UP));
        assertEquals(new Cell(5, 6), origin.translate(Direction.DOWN));
        assertEquals(origin, origin.translate(0, 0));
    }

    @Test
    void centreIsTheMiddleOfTheCell() {
        Cell cell = new Cell(2, 3);

        assertEquals(2 * 24 + 12.0, cell.centerX(24));
        assertEquals(3 * 24 + 12.0, cell.centerY(24));
    }
}
