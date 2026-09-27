package org.pack;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JPanel;

/** Shared helpers for driving and rendering scenes without a window. */
final class SceneHarness {

    static final double STEP = Constants.FIXED_STEP_SECONDS;

    private SceneHarness() {
    }

    /** Records what a scene asked of the window, so scenes can be driven without a display. */
    static final class FakeHost implements GameHost {

        GameState state = GameState.MENU;
        Difficulty difficulty = Difficulty.NORMAL;
        int lastTitleScore = -1;
        int stateChanges;
        boolean closed;

        @Override
        public void changeState(GameState newState) {
            state = newState;
            stateChanges++;
        }

        @Override
        public void setStateTitle(int score) {
            lastTitleScore = score;
        }

        @Override
        public Difficulty difficulty() {
            return difficulty;
        }

        @Override
        public void setDifficulty(Difficulty difficulty) {
            this.difficulty = difficulty;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    /** A {@link Random} that always returns the lowest index, parking the food in one known corner. */
    static final class FirstCellRandom extends Random {
        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }

    /**
     * A fixed seed, so a run is reproducible without the food piling up on the snake's own path
     * the way it does when placement is constant.
     */
    static final class SeededRandom extends Random {
        static final long SEED = 20260927L;

        SeededRandom() {
            super(SEED);
        }
    }

    /** Length the snake will have once the growth queued by the last meal is applied. */
    static int lengthIncludingPendingGrowth(GameScene scene) {
        return scene.length() + scene.snake().pendingGrowth();
    }

    static void tap(KL keys, int keyCode) {
        keys.keyPressed(keyEvent(KeyEvent.KEY_PRESSED, keyCode));
        keys.keyReleased(keyEvent(KeyEvent.KEY_RELEASED, keyCode));
    }

    private static java.awt.event.KeyEvent keyEvent(int id, int keyCode) {
        return new KeyEvent(new JPanel(), id, System.currentTimeMillis(), 0, keyCode,
                KeyEvent.CHAR_UNDEFINED);
    }

    static void advance(Scene scene, int steps) {
        for (int i = 0; i < steps; i++) {
            scene.update(STEP);
        }
    }

    /**
     * Advances until the snake has covered exactly one more cell.
     *
     * @return false if the run ended first, which is how a crash surfaces
     */
    static boolean advanceOneCell(GameScene scene) {
        Cell head = scene.snake().head();
        for (int step = 0; step <= 1000; step++) {
            scene.update(STEP);
            if (scene.phase() != GameScene.Phase.RUNNING) {
                return false;
            }
            if (!scene.snake().head().equals(head)) {
                return true;
            }
        }
        throw new AssertionError("the snake never moved");
    }

    /**
     * Steers the snake one cell at a time along a route to the current food, until the food has
     * been eaten. Each cell is simulated individually, so the snake never overshoots the food.
     */
    static void driveUntilEaten(GameScene scene, KL keys, int maxCells) {
        SnaKe snake = scene.snake();
        int startScore = scene.score();

        for (int cell = 0; cell < maxCells; cell++) {
            if (scene.score() > startScore) {
                return;
            }
            if (scene.phase() == GameScene.Phase.READY) {
                steerOneCellTowardsFood(scene, keys);
                scene.update(STEP);
            }
            if (scene.phase() != GameScene.Phase.RUNNING) {
                throw new AssertionError("the run ended before the food was reached, phase=" + scene.phase());
            }
            if (!snake.head().equals(scene.food().position())) {
                steerOneCellTowardsFood(scene, keys);
            }
            if (!advanceOneCell(scene)) {
                throw new AssertionError("the run ended after " + cell + " cells, short of the food");
            }
        }
        throw new AssertionError("the food was never reached within " + maxCells + " cells");
    }

    /**
     * Presses one key that moves the snake one cell closer to the food. When the shortest route
     * doubles back the snake refuses to reverse, so a sidestep is taken instead and the route
     * replanned on the next cell, which is how a player gets around the same restriction.
     */
    private static void steerOneCellTowardsFood(GameScene scene, KL keys) {
        SnaKe snake = scene.snake();
        Direction route = routeToFood(snake, scene.food());
        if (snake.length() == 1 || route != snake.direction().opposite()) {
            tap(keys, keyCodeFor(route));
            return;
        }

        Cell goal = scene.food().position();
        Direction sidestep = null;
        int best = Integer.MAX_VALUE;
        for (Direction side : perpendicularTo(snake.direction())) {
            Cell next = snake.head().translate(side);
            if (snake.isOnBoard(next) && !snake.occupies(next)) {
                int distance = Math.abs(next.x() - goal.x()) + Math.abs(next.y() - goal.y());
                if (distance < best) {
                    best = distance;
                    sidestep = side;
                }
            }
        }
        tap(keys, keyCodeFor(sidestep != null ? sidestep : route));
    }

    private static List<Direction> perpendicularTo(Direction direction) {
        return direction.isHorizontal() ? List.of(Direction.UP, Direction.DOWN)
                : List.of(Direction.LEFT, Direction.RIGHT);
    }

    /** Steers the snake one cell at a time in a fixed direction until the run ends. */
    static void driveInDirection(GameScene scene, KL keys, Direction direction, int maxCells) {
        if (scene.phase() == GameScene.Phase.READY) {
            tap(keys, keyCodeFor(direction));
            scene.update(STEP);
        }
        for (int cell = 0; cell < maxCells && scene.phase() == GameScene.Phase.RUNNING; cell++) {
            tap(keys, keyCodeFor(direction));
            advanceOneCell(scene);
        }
    }

    /**
     * Breadth first route from the head to the goal that avoids the snake's own body, returned
     * as the single direction to press next.
     */
    static Direction routeToFood(SnaKe snake, Food food) {
        return routeTo(snake, food.position());
    }

    static Direction routeTo(SnaKe snake, Cell goal) {
        // The tail is the one body cell that will have been vacated by the time the head arrives.
        List<Cell> body = snake.segments().subList(0, snake.length() - 1);
        Map<Cell, Cell> cameFrom = new HashMap<>();
        Deque<Cell> frontier = new ArrayDeque<>();
        frontier.add(snake.head());
        cameFrom.put(snake.head(), null);

        while (!frontier.isEmpty()) {
            Cell current = frontier.poll();
            if (current.equals(goal)) {
                Cell previous = cameFrom.get(current);
                if (previous == null) {
                    throw new AssertionError("already on the target cell " + current);
                }
                return directionBetween(previous, current);
            }
            for (Direction direction : Direction.values()) {
                Cell next = current.translate(direction);
                if (!snake.isOnBoard(next) || body.contains(next) || cameFrom.containsKey(next)) {
                    continue;
                }
                cameFrom.put(next, current);
                frontier.add(next);
            }
        }
        throw new AssertionError("no route from " + snake.head() + " to " + goal);
    }

    private static Direction directionBetween(Cell from, Cell to) {
        if (to.x() > from.x()) {
            return Direction.RIGHT;
        }
        if (to.x() < from.x()) {
            return Direction.LEFT;
        }
        return to.y() > from.y() ? Direction.DOWN : Direction.UP;
    }

    static int keyCodeFor(Direction direction) {
        return switch (direction) {
            case UP -> KeyEvent.VK_UP;
            case DOWN -> KeyEvent.VK_DOWN;
            case LEFT -> KeyEvent.VK_LEFT;
            case RIGHT -> KeyEvent.VK_RIGHT;
        };
    }

    /** Scenes under test never make a sound, so no test can touch the audio device. */
    static Sound silentSound() {
        return Sound.silent();
    }

    static GameScene newGameScene(FakeHost host, KL keys, HighScore highScore, Random random) {
        return new GameScene(host, keys, highScore, silentSound(), random);
    }

    static MenuScene newMenuScene(FakeHost host, KL keys, ML mouse, HighScore highScore) {
        return new MenuScene(host, keys, mouse, highScore, silentSound());
    }

    static BufferedImage render(Scene scene) {
        BufferedImage image = new BufferedImage(
                Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        try {
            g2.setColor(Constants.COLOR_BACKGROUND);
            g2.fillRect(0, 0, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
            scene.draw(g2);
        } finally {
            g2.dispose();
        }
        return image;
    }

    static int countPixels(BufferedImage image, Color color) {
        return countPixels(image, color.getRGB() & 0xFFFFFF);
    }

    static int countPixels(BufferedImage image, int rgb) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) == rgb) {
                    count++;
                }
            }
        }
        return count;
    }

    static BufferedImage crop(BufferedImage image, int x, int y, int width, int height) {
        return image.getSubimage(x, y, width, height);
    }

    static boolean pixelsEqual(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return false;
        }
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }
}
