package org.pack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * The snake, modelled as a deque of grid cells with the head at the front.
 *
 * <p>Timing is deliberately <em>not</em> handled here: {@link #step()} advances the snake by
 * exactly one cell, which makes the whole rule set deterministic and unit testable without a
 * window, a graphics context or a clock.
 */
public class SnaKe {

    private final int columns;
    private final int rows;

    /** Head first, tail last. */
    private final Deque<Cell> segments;

    private Direction direction;
    private final Deque<Direction> queuedTurns = new ArrayDeque<>(2);

    /** How many upcoming {@link #step()} calls must not shed the tail. */
    private int pendingGrowth;

    public SnaKe(int columns, int rows, int startX, int startY, int startLength) {
        if (columns < 1 || rows < 1) {
            throw new IllegalArgumentException("board must have at least one cell");
        }
        this.columns = columns;
        this.rows = rows;
        this.segments = new ArrayDeque<>(Math.max(1, startLength));
        build(startX, startY, startLength);
    }

    /** Returns the snake to its starting state, keeping the same board. */
    public void reset(int startX, int startY, int startLength) {
        build(startX, startY, startLength);
    }

    private void build(int startX, int startY, int startLength) {
        if (startLength < 1) {
            throw new IllegalArgumentException("startLength must be at least 1, was " + startLength);
        }
        if (startX < 0 || startX >= columns || startY < 0 || startY >= rows) {
            throw new IllegalArgumentException("start cell " + startX + "," + startY + " is off the board");
        }
        if (startX - startLength + 1 < 0) {
            throw new IllegalArgumentException("a snake of length " + startLength + " does not fit at x=" + startX);
        }
        segments.clear();
        queuedTurns.clear();
        pendingGrowth = 0;
        // Head sits on the start cell and the body trails behind it, so the snake starts heading right.
        for (int i = startLength - 1; i >= 0; i--) {
            segments.addFirst(new Cell(startX - i, startY));
        }
        this.direction = Direction.RIGHT;
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    public int length() {
        return segments.size();
    }

    public Direction direction() {
        return direction;
    }

    public Cell head() {
        return segments.peekFirst();
    }

    public Cell tail() {
        return segments.peekLast();
    }

    /** Head-to-tail snapshot, safe to iterate. */
    public List<Cell> segments() {
        return List.copyOf(segments);
    }

    /**
     * Buffers a turn so an input that lands mid-step is not swallowed. Up to two turns are
     * held, which is what makes a fast "right then up" feel responsive. Reversals and
     * no-ops relative to the last accepted direction are rejected, so the snake can never
     * fold into itself. A single-segment snake has no body to run into, so it is free to
     * reverse until it has eaten for the first time.
     */
    public void queueTurn(Direction newDirection) {
        if (newDirection == null || queuedTurns.size() >= 2) {
            return;
        }
        if (segments.size() == 1) {
            queuedTurns.clear();
            queuedTurns.addLast(newDirection);
            return;
        }
        Direction reference = queuedTurns.isEmpty() ? direction : queuedTurns.peekLast();
        if (newDirection == reference || newDirection == reference.opposite()) {
            return;
        }
        queuedTurns.addLast(newDirection);
    }

    /** The cell the head will move into on the next {@link #step()}. */
    public Cell nextHead() {
        return head().translate(nextDirection());
    }

    private Direction nextDirection() {
        Direction next = queuedTurns.peekFirst();
        return next != null ? next : direction;
    }

    public boolean isOnBoard(Cell cell) {
        return cell != null && cell.x() >= 0 && cell.x() < columns && cell.y() >= 0 && cell.y() < rows;
    }

    public boolean occupies(Cell cell) {
        return segments.contains(cell);
    }

    /**
     * Whether the next step kills the snake. A step that does not grow vacates the tail
     * cell in the same move, so sliding into the old tail position is legal.
     */
    public boolean isDead() {
        Cell next = nextHead();
        if (!isOnBoard(next)) {
            return true;
        }
        int toCheck = pendingGrowth > 0 ? segments.size() : segments.size() - 1;
        Iterator<Cell> iterator = segments.iterator();
        for (int i = 0; i < toCheck; i++) {
            if (iterator.next().equals(next)) {
                return true;
            }
        }
        return false;
    }

    /** Advances one cell, applying any queued turn and any pending growth. */
    public Cell step() {
        direction = nextDirection();
        queuedTurns.pollFirst();

        Cell next = head().translate(direction);
        segments.addFirst(next);
        if (pendingGrowth > 0) {
            pendingGrowth--;
        } else {
            segments.removeLast();
        }
        return next;
    }

    /** Queues the snake to be one cell longer after the next {@link #step()}. */
    public void grow() {
        pendingGrowth++;
    }

    public int pendingGrowth() {
        return pendingGrowth;
    }
}
