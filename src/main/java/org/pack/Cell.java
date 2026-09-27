package org.pack;

/**
 * Integer grid coordinate. Using whole cells instead of floating point pixels keeps the
 * simulation free of drift and makes every rule (collision, wrapping, food placement)
 * exact and cheap to test.
 */
public record Cell(int x, int y) {

    public Cell translate(Direction direction) {
        return translate(direction.dx(), direction.dy());
    }

    public Cell translate(int dx, int dy) {
        return new Cell(x + dx, y + dy);
    }

    public double centerX(int tileSize) {
        return x * tileSize + tileSize / 2.0;
    }

    public double centerY(int tileSize) {
        return y * tileSize + tileSize / 2.0;
    }
}
