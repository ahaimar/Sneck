package org.pack;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Random;

public class GameScene extends Scene {

    public enum Phase {READY, RUNNING, PAUSED, GAME_OVER, WON}

    private static final int START_X = Constants.BOARD_COLUMNS / 2;
    private static final int START_Y = Constants.BOARD_ROWS / 2;
    private static final double SNAKE_STROKE = Constants.TILE_SIZE - 6.0;
    private static final double SNAKE_HEAD_RADIUS = Constants.TILE_SIZE * 0.52;

    private final GameHost host;
    private final KL keys;
    private final HighScore highScore;
    private final Sound sound;
    private final Difficulty difficulty;

    private final SnaKe snake;
    private final Food food;

    private Phase phase = Phase.READY;
    private int score;
    private double moveTimer;
    private double clock;

    public GameScene(GameHost host, KL keys, HighScore highScore, Sound sound, Random random) {
        this.host = host;
        this.keys = keys;
        this.highScore = highScore;
        this.sound = sound;
        this.difficulty = host.difficulty();

        this.snake = new SnaKe(
                Constants.BOARD_COLUMNS, Constants.BOARD_ROWS,
                START_X, START_Y, difficulty.startLength());
        this.food = new Food(Constants.BOARD_COLUMNS, Constants.BOARD_ROWS, random);
        this.food.spawn(snake);
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    @Override
    public void onEnter() {
        keys.flush();
        host.setStateTitle(score);
    }

    public int score() {
        return score;
    }

    public int length() {
        return snake.length();
    }

    public Phase phase() {
        return phase;
    }

    public SnaKe snake() {
        return snake;
    }

    public Food food() {
        return food;
    }

    /** Seconds the snake currently waits between cells. Falls as the score climbs. */
    public double moveInterval() {
        return difficulty.moveSecondsAfter(foodsEaten());
    }

    private int foodsEaten() {
        return score / Constants.SCORE_PER_FOOD;
    }

    @Override
    public void update(double deltaTime) {
        clock += deltaTime;

        if (handleGlobalKeys()) {
            return;
        }

        switch (phase) {
            case READY -> startOnInput();
            case RUNNING -> tickPlaying(deltaTime);
            case PAUSED -> {
            }
            case GAME_OVER, WON -> {
                if (anyRestartKey()) {
                    restart();
                }
            }
        }
    }

    /** The first direction or confirm key press starts the run, so the snake never moves unasked. */
    private void startOnInput() {
        if (anyStartKey() || readDirection()) {
            phase = Phase.RUNNING;
            sound.play(Sound.Effect.START);
        }
    }

    /** @return true when the key changed scene, so the rest of the frame should be skipped. */
    private boolean handleGlobalKeys() {
        if (keys.consumePress(KeyEvent.VK_ESCAPE)) {
            host.changeState(GameState.MENU);
            return true;
        }
        if (phase == Phase.RUNNING || phase == Phase.PAUSED) {
            boolean pause = keys.consumePress(KeyEvent.VK_P)
                    || keys.consumePress(KeyEvent.VK_SPACE)
                    || keys.consumePress(KeyEvent.VK_ENTER);
            if (pause) {
                phase = phase == Phase.RUNNING ? Phase.PAUSED : Phase.RUNNING;
                return true;
            }
        }
        if (keys.consumePress(KeyEvent.VK_M)) {
            sound.toggleMuted();
        }
        return false;
    }

    private void tickPlaying(double deltaTime) {
        readDirection();

        moveTimer += deltaTime;
        double interval = moveInterval();
        while (moveTimer >= interval) {
            moveTimer -= interval;
            advance();
            if (phase != Phase.RUNNING) {
                moveTimer = 0;
                return;
            }
        }
    }

    private void advance() {
        if (snake.isDead()) {
            finish(Phase.GAME_OVER);
            return;
        }
        snake.step();

        if (food.tryEat(snake)) {
            score += Constants.SCORE_PER_FOOD;
            host.setStateTitle(score);
            sound.play(Sound.Effect.EAT);
            if (!food.spawn(snake)) {
                finish(Phase.WON);
            }
        }
    }

    private void finish(Phase finished) {
        phase = finished;
        moveTimer = 0;
        highScore.submit(score);
        sound.play(finished == Phase.WON ? Sound.Effect.WIN : Sound.Effect.DEATH);
    }

    private void restart() {
        score = 0;
        moveTimer = 0;
        snake.reset(START_X, START_Y, difficulty.startLength());
        food.spawn(snake);
        phase = Phase.READY;
        keys.flush();
        host.setStateTitle(score);
    }

    private boolean anyRestartKey() {
        return keys.consumePress(KeyEvent.VK_ENTER)
                || keys.consumePress(KeyEvent.VK_SPACE)
                || keys.consumePress(KeyEvent.VK_R);
    }

    private boolean anyStartKey() {
        return keys.consumePress(KeyEvent.VK_ENTER) || keys.consumePress(KeyEvent.VK_SPACE);
    }

    /**
     * Reads every direction key, so no press can be left latched for a later frame, and
     * returns true if any of them was pressed.
     */
    private boolean readDirection() {
        boolean up = keys.consumePress(KeyEvent.VK_UP) || keys.consumePress(KeyEvent.VK_W);
        boolean down = keys.consumePress(KeyEvent.VK_DOWN) || keys.consumePress(KeyEvent.VK_S);
        boolean right = keys.consumePress(KeyEvent.VK_RIGHT) || keys.consumePress(KeyEvent.VK_D);
        boolean left = keys.consumePress(KeyEvent.VK_LEFT) || keys.consumePress(KeyEvent.VK_A);

        if (up) {
            snake.queueTurn(Direction.UP);
        } else if (down) {
            snake.queueTurn(Direction.DOWN);
        } else if (right) {
            snake.queueTurn(Direction.RIGHT);
        } else if (left) {
            snake.queueTurn(Direction.LEFT);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void draw(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            drawBackground(g2);
            drawHud(g2);
            drawBoard(g2);
            drawFood(g2);
            drawSnake(g2);
            drawOverlay(g2);
        } finally {
            g2.dispose();
        }
    }

    private void drawBackground(Graphics2D g2) {
        g2.setPaint(new GradientPaint(
                0, 0, Constants.COLOR_BACKGROUND.brighter(),
                0, Constants.WINDOW_HEIGHT, Constants.COLOR_BACKGROUND));
        g2.fillRect(0, 0, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
    }

    private void drawHud(Graphics2D g2) {
        g2.setFont(new Font(Constants.FONT_FAMILY, Font.PLAIN, 15));
        g2.setColor(Constants.COLOR_TEXT_DIM);
        g2.drawString("SCORE " + score, Constants.BOARD_X, 30);
        String best = "BEST " + highScore.value();
        g2.drawString(best, Constants.BOARD_X + Constants.BOARD_WIDTH - g2.getFontMetrics().stringWidth(best), 30);

        String hint = switch (phase) {
            case READY -> "Arrows or WASD to start";
            case RUNNING -> "P pause  -  ESC menu";
            case PAUSED -> "P to resume";
            case GAME_OVER, WON -> "SPACE retry  -  ESC menu";
        };
        g2.setFont(new Font(Constants.FONT_FAMILY, Font.PLAIN, 13));
        g2.setColor(Constants.COLOR_TEXT_DIM);
        int width = g2.getFontMetrics().stringWidth(hint);
        g2.drawString(hint, Constants.BOARD_X + (Constants.BOARD_WIDTH - width) / 2, 30);
    }

    private void drawBoard(Graphics2D g2) {
        RoundRectangle2D board = new RoundRectangle2D.Double(
                Constants.BOARD_X - 6, Constants.BOARD_Y - 6,
                Constants.BOARD_WIDTH + 12, Constants.BOARD_HEIGHT + 12, 14, 14);
        g2.setColor(Constants.COLOR_BOARD);
        g2.fill(board);
        g2.setColor(Constants.COLOR_BOARD_BORDER);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(board);

        g2.setColor(Constants.COLOR_GRID);
        g2.setStroke(new BasicStroke(1f));
        for (int column = 1; column < Constants.BOARD_COLUMNS; column++) {
            double x = Constants.BOARD_X + column * Constants.TILE_SIZE + 0.5;
            g2.draw(new Line2D.Double(x, Constants.BOARD_Y + 1, x,
                    Constants.BOARD_Y + Constants.BOARD_HEIGHT - 1));
        }
        for (int row = 1; row < Constants.BOARD_ROWS; row++) {
            double y = Constants.BOARD_Y + row * Constants.TILE_SIZE + 0.5;
            g2.draw(new Line2D.Double(Constants.BOARD_X + 1, y,
                    Constants.BOARD_X + Constants.BOARD_WIDTH - 1, y));
        }
    }

    private void drawFood(Graphics2D g2) {
        Cell cell = food.position();
        if (cell == null) {
            return;
        }
        double cx = boardX(cell);
        double cy = boardY(cell);
        double pulse = 1.0 + 0.08 * Math.sin(clock * 6.0);
        double radius = Constants.TILE_SIZE * 0.36 * pulse;

        g2.setColor(Constants.COLOR_FOOD);
        g2.fill(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
        g2.setColor(Constants.COLOR_FOOD_HIGHLIGHT);
        g2.fill(new Ellipse2D.Double(cx - radius * 0.5, cy - radius * 0.6, radius * 0.6, radius * 0.6));
    }

    /**
     * The snake is stroked as one round-capped polyline through the segment centres, which is
     * both cheaper and far smoother than drawing a box per segment, and it guarantees the head
     * is drawn rather than skipped as the last entry of the list.
     */
    private void drawSnake(Graphics2D g2) {
        List<Cell> segments = snake.segments();
        if (segments.isEmpty()) {
            return;
        }

        Path2D.Double body = new Path2D.Double();
        for (int i = segments.size() - 1; i >= 0; i--) {
            Cell cell = segments.get(i);
            double cx = boardX(cell);
            double cy = boardY(cell);
            if (i == segments.size() - 1) {
                body.moveTo(cx, cy);
            } else {
                body.lineTo(cx, cy);
            }
        }

        g2.setStroke(new BasicStroke((float) SNAKE_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Constants.COLOR_SNAKE);
        g2.draw(body);

        Cell head = segments.get(0);
        double hx = boardX(head);
        double hy = boardY(head);
        g2.setColor(Constants.COLOR_SNAKE_HEAD);
        g2.fill(new Ellipse2D.Double(hx - SNAKE_HEAD_RADIUS, hy - SNAKE_HEAD_RADIUS,
                SNAKE_HEAD_RADIUS * 2, SNAKE_HEAD_RADIUS * 2));

        drawEyes(g2, hx, hy);
    }

    /** Two eyes set forward of the head centre, splayed to either side of the heading. */
    private void drawEyes(Graphics2D g2, double hx, double hy) {
        Direction facing = snake.direction();
        double ahead = Constants.TILE_SIZE * 0.18;
        double side = Constants.TILE_SIZE * 0.20;
        double dx = facing.dx() * ahead;
        double dy = facing.dy() * ahead;
        double sx = -facing.dy() * side;
        double sy = facing.dx() * side;

        g2.setColor(Constants.COLOR_SNAKE_EYE);
        double eye = Constants.TILE_SIZE * 0.11;
        g2.fill(new Ellipse2D.Double(hx + dx + sx - eye, hy + dy + sy - eye, eye * 2, eye * 2));
        g2.fill(new Ellipse2D.Double(hx + dx - sx - eye, hy + dy - sy - eye, eye * 2, eye * 2));
    }

    private void drawOverlay(Graphics2D g2) {
        String headline = switch (phase) {
            case READY -> null;
            case RUNNING -> null;
            case PAUSED -> "PAUSED";
            case GAME_OVER -> "GAME OVER";
            case WON -> "YOU WIN";
        };
        if (headline == null) {
            return;
        }

        g2.setColor(Constants.COLOR_OVERLAY);
        g2.fillRect(0, 0, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

        double centerY = Constants.BOARD_Y + Constants.BOARD_HEIGHT / 2.0;

        g2.setFont(new Font(Constants.FONT_FAMILY, Font.BOLD, 52));
        g2.setColor(Constants.COLOR_TEXT);
        drawCentered(g2, headline, centerY - 24);

        String detail = switch (phase) {
            case PAUSED -> "P or SPACE to resume";
            case GAME_OVER -> "final score " + score + "   -   length " + snake.length();
            case WON -> "the whole board is yours  -  score " + score;
            default -> null;
        };
        g2.setFont(new Font(Constants.FONT_FAMILY, Font.PLAIN, 18));
        g2.setColor(Constants.COLOR_TEXT_DIM);
        drawCentered(g2, detail, centerY + 14);

        if (phase != Phase.PAUSED && Math.sin(clock * 4.0) > -0.2) {
            g2.setFont(new Font(Constants.FONT_FAMILY, Font.BOLD, 18));
            g2.setColor(Constants.COLOR_TEXT_ACCENT);
            drawCentered(g2, "press SPACE to play again", centerY + 62);
        }
    }

    private void drawCentered(Graphics2D g2, String text, double y) {
        int width = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (Constants.WINDOW_WIDTH - width) / 2.0f, (float) y);
    }

    private static double boardX(Cell cell) {
        return Constants.BOARD_X + cell.centerX(Constants.TILE_SIZE);
    }

    private static double boardY(Cell cell) {
        return Constants.BOARD_Y + cell.centerY(Constants.TILE_SIZE);
    }
}
