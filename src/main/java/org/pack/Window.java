package org.pack;

import javax.swing.*;
import java.awt.*;

public class Window extends JFrame implements Runnable {

    private volatile static Window window = null;

    private volatile boolean isRunning = false;
    private int currentState;

    private Scene currentScene;
    private KL keyListener = new KL();
    private ML mouseListener = new ML();

    private Image dbImage;
    private Graphics dbg;

    private Window(int width, int height, String title) {
        setSize(width, height);
        setTitle(title);
        setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addKeyListener(keyListener);
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);

        isRunning = true;
        changeState(0);
    }

    public static Window getWindow() {
        if (window == null) {
            synchronized (Window.class) {
                if (window == null) {
                    window = new Window(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT, Constants.WINDOW_TITLE);
                }
            }
        }
        return window;
    }

    public void changeState(int newState) {
        System.out.println("Changing state to: " + newState);
        currentState = newState;
        switch (newState) {
            case 0:
                currentScene = new MenuScene(keyListener, mouseListener);
                break;
            case 1:
                currentScene = new GameScene(keyListener);
                break;
            default:
                System.out.println("Unknown Scene");
                currentScene = null;
                break;
        }
    }

    public void update(double deltaTime) {
        if (currentScene != null) {
            currentScene.update(deltaTime);
        }

        // Double buffering setup
        if (dbImage == null) {
            dbImage = createImage(getWidth(), getHeight());
            dbg = dbImage.getGraphics();
        }

        draw(dbg);
        getGraphics().drawImage(dbImage, 0, 0, this);
    }

    public void draw(Graphics g) {
        if (currentScene != null) {
            Graphics2D g2 = (Graphics2D) g;
            currentScene.draw(g2);
        }
    }

    @Override
    public void run() {
        double lastFrameTime = Time.getTime();
        try {
            while (isRunning) {
                double time = Time.getTime();
                double deltaTime = time - lastFrameTime;
                lastFrameTime = time;

                update(deltaTime);

                double frameDuration = (Time.getTime() - time) * 1000; // Convert to milliseconds
                long sleepTime = Math.max(0, 16 - (long) frameDuration);

                // Sleep to maintain consistent frame rate
                Thread.sleep(sleepTime);  // Adjust sleep for consistent 60 FPS
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        isRunning = false;
        dispose();  // Close window on exit
    }
}
