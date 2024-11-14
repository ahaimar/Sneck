package org.pack;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;

public class GameScene extends Scene {

    Rect background, foreground;
    SnaKe snaKe;
    KL keyListener;

    public Food food;

    public GameScene(KL keyListener) {

        background = new Rect(0, 0, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        foreground = new Rect(24,  48, Constants.TILE_WIDTH * 31, Constants.TILE_WIDTH * 22);
        snaKe = new SnaKe(3, 48, 48 +24, 24, 24, foreground);
        this.keyListener = keyListener;

        food = new Food(foreground, snaKe, 12, 12, Color.GREEN);
        food.spawn();
    }

    @Override
    public void update(double deltaTime) {

        if (keyListener.isKeyPressed(KeyEvent.VK_UP)) snaKe.changeDirection(Direction.UP);
        else if (keyListener.isKeyPressed(KeyEvent.VK_DOWN)) snaKe.changeDirection(Direction.DOWN);
        else if (keyListener.isKeyPressed(KeyEvent.VK_RIGHT)) snaKe.changeDirection(Direction.RIGHT);
        else if (keyListener.isKeyPressed(KeyEvent.VK_LEFT)) snaKe.changeDirection(Direction.LEFT);

        if (!food.isSpawned) food.spawn();

        food.update(deltaTime);
        snaKe.update(deltaTime);

    }

    @Override
    public void draw(Graphics graphics) {

        Graphics2D g2 = (Graphics2D) graphics;
        g2.setColor(Color.BLACK);
        g2.fill(new Rectangle2D.Double(background.x, background.y, background.width, background.height));

        g2.setColor(Color.WHITE);
        g2.fill(new Rectangle2D.Double(foreground.x, foreground.y, foreground.width, foreground.height));

        snaKe.draw(g2);
        food.draw(g2);
    }
}