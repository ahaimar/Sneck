package org.pack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnaKeTest {

    private static SnaKe snake(int columns, int rows, int x, int y, int length) {
        return new SnaKe(columns, rows, x, y, length);
    }

    @Test
    @DisplayName("the body trails behind the head and it starts heading right")
    void startsWithBodyBehindHead() {
        SnaKe snake = snake(25, 20, 12, 10, 4);

        assertEquals(List.of(new Cell(12, 10), new Cell(11, 10), new Cell(10, 10), new Cell(9, 10)), snake.segments());
        assertEquals(new Cell(12, 10), snake.head());
        assertEquals(new Cell(9, 10), snake.tail());
        assertEquals(Direction.RIGHT, snake.direction());
        assertEquals(4, snake.length());
    }

    @Test
    void rejectsSnakesThatDoNotFit() {
        assertThrows(IllegalArgumentException.class, () -> snake(25, 20, 0, 10, 4));
        assertThrows(IllegalArgumentException.class, () -> snake(25, 20, 12, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> snake(25, 20, 30, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> snake(25, 20, 12, 99, 1));
    }

    @Test
    @DisplayName("stepping moves the head one cell and keeps the length")
    void stepMovesHead() {
        SnaKe snake = snake(25, 20, 12, 10, 3);

        assertEquals(new Cell(13, 10), snake.step());
        assertEquals(new Cell(13, 10), snake.head());
        assertEquals(new Cell(11, 10), snake.tail());
        assertEquals(3, snake.length());
    }

    @Test
    void everyDirectionMovesOneCell() {
        // A single-segment snake has no body, so it may face any way including backwards.
        assertEquals(new Cell(13, 10), oneStep(snake(25, 20, 12, 10, 1), Direction.RIGHT));
        assertEquals(new Cell(12, 9), oneStep(snake(25, 20, 12, 10, 1), Direction.UP));
        assertEquals(new Cell(12, 11), oneStep(snake(25, 20, 12, 10, 1), Direction.DOWN));
        assertEquals(new Cell(11, 10), oneStep(snake(25, 20, 12, 10, 1), Direction.LEFT));
    }

    private static Cell oneStep(SnaKe snake, Direction direction) {
        snake.queueTurn(direction);
        return snake.step();
    }

    @Test
    @DisplayName("a reversal is refused so the snake cannot run into its own neck")
    void refusesReversal() {
        SnaKe snake = snake(25, 20, 12, 10, 3);

        snake.queueTurn(Direction.LEFT);
        snake.step();

        assertEquals(Direction.RIGHT, snake.direction());
    }

    @Test
    @DisplayName("a buffered second turn is honoured on the next step instead of being lost")
    void buffersTwoTurns() {
        SnaKe snake = snake(25, 20, 12, 10, 3);

        snake.queueTurn(Direction.UP);
        snake.queueTurn(Direction.LEFT);

        assertEquals(new Cell(12, 9), snake.step());
        assertEquals(Direction.UP, snake.direction());
        assertEquals(new Cell(11, 9), snake.step());
        assertEquals(Direction.LEFT, snake.direction());
    }

    @Test
    @DisplayName("a third buffered turn is dropped, so a stale key can never survive a frame")
    void dropsThirdTurn() {
        SnaKe snake = snake(25, 20, 12, 10, 3);

        snake.queueTurn(Direction.UP);
        snake.queueTurn(Direction.LEFT);
        snake.queueTurn(Direction.DOWN);

        snake.step();
        snake.step();
        snake.step();

        assertEquals(Direction.LEFT, snake.direction());
    }

    @Test
    void growAddsOneSegment() {
        SnaKe snake = snake(25, 20, 12, 10, 3);

        snake.grow();
        snake.step();

        assertEquals(4, snake.length());
        assertEquals(new Cell(13, 10), snake.head());
        assertEquals(new Cell(10, 10), snake.tail());

        snake.step();
        assertEquals(4, snake.length());
    }

    @Test
    void growCanBeRequestedRepeatedly() {
        SnaKe snake = snake(25, 20, 12, 10, 2);

        snake.grow();
        snake.grow();
        snake.step();
        snake.step();

        assertEquals(4, snake.length());
        assertEquals(0, snake.pendingGrowth());
    }

    @Test
    @DisplayName("walking off any edge is fatal")
    void diesAtEdges() {
        assertEquals(new Cell(5, 2), walkUntilDead(snake(5, 5, 2, 2, 1), Direction.RIGHT));
        assertEquals(new Cell(2, -1), walkUntilDead(snake(5, 5, 2, 2, 1), Direction.UP));
        assertEquals(new Cell(2, 5), walkUntilDead(snake(5, 5, 2, 2, 1), Direction.DOWN));
        assertEquals(new Cell(-1, 2), walkUntilDead(snake(5, 5, 2, 2, 1), Direction.LEFT));
    }

    /** Runs in one direction until the snake is off the board, and returns the fatal cell. */
    private static Cell walkUntilDead(SnaKe snake, Direction direction) {
        for (int step = 0; step <= snake.columns() + snake.rows(); step++) {
            snake.queueTurn(direction);
            if (snake.isDead()) {
                return snake.nextHead();
            }
            snake.step();
        }
        throw new AssertionError("the snake never left the board heading " + direction);
    }

    @Test
    void survivesInTheMiddleOfTheBoard() {
        assertFalse(snake(5, 5, 2, 2, 3).isDead());
    }

    @Test
    @DisplayName("running into the body is fatal")
    void diesOnSelfCollision() {
        SnaKe snake = snake(10, 10, 5, 5, 5);
        snake.queueTurn(Direction.UP);
        snake.step();
        snake.queueTurn(Direction.LEFT);
        snake.step();
        snake.queueTurn(Direction.DOWN);

        assertTrue(snake.isDead(), "the head should be about to hit its own body");
    }

    @Test
    @DisplayName("sliding into the cell the tail is vacating is legal")
    void mayFollowItsOwnTail() {
        SnaKe snake = filledRing();
        snake.queueTurn(Direction.DOWN);
        assertEquals(snake.tail(), snake.nextHead(), "the ring's only free move is onto its tail");

        assertFalse(snake.isDead(), "the tail moves out of the way in the same step");
        assertEquals(new Cell(4, 5), snake.step());
        assertEquals(4, snake.length());
    }

    @Test
    @DisplayName("a growing snake may not move onto its own tail, which is not vacating")
    void growingSnakeCannotFollowItsTail() {
        SnaKe snake = filledRing();
        snake.grow();
        snake.queueTurn(Direction.DOWN);

        assertTrue(snake.isDead(), "the tail stays put while growing, so it is still solid");
    }

    /** A length four snake coiled into a 2x2 block, with its only legal exit on the tail cell. */
    private static SnaKe filledRing() {
        SnaKe snake = snake(10, 10, 5, 5, 4);
        snake.queueTurn(Direction.UP);
        snake.step();
        snake.queueTurn(Direction.LEFT);
        snake.step();
        assertEquals(List.of(new Cell(4, 4), new Cell(5, 4), new Cell(5, 5), new Cell(4, 5)), snake.segments());
        return snake;
    }

    @Test
    void occupancyAndBounds() {
        SnaKe snake = snake(10, 10, 5, 5, 3);

        assertTrue(snake.occupies(new Cell(4, 5)));
        assertTrue(snake.occupies(new Cell(3, 5)));
        assertFalse(snake.occupies(new Cell(2, 5)));
        assertTrue(snake.isOnBoard(new Cell(0, 0)));
        assertTrue(snake.isOnBoard(new Cell(9, 9)));
        assertFalse(snake.isOnBoard(new Cell(-1, 0)));
        assertFalse(snake.isOnBoard(new Cell(10, 0)));
        assertFalse(snake.isOnBoard(null));
    }

    @Test
    void resetRestoresTheStartingShape() {
        SnaKe snake = snake(10, 10, 8, 5, 2);
        snake.grow();
        snake.queueTurn(Direction.DOWN);
        snake.step();
        snake.step();

        snake.reset(8, 5, 2);

        assertEquals(List.of(new Cell(8, 5), new Cell(7, 5)), snake.segments());
        assertEquals(Direction.RIGHT, snake.direction());
        assertEquals(0, snake.pendingGrowth());
        assertEquals(new Cell(9, 5), snake.nextHead());
        assertFalse(snake.isDead());
    }
}
