package org.pack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSceneTest {

    private final KL keys = new KL();
    private final SceneHarness.FakeHost host = new SceneHarness.FakeHost();

    private GameScene scene(HighScore highScore, Random random) {
        return SceneHarness.newGameScene(host, keys, highScore, random);
    }

    /** Food lands anywhere but reproducibly, so a run is a real run and not a set piece. */
    private GameScene scene() {
        return scene(new HighScore(Path.of(System.getProperty("java.io.tmpdir"), "unused-highscore"), 0),
                new SceneHarness.SeededRandom());
    }

    @Test
    @DisplayName("the snake waits on the start line until the player asks it to move")
    void waitsForTheFirstInput() {
        GameScene scene = scene();
        Cell start = scene.snake().head();

        SceneHarness.advance(scene, 120);

        assertEquals(GameScene.Phase.READY, scene.phase());
        assertEquals(start, scene.snake().head());
        assertEquals(4, scene.length());
    }

    @Test
    void firstDirectionStartsTheRun() {
        GameScene scene = scene();
        int startY = scene.snake().head().y();

        SceneHarness.tap(keys, KeyEvent.VK_UP);
        SceneHarness.advance(scene, 30);

        assertEquals(GameScene.Phase.RUNNING, scene.phase());
        assertTrue(scene.snake().head().y() < startY, "the snake should have travelled upwards");
    }

    @Test
    @DisplayName("one cell is covered per move interval, not per frame")
    void movementFollowsTheMoveInterval() {
        GameScene scene = scene();
        SceneHarness.tap(keys, KeyEvent.VK_UP);
        SceneHarness.advance(scene, 1);
        assertEquals(GameScene.Phase.RUNNING, scene.phase());

        Cell head = scene.snake().head();
        int steps = 0;
        while (scene.snake().head().equals(head) && steps < 200) {
            SceneHarness.advance(scene, 1);
            steps++;
        }

        int expected = (int) Math.ceil(scene.moveInterval() / SceneHarness.STEP);
        assertEquals(expected, steps, "the snake must not move sooner than its interval allows");
    }

    @Test
    @DisplayName("the snake can reach the food, which scores a point and makes it longer")
    void eatsFoodAndScores() {
        GameScene scene = scene();
        double startInterval = scene.moveInterval();

        SceneHarness.driveUntilEaten(scene, keys, 400);

        assertEquals(Constants.SCORE_PER_FOOD, scene.score());
        assertEquals(1, scene.snake().pendingGrowth(), "the extra segment lands on the next cell");
        assertEquals(5, SceneHarness.lengthIncludingPendingGrowth(scene), "the snake ends up a segment longer");
        assertEquals(4, scene.length(), "but not before it takes that step");
        assertEquals(Constants.SCORE_PER_FOOD, host.lastTitleScore);
        assertTrue(scene.moveInterval() < startInterval, "the snake speeds up as it eats");
        assertTrue(scene.moveInterval() >= Difficulty.NORMAL.minMoveSeconds());
        assertTrue(scene.food().isSpawned(), "a new piece of food appears straight away");
        assertNotEquals(scene.snake().head(), scene.food().position());
    }

    @Test
    @DisplayName("food never appears underneath the snake, over many rounds")
    void foodStaysOnFreeCells() {
        GameScene scene = scene();

        for (int round = 1; round <= 6; round++) {
            SceneHarness.driveUntilEaten(scene, keys, 400);
            assertTrue(scene.food().isSpawned(), "a new piece of food is placed after every meal");
            assertTrue(scene.snake().isOnBoard(scene.food().position()));
            assertTrue(!scene.snake().occupies(scene.food().position()), "round " + round + " spawned on the snake");
            assertEquals(round * Constants.SCORE_PER_FOOD, scene.score());
            assertEquals(4 + round, SceneHarness.lengthIncludingPendingGrowth(scene),
                    "round " + round + " left the snake the right length");
        }
    }

    @Test
    @DisplayName("driving straight into a wall ends the run")
    void wallCollisionEndsTheRun() {
        // The food is parked in the opposite corner, so nothing is eaten on the way to the wall.
        GameScene scene = scene(new HighScore(Path.of(System.getProperty("java.io.tmpdir"), "unused-highscore"), 0),
                new SceneHarness.FirstCellRandom());

        SceneHarness.tap(keys, KeyEvent.VK_RIGHT);
        SceneHarness.driveInDirection(scene, keys, Direction.RIGHT, 200);

        assertEquals(GameScene.Phase.GAME_OVER, scene.phase());
        assertEquals(0, scene.score());
    }

    @Test
    @DisplayName("the final score is banked as a new best")
    void gameOverBanksTheScore(@TempDir Path dir) {
        HighScore highScore = new HighScore(dir.resolve("highscore"), 0);
        GameScene scene = scene(highScore, new SceneHarness.SeededRandom());

        SceneHarness.driveUntilEaten(scene, keys, 400);
        assertTrue(scene.score() > 0);

        // Hold one heading until the run ends against a wall.
        SceneHarness.driveInDirection(scene, keys, Direction.DOWN, 600);

        assertEquals(GameScene.Phase.GAME_OVER, scene.phase());
        assertTrue(scene.score() > 0, "the meals eaten on the way still count");
        assertEquals(scene.score(), highScore.value(), "the final score is banked as the best");
        assertFalse(highScore.submit(scene.score() / 2), "a worse score never replaces the best");
        assertEquals(scene.score(), highScore.value());
    }

    @Test
    @DisplayName("the speed ramp is clamped so the snake never gets unplayably fast")
    void speedIsCapped() {
        assertEquals(Difficulty.NORMAL.startMoveSeconds(), scene().moveInterval(), 1e-9);

        double uncapped = Difficulty.NORMAL.startMoveSeconds()
                - (Constants.BOARD_COLUMNS * Constants.BOARD_ROWS) * Difficulty.NORMAL.speedupPerFood();
        assertTrue(uncapped < Difficulty.NORMAL.minMoveSeconds(),
                "a full board would want to beat the cap, so the clamp has to hold");
    }

    @Test
    void escapeReturnsToTheMenu() {
        GameScene scene = scene();

        SceneHarness.tap(keys, KeyEvent.VK_UP);
        SceneHarness.advance(scene, 1);
        SceneHarness.tap(keys, KeyEvent.VK_ESCAPE);
        SceneHarness.advance(scene, 1);

        assertEquals(GameState.MENU, host.state);
        assertEquals(1, host.stateChanges);
    }

    @Test
    @DisplayName("pause freezes the snake and unpause releases it")
    void pauseAndResume() {
        GameScene scene = scene();
        SceneHarness.tap(keys, KeyEvent.VK_UP);
        SceneHarness.advance(scene, 30);
        Cell before = scene.snake().head();

        SceneHarness.tap(keys, KeyEvent.VK_P);
        SceneHarness.advance(scene, 1);
        assertEquals(GameScene.Phase.PAUSED, scene.phase());
        SceneHarness.advance(scene, 600);
        assertEquals(before, scene.snake().head(), "a long pause must not bank up movement");

        SceneHarness.tap(keys, KeyEvent.VK_P);
        SceneHarness.advance(scene, 1);
        assertEquals(GameScene.Phase.RUNNING, scene.phase());
        SceneHarness.advance(scene, 30);
        assertNotEquals(before, scene.snake().head());
    }

    @Test
    @DisplayName("after a crash, space plays again with a fresh snake")
    void restartResetsEverything() {
        GameScene scene = scene();
        SceneHarness.driveUntilEaten(scene, keys, 400);
        assertTrue(scene.score() > 0);

        SceneHarness.driveInDirection(scene, keys, Direction.UP, 200);
        assertEquals(GameScene.Phase.GAME_OVER, scene.phase());

        SceneHarness.tap(keys, KeyEvent.VK_SPACE);
        SceneHarness.advance(scene, 1);

        assertEquals(GameScene.Phase.READY, scene.phase());
        assertEquals(0, scene.score());
        assertEquals(Difficulty.NORMAL.startLength(), scene.length());
        assertEquals(Difficulty.NORMAL.startMoveSeconds(), scene.moveInterval(), 1e-9);
    }

    @Test
    @DisplayName("keys are ignored until released when a scene is entered")
    void enteringASceneFlushesInput() {
        GameScene scene = scene();
        keys.keyPressed(new java.awt.event.KeyEvent(new javax.swing.JPanel(),
                KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
                KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED));

        scene.onEnter();
        SceneHarness.advance(scene, 30);

        assertEquals(GameScene.Phase.READY, scene.phase(),
                "the key held while the menu was up must not start the run");
    }

    @Test
    @DisplayName("a fixed random seed makes a run reproducible")
    void sameSeedSameRun() {
        HighScore highScore = new HighScore(Path.of(System.getProperty("java.io.tmpdir"), "unused-highscore"), 0);
        GameScene first = scene(highScore, new Random(99));
        GameScene second = scene(highScore, new Random(99));

        assertEquals(first.food().position(), second.food().position());
        assertNotEquals(first.food().position(),
                scene(highScore, new Random(100)).food().position(), "a different seed gives a different board");
    }
}
