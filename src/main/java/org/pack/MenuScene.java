package org.pack;

import javax.imageio.ImageIO;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

public class MenuScene extends Scene {

    public Rect playRect, exitRect, titleRect;
    public KL keyListener;
    public ML mouseListener;

    public BufferedImage title, play, playPressed, exitPressed, exit;
    public BufferedImage exitCurrentImage, playCurrentImage;

    public MenuScene(KL keyListener, ML mouseListener) {
        this.keyListener = keyListener;
        this.mouseListener = mouseListener;
        try {
            BufferedImage spritesSheet = ImageIO.read(new File("assets/menuSprite.png"));
            title = spritesSheet.getSubimage(0, 242, 960, 240);
            play = spritesSheet.getSubimage(0, 121, 261, 121);
            playPressed = spritesSheet.getSubimage(264, 121, 261, 121);
            exit = spritesSheet.getSubimage(0, 0, 233, 93);
            exitPressed = spritesSheet.getSubimage(264, 0, 233, 93);
        } catch (Exception e) {
            e.printStackTrace();
        }

        playCurrentImage = play;
        exitCurrentImage = exit;

        titleRect = new Rect(240, 100, 300, 100);
        playRect = new Rect(310, 280, 150, 70);
        exitRect = new Rect(318, 355, 130, 55);
    }

    @Override
    public void update(double deltaTime) {
        // Handle Play button
        if (isMouseOver(playRect)) {
            playCurrentImage = playPressed;
            if (mouseListener.isPressed()) {
                Window.getWindow().changeState(1); // Start game
            }
        } else {
            playCurrentImage = play;
        }

        // Handle Exit button
        if (isMouseOver(exitRect)) {
            exitCurrentImage = exitPressed;
            if (mouseListener.isPressed()) {
                Window.getWindow().close(); // Close game
            }
        } else {
            exitCurrentImage = exit;
        }
    }

    private boolean isMouseOver(Rect rect) {
        return mouseListener.getX() >= rect.x && mouseListener.getX() <= rect.x + rect.width
                && mouseListener.getY() >= rect.y && mouseListener.getY() <= rect.y + rect.height;
    }

    @Override
    public void draw(Graphics graphics) {
        graphics.setColor(new Color(0xFF131742));
        graphics.fillRect(0, 0, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

        graphics.drawImage(title, (int) titleRect.x, (int) titleRect.y, (int) titleRect.width, (int) titleRect.height, null);
        graphics.drawImage(playCurrentImage, (int) playRect.x, (int) playRect.y, (int) playRect.width, (int) playRect.height, null);
        graphics.drawImage(exitCurrentImage, (int) exitRect.x, (int) exitRect.y, (int) exitRect.width, (int) exitRect.height, null);
    }
}
