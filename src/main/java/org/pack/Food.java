package org.pack;

import java.awt.*;

public class Food {

    public SnaKe snaKe;
    public Rect background;
    public Rect rect;
    public Color color;
    public int width;
    public int height;

    public boolean isSpawned;

    public int xPadding;

    public Food(Rect rect,SnaKe snaKe, int width, int height, Color color) {

        this.snaKe = snaKe;
        this.width = width;
        this.height = height;
        this.color = color;
        this.background = rect;
        this.rect = new Rect(0, 0, width, height);

        xPadding = (int) ((Constants.TILE_WIDTH - this.width) / 2.0);
    }

    public void spawn() {
        do {
            double randX = (int)(Math.random() * (int) (background.width / Constants.TILE_WIDTH)) * Constants.TILE_WIDTH + background.x;
            double randY = (int)(Math.random() * (int) (background.height / Constants.TILE_WIDTH)) * Constants.TILE_WIDTH + background.y;
            this.rect.x = randX;
            this.rect.y = randY;

        }while (snaKe.intersectingWithSelfRect(this.rect));
        this.isSpawned = true;
    }

    public void update(double deltaTime) {

        if (snaKe.intersectingWithSelfRect(this.rect)) {

            snaKe.grow();
            this.rect.x = - 100;
            this.rect.y = - 100;
            this.isSpawned = false;
        }
    }

    public void draw(Graphics graphics) {
        graphics.setColor(color);
        graphics.fillRect((int)this.rect.x + xPadding, (int)this.rect.y + xPadding, width, height);
    }
}