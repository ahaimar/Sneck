package org.pack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuSceneTest {

    /** The play button is drawn at (305, 280) and is 190 by 86; the difficulty pill is centred above. */
    private static final int MENU_MID_X = 400;
    private static final int PLAY_TOP = 280;
    private static final int QUIT_Y = 423;
    private static final int DIFFICULTY_Y = 226;

    private final SceneHarness.FakeHost host = new SceneHarness.FakeHost();
    private final KL keys = new KL();
    private final ML mouse = new ML();

    private MenuScene scene() {
        return scene(new HighScore(Path.of(System.getProperty("java.io.tmpdir"), "unused-highscore"), 0));
    }

    private MenuScene scene(HighScore highScore) {
        return SceneHarness.newMenuScene(host, keys, mouse, highScore);
    }

    private static MouseEvent at(int id, int x, int y, int clickCount) {
        return new MouseEvent(new javax.swing.JPanel(), id, System.currentTimeMillis(), 0,
                x, y, clickCount, false, MouseEvent.BUTTON1);
    }

    private static void moveMouseTo(ML mouse, int x, int y) {
        mouse.mouseMoved(at(MouseEvent.MOUSE_MOVED, x, y, 0));
    }

    private static void click(ML mouse, int x, int y) {
        moveMouseTo(mouse, x, y);
        mouse.mousePressed(at(MouseEvent.MOUSE_PRESSED, x, y, 1));
        mouse.mouseReleased(at(MouseEvent.MOUSE_RELEASED, x, y, 1));
        mouse.mouseClicked(at(MouseEvent.MOUSE_CLICKED, x, y, 1));
    }

    @Test
    void enterStartsTheGame() {
        MenuScene scene = scene();

        SceneHarness.tap(keys, KeyEvent.VK_ENTER);
        scene.update(SceneHarness.STEP);

        assertEquals(GameState.GAME, host.state);
        assertEquals(1, host.stateChanges);
    }

    @Test
    void escapeQuits() {
        MenuScene scene = scene();

        SceneHarness.tap(keys, KeyEvent.VK_ESCAPE);
        scene.update(SceneHarness.STEP);

        assertTrue(host.closed);
        assertEquals(0, host.stateChanges, "quitting is not a state change");
    }

    @Test
    @DisplayName("clicking play starts the game and clicking quit closes it")
    void clickingTheButtons() {
        click(mouse, MENU_MID_X, QUIT_Y);
        scene().update(SceneHarness.STEP);
        assertTrue(host.closed, "the lower button quits");

        SceneHarness.FakeHost second = new SceneHarness.FakeHost();
        ML secondMouse = new ML();
        MenuScene scene = SceneHarness.newMenuScene(second, keys, secondMouse, new HighScore(Path.of("unused"), 0));
        click(secondMouse, MENU_MID_X, PLAY_TOP + 10);
        scene.update(SceneHarness.STEP);
        assertEquals(GameState.GAME, second.state, "the upper button plays");
    }

    @Test
    @DisplayName("the clickable area matches the drawn button, to the pixel")
    void hitAreaMatchesTheButton() {
        click(mouse, MENU_MID_X, PLAY_TOP - 1);
        scene().update(SceneHarness.STEP);
        assertEquals(GameState.MENU, host.state, "one pixel above the button is not a click");
        assertFalse(host.closed);

        click(mouse, MENU_MID_X, PLAY_TOP);
        scene().update(SceneHarness.STEP);
        assertEquals(GameState.GAME, host.state, "the top edge of the button is a click");
    }

    @Test
    @DisplayName("one click is acted on once, not on every later frame")
    void aClickIsConsumedOnce() {
        MenuScene scene = scene();
        click(mouse, MENU_MID_X, PLAY_TOP + 10);
        scene.update(SceneHarness.STEP);
        assertEquals(GameState.GAME, host.state);

        host.state = GameState.MENU;
        scene.update(SceneHarness.STEP);
        scene.update(SceneHarness.STEP);

        assertEquals(GameState.MENU, host.state, "the click must not fire again on later frames");
    }

    @Test
    void clickingTheBackgroundDoesNothing() {
        MenuScene scene = scene();

        click(mouse, 40, 40);
        scene.update(SceneHarness.STEP);
        scene.update(SceneHarness.STEP);

        assertEquals(GameState.MENU, host.state);
        assertFalse(host.closed);
    }

    @Test
    @DisplayName("the arrow keys walk through the difficulty options in both directions")
    void arrowsChangeDifficulty() {
        MenuScene scene = scene();
        assertEquals(Difficulty.NORMAL, host.difficulty());

        SceneHarness.tap(keys, KeyEvent.VK_RIGHT);
        scene.update(SceneHarness.STEP);
        assertEquals(Difficulty.HARD, host.difficulty());

        SceneHarness.tap(keys, KeyEvent.VK_RIGHT);
        scene.update(SceneHarness.STEP);
        assertEquals(Difficulty.INSANE, host.difficulty());

        SceneHarness.tap(keys, KeyEvent.VK_LEFT);
        scene.update(SceneHarness.STEP);
        assertEquals(Difficulty.HARD, host.difficulty(), "left steps back the other way");

        SceneHarness.tap(keys, KeyEvent.VK_UP);
        scene.update(SceneHarness.STEP);
        assertEquals(Difficulty.NORMAL, host.difficulty());
    }

    @Test
    void wasdAlsoChangesDifficulty() {
        MenuScene scene = scene();

        SceneHarness.tap(keys, KeyEvent.VK_S);
        scene.update(SceneHarness.STEP);
        assertEquals(Difficulty.HARD, host.difficulty());

        SceneHarness.tap(keys, KeyEvent.VK_A);
        scene.update(SceneHarness.STEP);
        assertEquals(Difficulty.NORMAL, host.difficulty());
    }

    @Test
    @DisplayName("clicking the difficulty pill steps forward through the options")
    void clickingTheDifficultyPillCycles() {
        MenuScene scene = scene();

        click(mouse, MENU_MID_X, DIFFICULTY_Y);
        scene.update(SceneHarness.STEP);
        assertEquals(Difficulty.HARD, host.difficulty());
        assertEquals(GameState.MENU, host.state, "changing the difficulty must not start the game");
        assertFalse(host.closed);
    }

    @Test
    void oneDifficultyKeyPressChangesOneStep() {
        MenuScene scene = scene();

        SceneHarness.tap(keys, KeyEvent.VK_RIGHT);
        scene.update(SceneHarness.STEP);
        scene.update(SceneHarness.STEP);
        scene.update(SceneHarness.STEP);

        assertEquals(Difficulty.HARD, host.difficulty(), "a single press must not be applied twice");
    }

    @Test
    @DisplayName("the selected difficulty is what the next run uses")
    void theChoiceReachesTheGame() {
        host.setDifficulty(Difficulty.INSANE);

        GameScene game = SceneHarness.newGameScene(host, keys,
                new HighScore(Path.of("unused-highscore"), 0), new SceneHarness.SeededRandom());

        assertEquals(Difficulty.INSANE, game.difficulty());
        assertEquals(Difficulty.INSANE.startLength(), game.length());
        assertEquals(Difficulty.INSANE.startMoveSeconds(), game.moveInterval(), 1e-9);
    }

    @Test
    void renders(@TempDir Path dir) {
        BufferedImage image = SceneHarness.render(scene(new HighScore(dir.resolve("highscore"), 400)));

        assertEquals(Constants.WINDOW_WIDTH, image.getWidth());
        assertEquals(Constants.WINDOW_HEIGHT, image.getHeight());
        assertTrue(distinctColours(image) > 50, "the menu should not be a flat fill");
    }

    @Test
    @DisplayName("hovering a button repaints it, so the pressed state is visible")
    void hoverChangesTheFrame() {
        MenuScene scene = scene();

        moveMouseTo(mouse, 20, 20);
        scene.update(0);
        BufferedImage idle = SceneHarness.render(scene);

        moveMouseTo(mouse, MENU_MID_X, PLAY_TOP + 10);
        scene.update(0);
        BufferedImage hovered = SceneHarness.render(scene);

        assertFalse(pixelsEqual(idle, hovered), "hovering must change what is drawn");
    }

    private static int distinctColours(BufferedImage image) {
        Set<Integer> seen = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y += 3) {
            for (int x = 0; x < image.getWidth(); x += 3) {
                seen.add(image.getRGB(x, y));
            }
        }
        return seen.size();
    }

    private static boolean pixelsEqual(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }
}
