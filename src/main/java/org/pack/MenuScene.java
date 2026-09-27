package org.pack;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Title screen. Buttons are drawn from the sprite sheet when it is available and fall back to
 * plain text otherwise, so a missing asset degrades the look instead of crashing the game.
 */
public class MenuScene extends Scene {

    private static final String SPRITE_RESOURCE = "/assets/menuSprite.png";
    private static final String SPRITE_FILE = "assets/menuSprite.png";

    /** Horizontal distance from a button's normal sprite to its hovered sprite. */
    private static final int HOVER_OFFSET_X = 264;

    private static final Rect TITLE_RECT = new Rect(240, 78, 320, 80);
    private static final Rect DIFFICULTY_RECT = new Rect(275, 200, 250, 52);
    private static final Button PLAY =
            new Button(new Rect(305, 280, 190, 86), 0, 121, 261, 121, "PLAY");
    private static final Button QUIT =
            new Button(new Rect(305, 380, 190, 86), 0, 0, 233, 93, "QUIT");
    private static final List<Button> BUTTONS = List.of(PLAY, QUIT);

    private static final Color COLOR_TOP = new Color(0x1B1F4B);
    private static final Color COLOR_BOTTOM = new Color(0x0B1020);
    private static final Color COLOR_BANNER = new Color(0x38, 0x44, 0x8C, 28);
    private static final Color COLOR_BUTTON = new Color(0x1E2745);
    private static final Color COLOR_BUTTON_HOVER = new Color(0x4ADE80);
    private static final Color COLOR_BUTTON_HOVER_EDGE = new Color(0x86EFAC);

    private static BufferedImage sprites;

    private final GameHost host;
    private final KL keys;
    private final ML mouse;
    private final HighScore highScore;
    private final Sound sound;

    private Button hovered;
    private boolean difficultyHovered;
    private double clock;

    public MenuScene(GameHost host, KL keys, ML mouse, HighScore highScore, Sound sound) {
        this.host = host;
        this.keys = keys;
        this.mouse = mouse;
        this.highScore = highScore;
        this.sound = sound;
    }

    /** Reads the sprite sheet once, off the critical path, so opening the menu never stutters. */
    public static void preload() {
        loadSprites();
    }

    private static synchronized BufferedImage loadSprites() {
        if (sprites != null) {
            return sprites;
        }
        try (InputStream in = MenuScene.class.getResourceAsStream(SPRITE_RESOURCE)) {
            sprites = in != null ? ImageIO.read(in) : ImageIO.read(new File(SPRITE_FILE));
        } catch (Exception e) {
            sprites = null;
        }
        return sprites;
    }

    private static BufferedImage region(int x, int y, int width, int height) {
        BufferedImage sheet = loadSprites();
        if (sheet == null || x + width > sheet.getWidth() || y + height > sheet.getHeight()) {
            return null;
        }
        return sheet.getSubimage(x, y, width, height);
    }

    @Override
    public void update(double deltaTime) {
        clock += deltaTime;

        if (keys.consumePress(KeyEvent.VK_M)) {
            sound.toggleMuted();
            if (!sound.isMuted()) {
                sound.play(Sound.Effect.MENU_CLICK);
            }
        }
        if (readDifficultyKey()) {
            return;
        }

        if (keys.consumePress(KeyEvent.VK_ENTER) || keys.consumePress(KeyEvent.VK_SPACE)) {
            start();
            return;
        }
        if (keys.consumePress(KeyEvent.VK_ESCAPE) || keys.consumePress(KeyEvent.VK_Q)) {
            host.close();
            return;
        }

        hovered = buttonAt(mouse.getX(), mouse.getY());
        difficultyHovered = DIFFICULTY_RECT.contains(mouse.getX(), mouse.getY());
        if (mouse.consumePress()) {
            if (difficultyHovered) {
                cycleDifficulty();
            } else if (hovered == PLAY) {
                start();
            } else if (hovered == QUIT) {
                host.close();
            }
        }
    }

    /** @return true when a difficulty key changed the selection, so the rest of the frame is skipped. */
    private boolean readDifficultyKey() {
        Difficulty current = host.difficulty();
        Difficulty chosen = null;
        if (keys.consumePress(KeyEvent.VK_RIGHT) || keys.consumePress(KeyEvent.VK_D)) {
            chosen = current.next();
        } else if (keys.consumePress(KeyEvent.VK_LEFT) || keys.consumePress(KeyEvent.VK_A)) {
            chosen = current.previous();
        } else if (keys.consumePress(KeyEvent.VK_DOWN) || keys.consumePress(KeyEvent.VK_S)) {
            chosen = current.next();
        } else if (keys.consumePress(KeyEvent.VK_UP) || keys.consumePress(KeyEvent.VK_W)) {
            chosen = current.previous();
        }
        if (chosen == null) {
            return false;
        }
        host.setDifficulty(chosen);
        sound.play(Sound.Effect.MENU_CLICK);
        return true;
    }

    private void cycleDifficulty() {
        host.setDifficulty(host.difficulty().next());
        sound.play(Sound.Effect.MENU_CLICK);
    }

    private void start() {
        sound.play(Sound.Effect.START);
        host.changeState(GameState.GAME);
    }

    private static Button buttonAt(double x, double y) {
        for (Button button : BUTTONS) {
            if (button.rect().contains(x, y)) {
                return button;
            }
        }
        return null;
    }

    @Override
    public void draw(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            drawBackground(g2);
            drawTitle(g2);
            drawDifficulty(g2);
            for (Button button : BUTTONS) {
                drawButton(g2, button);
            }
            drawFooter(g2);
        } finally {
            g2.dispose();
        }
    }

    private void drawBackground(Graphics2D g2) {
        g2.setPaint(new GradientPaint(0, 0, COLOR_TOP, 0, Constants.WINDOW_HEIGHT, COLOR_BOTTOM));
        g2.fillRect(0, 0, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

        g2.setColor(COLOR_BANNER);
        for (int i = 0; i < 3; i++) {
            double offset = (clock * 18 * (i + 1)) % (Constants.WINDOW_WIDTH + 160) - 80;
            g2.fill(new RoundRectangle2D.Double(offset, -40 + i * 120, 80, 700, 80, 80));
        }
    }

    private void drawTitle(Graphics2D g2) {
        BufferedImage image = region(0, 242, 960, 240);
        if (image != null) {
            g2.drawImage(image, (int) TITLE_RECT.x, (int) TITLE_RECT.y,
                    (int) TITLE_RECT.width, (int) TITLE_RECT.height, null);
            return;
        }
        g2.setFont(new Font(Constants.FONT_FAMILY, Font.BOLD, 76));
        g2.setColor(Constants.COLOR_TEXT);
        drawCentered(g2, "SNAKE", (int) TITLE_RECT.centerY() + 26);
    }

    private void drawDifficulty(Graphics2D g2) {
        Difficulty difficulty = host.difficulty();
        Color accent = accentFor(difficulty);

        g2.setFont(new Font(Constants.FONT_FAMILY, Font.PLAIN, 13));
        g2.setColor(Constants.COLOR_TEXT_DIM);
        drawCentered(g2, "DIFFICULTY", 190);

        RoundRectangle2D shape = new RoundRectangle2D.Double(
                DIFFICULTY_RECT.x, DIFFICULTY_RECT.y,
                DIFFICULTY_RECT.width, DIFFICULTY_RECT.height, 26, 26);
        g2.setColor(difficultyHovered ? accent : COLOR_BUTTON);
        g2.fill(shape);
        g2.setColor(accent);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(shape);

        g2.setFont(new Font(Constants.FONT_FAMILY, Font.BOLD, 22));
        g2.setColor(difficultyHovered ? Constants.COLOR_BACKGROUND : accent);
        drawCentered(g2, "<  " + difficulty.label() + "  >",
                (int) DIFFICULTY_RECT.centerY() + g2.getFontMetrics().getAscent() / 2 - 2);
    }

    private static Color accentFor(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new Color(0x4ADE80);
            case NORMAL -> new Color(0x60A5FA);
            case HARD -> new Color(0xFBBF24);
            case INSANE -> new Color(0xF87171);
        };
    }

    private void drawButton(Graphics2D g2, Button button) {
        Rect rect = button.rect();
        boolean hover = button == hovered;

        BufferedImage image = region(button.spriteX() + (hover ? HOVER_OFFSET_X : 0),
                button.spriteY(), button.spriteWidth(), button.spriteHeight());
        if (image != null) {
            g2.drawImage(image, (int) rect.x, (int) rect.y, (int) rect.width, (int) rect.height, null);
            return;
        }

        RoundRectangle2D shape = new RoundRectangle2D.Double(
                rect.x, rect.y, rect.width, rect.height, 24, 24);
        g2.setColor(hover ? COLOR_BUTTON_HOVER : COLOR_BUTTON);
        g2.fill(shape);
        g2.setColor(hover ? COLOR_BUTTON_HOVER_EDGE : Constants.COLOR_BOARD_BORDER);
        g2.setStroke(new BasicStroke(3f));
        g2.draw(shape);

        g2.setFont(new Font(Constants.FONT_FAMILY, Font.BOLD, 30));
        g2.setColor(hover ? Constants.COLOR_BACKGROUND : Constants.COLOR_TEXT);
        int textWidth = g2.getFontMetrics().stringWidth(button.label());
        int baseline = (int) rect.centerY() + g2.getFontMetrics().getAscent() / 2 - 2;
        g2.drawString(button.label(), (int) rect.centerX() - textWidth / 2, baseline);
    }

    private void drawFooter(Graphics2D g2) {
        g2.setFont(new Font(Constants.FONT_FAMILY, Font.PLAIN, 15));
        g2.setColor(Constants.COLOR_TEXT_DIM);
        drawCentered(g2, "BEST " + highScore.value(), 500);

        g2.setFont(new Font(Constants.FONT_FAMILY, Font.PLAIN, 13));
        drawCentered(g2, "arrows change difficulty  -  M sound " + (sound.isMuted() ? "off" : "on"), 524);
        drawCentered(g2, "ENTER to play  -  ESC to quit", 546);
    }

    private void drawCentered(Graphics2D g2, String text, int baseline) {
        int width = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (Constants.WINDOW_WIDTH - width) / 2, baseline);
    }

    /** A clickable region plus the sprite-sheet cell and fallback label used to draw it. */
    private record Button(Rect rect, int spriteX, int spriteY, int spriteWidth, int spriteHeight,
                          String label) {
    }
}
