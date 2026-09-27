package org.pack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DifficultyTest {

    @Test
    @DisplayName("a fresh run starts at the option's own interval")
    void startsAtItsOwnInterval() {
        for (Difficulty difficulty : Difficulty.values()) {
            assertEquals(difficulty.startMoveSeconds(),
                    difficulty.moveSecondsAfter(0), 1e-9,
                    difficulty + " should start at its starting interval");
        }
    }

    @Test
    @DisplayName("the ramp never drops below the option's floor, however much is eaten")
    void theRampIsClamped() {
        for (Difficulty difficulty : Difficulty.values()) {
            double uncapped = difficulty.startMoveSeconds()
                    - (Constants.BOARD_COLUMNS * Constants.BOARD_ROWS) * difficulty.speedupPerFood();
            assertTrue(uncapped < difficulty.minMoveSeconds(),
                    difficulty + " should reach its floor on a full board, otherwise the floor is dead code");

            assertEquals(difficulty.minMoveSeconds(),
                    difficulty.moveSecondsAfter(Constants.BOARD_COLUMNS * Constants.BOARD_ROWS), 1e-9);
        }
    }

    @Test
    @DisplayName("every interval outlasts several fixed steps, so a cell is never skipped")
    void everyIntervalIsResolvable() {
        for (Difficulty difficulty : Difficulty.values()) {
            assertTrue(difficulty.startMoveSeconds() > 4 * Constants.FIXED_STEP_SECONDS,
                    difficulty + " starts so fast the player cannot react to a cell");
            assertTrue(difficulty.minMoveSeconds() > Constants.FIXED_STEP_SECONDS,
                    difficulty + " is faster than the simulation can step");
        }
    }

    @Test
    void fasterOptionsAreActuallyFaster() {
        for (Difficulty current : Difficulty.values()) {
            Difficulty faster = current.next();
            if (current == Difficulty.INSANE) {
                continue;
            }
            assertTrue(faster.startMoveSeconds() < current.startMoveSeconds(),
                    faster + " should start faster than " + current);
            assertTrue(faster.minMoveSeconds() < current.minMoveSeconds(),
                    faster + " should stay faster than " + current);
        }
    }

    @Test
    @DisplayName("cycling wraps around in both directions")
    void cyclingWraps() {
        assertSame(Difficulty.HARD, Difficulty.NORMAL.next());
        assertSame(Difficulty.EASY, Difficulty.NORMAL.previous());
        assertSame(Difficulty.INSANE, Difficulty.EASY.previous());
        assertSame(Difficulty.EASY, Difficulty.INSANE.next());
        assertSame(Difficulty.INSANE, Difficulty.EASY.previous());
    }

    @Test
    void everyStartLengthFitsTheBoard() {
        for (Difficulty difficulty : Difficulty.values()) {
            assertTrue(difficulty.startLength() >= 1, "a snake needs at least one segment");
            assertTrue(difficulty.startLength() <= Constants.BOARD_COLUMNS,
                    difficulty + " starts longer than the board is wide");
        }
    }

    @Test
    @DisplayName("an unknown or missing name falls back to Normal rather than failing")
    void fromNameFallsBack() {
        assertSame(Difficulty.NORMAL, Difficulty.fromName(null));
        assertSame(Difficulty.NORMAL, Difficulty.fromName(""));
        assertSame(Difficulty.NORMAL, Difficulty.fromName("nightmare"));
        assertSame(Difficulty.HARD, Difficulty.fromName("hard"));
        assertSame(Difficulty.HARD, Difficulty.fromName("  Hard  "));
        assertSame(Difficulty.INSANE, Difficulty.fromName("INSANE"));
    }

    @Test
    void everyOptionHasALabel() {
        for (Difficulty difficulty : Difficulty.values()) {
            assertTrue(!difficulty.label().isBlank(), difficulty + " needs a label to draw");
        }
    }
}
