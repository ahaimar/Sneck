package org.pack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rendering checks. The scenes are drawn into an image and inspected pixel by pixel, which
 * catches the mistakes unit tests on the rules cannot see, such as drawing a board but no
 * snake, or forgetting the head.
 */
class GameSceneRenderTest {

    private final KL keys = new KL();

    private GameScene scene(HighScore highScore) {
        return SceneHarness.newGameScene(new SceneHarness.FakeHost(), keys, highScore, new SceneHarness.SeededRandom());
    }

    private GameScene scene() {
        return scene(new HighScore(Path.of(System.getProperty("java.io.tmpdir"), "unused-highscore"), 0));
    }

    @Test
    @DisplayName("the board, the snake and the food are all on screen")
    void drawsEverything() {
        GameScene scene = scene();
        SceneHarness.tap(keys, KeyEvent.VK_UP);
        SceneHarness.advanceOneCell(scene);

        BufferedImage image = SceneHarness.render(scene);

        assertTrue(SceneHarness.countPixels(image, Constants.COLOR_BOARD) > 1000, "the board is filled");
        assertTrue(SceneHarness.countPixels(image, Constants.COLOR_SNAKE) > 0, "the snake is drawn");
        assertTrue(SceneHarness.countPixels(image, Constants.COLOR_SNAKE_HEAD) > 0, "the head is drawn");
        assertTrue(SceneHarness.countPixels(image, Constants.COLOR_FOOD) > 0, "the food is drawn");
        assertTrue(SceneHarness.countPixels(image, Constants.COLOR_GRID) > 0, "the grid is drawn");
    }

    @Test
    @DisplayName("the head is drawn on top of the body, so the snake has a visible front")
    void headIsDistinctFromTheBody() {
        GameScene scene = scene();

        BufferedImage image = SceneHarness.render(scene);

        int head = SceneHarness.countPixels(image, Constants.COLOR_SNAKE_HEAD);
        int body = SceneHarness.countPixels(image, Constants.COLOR_SNAKE);
        assertTrue(head > 0, "the head must not be skipped when drawing the segments");
        assertTrue(body > head, "a four segment snake has more body than head");
    }

    @Test
    @DisplayName("a longer snake covers more of the board")
    void aLongerSnakeDrawsMore() {
        GameScene scene = scene();
        BufferedImage shortSnake = SceneHarness.render(scene);
        int before = SceneHarness.countPixels(shortSnake, Constants.COLOR_SNAKE);

        for (int round = 0; round < 4; round++) {
            SceneHarness.driveUntilEaten(scene, keys, 400);
        }

        BufferedImage longSnake = SceneHarness.render(scene);
        assertEquals(8, SceneHarness.lengthIncludingPendingGrowth(scene));
        assertTrue(SceneHarness.countPixels(longSnake, Constants.COLOR_SNAKE) > before,
                "a longer snake must be visibly longer");
    }

    @Test
    @DisplayName("the game over overlay covers the board and shows the final score")
    void gameOverOverlayIsDrawn() {
        GameScene scene = scene();
        SceneHarness.tap(keys, KeyEvent.VK_RIGHT);
        SceneHarness.driveInDirection(scene, keys, Direction.RIGHT, 200);
        assertEquals(GameScene.Phase.GAME_OVER, scene.phase());

        BufferedImage image = SceneHarness.render(scene);

        assertEquals(0, SceneHarness.countPixels(image, Constants.COLOR_BOARD), "the overlay hides the board");
        assertEquals(0, SceneHarness.countPixels(image, Constants.COLOR_SNAKE), "and the snake with it");
        assertEquals(0, SceneHarness.countPixels(image, Constants.COLOR_FOOD));
        assertTrue(SceneHarness.countPixels(image, Constants.COLOR_TEXT) > 200, "the overlay text is drawn");
        assertTrue(SceneHarness.countPixels(image, Constants.COLOR_TEXT_ACCENT) > 0, "the retry prompt is drawn");
    }

    @Test
    @DisplayName("the pause overlay covers the board")
    void pauseOverlayIsDrawn() {
        GameScene scene = scene();
        SceneHarness.tap(keys, KeyEvent.VK_UP);
        SceneHarness.advanceOneCell(scene);
        BufferedImage running = SceneHarness.render(scene);
        assertTrue(SceneHarness.countPixels(running, Constants.COLOR_SNAKE) > 0);

        SceneHarness.tap(keys, KeyEvent.VK_P);
        SceneHarness.advance(scene, 1);
        BufferedImage paused = SceneHarness.render(scene);

        assertEquals(GameScene.Phase.PAUSED, scene.phase());
        assertEquals(0, SceneHarness.countPixels(paused, Constants.COLOR_SNAKE), "the overlay hides the board");
        assertTrue(SceneHarness.countPixels(paused, Constants.COLOR_TEXT) > 100);
    }

    @Test
    @DisplayName("the ready screen leaves the board clear but keeps the snake visible")
    void readyScreenShowsTheSnake() {
        GameScene scene = scene();
        SceneHarness.advance(scene, 30);

        BufferedImage image = SceneHarness.render(scene);

        assertEquals(GameScene.Phase.READY, scene.phase());
        assertTrue(SceneHarness.countPixels(image, Constants.COLOR_SNAKE) > 0);
        assertTrue(SceneHarness.countPixels(image, Constants.COLOR_FOOD) > 0);
    }

    @Test
    @DisplayName("the score in the header follows the run")
    void headerFollowsTheScore() {
        GameScene scene = scene();
        BufferedImage scoreZero = SceneHarness.render(scene);
        assertTrue(SceneHarness.countPixels(scoreZero, Constants.COLOR_TEXT_DIM) > 0, "the header is drawn");

        SceneHarness.driveUntilEaten(scene, keys, 400);
        BufferedImage afterEating = SceneHarness.render(scene);

        assertEquals(Constants.SCORE_PER_FOOD, scene.score());
        BufferedImage headerBefore = SceneHarness.crop(scoreZero, 0, 0, Constants.WINDOW_WIDTH, Constants.HUD_HEIGHT);
        BufferedImage headerAfter = SceneHarness.crop(afterEating, 0, 0, Constants.WINDOW_WIDTH, Constants.HUD_HEIGHT);
        assertFalse(SceneHarness.pixelsEqual(headerBefore, headerAfter), "the header must show the new score");
    }

    @Test
    @DisplayName("nothing is drawn outside the window")
    void fitsTheWindow(@TempDir Path dir) {
        BufferedImage image = SceneHarness.render(scene(new HighScore(dir.resolve("highscore"), 0)));
        assertEquals(Constants.WINDOW_WIDTH, image.getWidth());
        assertEquals(Constants.WINDOW_HEIGHT, image.getHeight());
    }
}
