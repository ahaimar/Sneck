package org.pack;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * The game shell: owns the window, the active scene, and the update/render loop.
 *
 * <p>Simulation and rendering both run on a dedicated game thread so neither blocks Swing's
 * event thread, and only the finished image is handed to the event thread to blit. Input
 * arrives on the event thread and is read by the game thread, so the listeners share state
 * atomically.
 */
public class Window extends JFrame implements GameHost {

    private static Window instance;

    private final KL keyListener = new KL();
    private final ML mouseListener = new ML();
    private final HighScore highScore;
    private final Settings settings;
    private final Sound sound;
    private final Random random = new Random();

    private final Surface surface;
    private final AtomicReference<BufferedImage> pendingFrame = new AtomicReference<>();

    private volatile boolean running;
    private volatile GameState state = GameState.MENU;
    private volatile Difficulty difficulty;
    private Scene currentScene;

    private Window() {
        this.highScore = HighScore.loadDefault();
        this.settings = Settings.loadDefault();
        this.sound = Sound.openFor(settings);
        this.difficulty = settings.difficulty();
        this.surface = new Surface();

        setTitle(Constants.WINDOW_TITLE);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(surface);
        pack();
        setLocationRelativeTo(null);
    }

    public static void launch() {
        if (instance != null) {
            throw new IllegalStateException("window already created");
        }
        try {
            // Swing components are only ever touched from the event thread.
            SwingUtilities.invokeAndWait(() -> {
                instance = new Window();
                instance.setVisible(true);
                instance.surface.requestFocusInWindow();
            });
        } catch (Exception e) {
            throw new IllegalStateException("could not open the game window", e);
        }
        Thread loader = new Thread(MenuScene::preload, "sprite-loader");
        loader.setDaemon(true);
        loader.start();
        instance.changeState(GameState.MENU);
        instance.start();
    }

    private void start() {
        running = true;
        Thread loop = new Thread(this::run, "game-loop");
        loop.setDaemon(true);
        loop.start();
    }

    @Override
    public void changeState(GameState newState) {
        if (newState == state && currentScene != null) {
            return;
        }
        if (currentScene != null) {
            currentScene.onExit();
        }
        // Stale keys and clicks from the old scene must not leak into the new one.
        keyListener.flush();
        mouseListener.flush();

        state = newState;
        currentScene = switch (newState) {
            case MENU -> new MenuScene(this, keyListener, mouseListener, highScore, sound);
            case GAME -> new GameScene(this, keyListener, highScore, sound, random);
        };
        setStateTitle(0);
        currentScene.onEnter();
    }

    @Override
    public void setStateTitle(int score) {
        String title = state == GameState.GAME && score > 0
                ? Constants.WINDOW_TITLE + "  -  score " + score
                : Constants.WINDOW_TITLE;
        SwingUtilities.invokeLater(() -> setTitle(title));
    }

    @Override
    public Difficulty difficulty() {
        return difficulty;
    }

    @Override
    public void setDifficulty(Difficulty difficulty) {
        if (difficulty != null) {
            this.difficulty = difficulty;
            settings.setDifficulty(difficulty);
        }
    }

    @Override
    public void close() {
        running = false;
        sound.shutdown();
        SwingUtilities.invokeLater(this::dispose);
    }

    /**
     * Fixed timestep: simulation advances in constant slices and rendering happens once per
     * real frame, so the game plays identically on any refresh rate and a slow frame can never
     * make the snake teleport.
     */
    private void run() {
        double previous = Time.getTime();
        double accumulator = 0;

        while (running) {
            double now = Time.getTime();
            accumulator += Math.min(now - previous, Constants.MAX_FRAME_SECONDS);
            previous = now;

            while (accumulator >= Constants.FIXED_STEP_SECONDS) {
                if (currentScene != null) {
                    currentScene.update(Constants.FIXED_STEP_SECONDS);
                }
                accumulator -= Constants.FIXED_STEP_SECONDS;
            }

            render();

            double remaining = Constants.FIXED_STEP_SECONDS - (Time.getTime() - now);
            if (remaining > 0) {
                try {
                    Thread.sleep((long) (remaining * 1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void render() {
        BufferedImage frame = pendingFrame.get();
        if (frame == null || frame.getWidth() != Constants.WINDOW_WIDTH
                || frame.getHeight() != Constants.WINDOW_HEIGHT) {
            frame = new BufferedImage(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT, BufferedImage.TYPE_INT_RGB);
        }
        Graphics2D g2 = frame.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Constants.COLOR_BACKGROUND);
            g2.fillRect(0, 0, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
            if (currentScene != null) {
                currentScene.draw(g2);
            }
        } finally {
            g2.dispose();
        }
        pendingFrame.set(frame);
        surface.repaint();
    }

    /** Blits the most recently rendered frame onto the screen. Runs on the event thread. */
    private final class Surface extends JPanel {

        Surface() {
            setPreferredSize(new Dimension(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT));
            setBackground(Color.BLACK);
            setFocusable(true);
            setFocusTraversalKeysEnabled(false);
            addKeyListener(keyListener);
            addMouseListener(mouseListener);
            addMouseMotionListener(mouseListener);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            BufferedImage frame = pendingFrame.get();
            if (frame != null) {
                graphics.drawImage(frame, 0, 0, null);
            }
        }
    }
}
