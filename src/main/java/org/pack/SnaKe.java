package org.pack;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class SnaKe {

    public Rect[] body = new Rect[100];

    public double bodyWidth, bodyHeight;
    public int size;
    public int tail = 0;
    public int head = 0;

    public Direction direction = Direction.RIGHT;
    public  Rect background;

    public double ogWaitBetweenUpdates = 0.1;
    public double waitTimeLeft = ogWaitBetweenUpdates;

    public SnaKe(int size, double startX, double startY, double bodyWidth, double bodyHeight, Rect background) {

        this.size = size;
        this.bodyWidth = bodyWidth;
        this.bodyHeight = bodyHeight;
        this.background = background;
        for (int i = 0; i <= size; i++) {
            body[i] = new Rect(startY + i * bodyWidth, startY, bodyWidth, bodyHeight);
            head++;
        }
        head--;
    }

    public void changeDirection(Direction newDirection) {
        if (newDirection == Direction.RIGHT && direction != Direction.LEFT) {
            direction = newDirection;
        } else if (newDirection == Direction.LEFT && direction != Direction.RIGHT) {
            direction = newDirection;
        } else if (newDirection == Direction.UP && direction != Direction.DOWN) {
            direction = newDirection;
        } else if (newDirection == Direction.DOWN && direction != Direction.UP) {
            direction = newDirection;
        }
    }

    public boolean intersectingWithSelf() {

        Rect headR = body[head];
        return intersectingWithSelfRect(headR) || intersectingWithScreenBoundaries(headR) ;
    }

    public boolean intersectingWithSelfRect(Rect rect) {

        for (int i = tail; i != head; i =(i + 1) % body.length) {
            if (intersecting(rect, body[i])) {
                return true;
            }
        }
        return  false;
    }

    public boolean intersectingWithScreenBoundaries(Rect rect) {

        return (rect.x < background.x ||(rect.x + rect.width) > background.x + background.width ||
                rect.y < background.y ||(rect.y + rect.height) > background.x + background.height);
    }

    public  boolean intersecting(Rect r1, Rect r2) {

        return (r1.x >= r2.x && r1.x + r1.width <= r2.x + r2.width
                && r1.y >= r2.y && r1.y + r1.height <= r2.y + r2.height);
    }

    public void update(double deltaTime) {

        if (waitTimeLeft > 0) {
            waitTimeLeft -= deltaTime;
            return;
        }

        if (intersectingWithSelf()) {
            Window.getWindow().changeState(0);
        }

        waitTimeLeft = ogWaitBetweenUpdates;
        double newX = 0;
        double newY = 0;

        // Determine new head position based on current direction
        if (direction == Direction.RIGHT) {
            newX = body[head].x + bodyWidth;
            newY = body[head].y;
        } else if (direction == Direction.LEFT) {
            newX = body[head].x - bodyWidth;
            newY = body[head].y;
        } else if (direction == Direction.UP) {
            newX = body[head].x ;
            newY = body[head].y - bodyWidth;
        } else if (direction == Direction.DOWN) {
            newX = body[head].x ;
            newY = body[head].y + bodyWidth;
        }

        // Move head to the new position
        body[(head + 1) % body.length] = body[tail];
        body[tail] = null;
        head = (head + 1) % body.length;
        tail = (tail + 1)% body.length;

        body[head].x = newX;
        body[head].y = newY;
    }

    public void grow() {

        double newX = 0;
        double newY = 0;

        // Determine new head position based on current direction
        if (direction == Direction.RIGHT) {
            newX = body[tail].x - bodyWidth;
            newY = body[tail].y;
        } else if (direction == Direction.LEFT) {
            newX = body[tail].x + bodyWidth;
            newY = body[tail].y;
        } else if (direction == Direction.UP) {
            newX = body[tail].x ;
            newY = body[tail].y + bodyWidth;
        } else if (direction == Direction.DOWN) {
            newX = body[tail].x ;
            newY = body[tail].y - bodyWidth;
        }

        Rect newBodyPiece = new Rect(newX, newY, bodyWidth, bodyHeight);
        tail = (tail - 1) % body.length;
        body[tail] = newBodyPiece;
    }

    public void draw(Graphics2D g2) {

        for (int i = tail; i != head; i =(i + 1) % body.length) {

            Rect piece = body[i];
            double subWidth= (piece.width - 6.0) / 2.0;
            double subHeight= (piece.height - 6.0) / 2.0;

            g2.setColor(Color.BLACK);
            g2.fill(new Rectangle2D.Double(piece.x + 2.0, piece.y + 2.0, subWidth, subHeight));
            g2.fill(new Rectangle2D.Double(piece.x + 4.0 + subWidth, piece.y + 2.0 , subWidth, subHeight));
            g2.fill(new Rectangle2D.Double(piece.x + 2.0 , piece.y + 4.0 + subHeight, subWidth, subHeight));
            g2.fill(new Rectangle2D.Double(piece.x + 4.0 + subWidth, piece.y + 4.0 + subHeight, subWidth, subHeight));
        }

    }
}






















