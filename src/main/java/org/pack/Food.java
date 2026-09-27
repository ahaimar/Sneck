package org.pack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A single piece of food, always parked on a cell that the snake does not occupy.
 *
 * <p>Free cells are enumerated instead of rejection-sampling, so placement is uniformly random
 * and can never spin forever once the snake fills the board.
 */
public class Food {

    private final int columns;
    private final int rows;
    private final Random random;

    private Cell position;

    public Food(int columns, int rows, Random random) {
        this.columns = columns;
        this.rows = rows;
        this.random = random;
    }

    public Cell position() {
        return position;
    }

    public boolean isSpawned() {
        return position != null;
    }

    /** @return false when the snake covers the whole board and nothing can be placed. */
    public boolean spawn(SnaKe snake) {
        List<Cell> free = new ArrayList<>(columns * rows);
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                Cell cell = new Cell(x, y);
                if (!snake.occupies(cell)) {
                    free.add(cell);
                }
            }
        }
        if (free.isEmpty()) {
            position = null;
            return false;
        }
        position = free.get(random.nextInt(free.size()));
        return true;
    }

    /** @return true when the head is on the food, in which case the snake is told to grow. */
    public boolean tryEat(SnaKe snake) {
        if (position == null || !snake.head().equals(position)) {
            return false;
        }
        position = null;
        snake.grow();
        return true;
    }
}
