package org.pack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodTest {

    private static final Direction[] DIRECTIONS = Direction.values();

    @Test
    @DisplayName("food lands on a real cell that the snake is not on")
    void spawnsOnAFreeCell() {
        SnaKe snake = new SnaKe(10, 10, 5, 5, 4);
        Food food = new Food(10, 10, new Random(1));

        assertTrue(food.spawn(snake));
        assertTrue(food.isSpawned());
        assertNotNull(food.position());
        assertTrue(snake.isOnBoard(food.position()));
        assertFalse(snake.occupies(food.position()));
    }

    @Test
    @DisplayName("every free cell is reachable, so a spawn never has to retry or loop forever")
    void coversEveryCell() {
        SnaKe snake = new SnaKe(4, 4, 2, 2, 1);
        Food food = new Food(4, 4, new Random(7));
        Set<Cell> seen = new HashSet<>();

        for (int i = 0; i < 400; i++) {
            assertTrue(food.spawn(snake));
            seen.add(food.position());
        }

        assertEquals(15, seen.size(), "every cell except the one the snake occupies");
    }

    @Test
    @DisplayName("spawning on a full board reports failure instead of hanging")
    void refusesToSpawnOnAFullBoard() {
        SnaKe snake = new SnaKe(3, 1, 2, 0, 3);
        Food food = new Food(3, 1, new Random(3));
        assertEquals(3, snake.length());

        assertFalse(food.spawn(snake));
        assertFalse(food.isSpawned());
        assertNull(food.position());
    }

    @Test
    @DisplayName("eating grows the snake by one and consumes the food")
    void tryEatGrowsTheSnake() {
        SnaKe snake = new SnaKe(6, 6, 0, 0, 1);
        Food food = new Food(6, 6, new Random(5));
        assertTrue(food.spawn(snake));

        walkTo(snake, food.position());
        assertEquals(food.position(), snake.head());

        assertTrue(food.tryEat(snake));
        assertEquals(1, snake.pendingGrowth());
        assertFalse(food.isSpawned());

        snake.step();
        assertEquals(2, snake.length());
    }

    @Test
    void tryEatIsFalseWhenTheHeadIsElsewhere() {
        SnaKe snake = new SnaKe(3, 1, 0, 0, 1);
        Food food = new Food(3, 1, new Random(0));
        food.spawn(snake);
        assertFalse(snake.head().equals(food.position()));

        assertFalse(food.tryEat(snake));
        assertEquals(0, snake.pendingGrowth());
        assertTrue(food.isSpawned());
    }

    @Test
    void tryEatOnAnUnspawnedFoodIsFalse() {
        SnaKe snake = new SnaKe(6, 6, 0, 0, 1);
        Food food = new Food(6, 6, new Random(2));

        assertFalse(food.isSpawned());
        assertFalse(food.tryEat(snake));
        assertEquals(0, snake.pendingGrowth());
    }

    @Test
    @DisplayName("spawning stays consistent over a long random walk")
    void respawnKeepsWorkingAcrossManyRounds() {
        SnaKe snake = new SnaKe(8, 8, 4, 4, 3);
        Food food = new Food(8, 8, new Random(11));
        Random random = new Random(11);

        for (int i = 0; i < 200; i++) {
            assertTrue(food.spawn(snake), "round " + i + " should have a free cell");
            assertFalse(snake.occupies(food.position()));

            food.tryEat(snake);
            snake.queueTurn(DIRECTIONS[random.nextInt(DIRECTIONS.length)]);
            if (snake.isDead()) {
                snake.reset(4, 4, 3);
            } else {
                snake.step();
            }
        }
    }

    /** A single-segment snake can reach any cell, so a greedy walk is enough here. */
    private static void walkTo(SnaKe snake, Cell target) {
        for (int guard = 0; guard < 200 && !snake.head().equals(target); guard++) {
            Direction wanted = towards(snake.head(), target);
            snake.queueTurn(wanted == snake.direction().opposite()
                    ? (snake.direction().isHorizontal() ? Direction.UP : Direction.LEFT)
                    : wanted);
            snake.step();
        }
    }

    private static Direction towards(Cell from, Cell to) {
        if (to.x() > from.x()) {
            return Direction.RIGHT;
        }
        if (to.x() < from.x()) {
            return Direction.LEFT;
        }
        if (to.y() > from.y()) {
            return Direction.DOWN;
        }
        return Direction.UP;
    }
}
