package org.pack;

/**
 * How fast the snake runs and how long it starts out.
 *
 * <p>Each option owns its own speed ramp, so a run reads as "start interval, then shave this
 * much off per food, but never below this floor". The intervals are all comfortably longer than
 * {@link Constants#FIXED_STEP_SECONDS}, which keeps every step exactly one cell.
 */
public enum Difficulty {

    EASY("Easy", 0.20, 0.100, 0.002, 4),
    NORMAL("Normal", 0.14, 0.055, 0.004, 4),
    HARD("Hard", 0.10, 0.040, 0.006, 5),
    INSANE("Insane", 0.075, 0.030, 0.008, 6);

    private final String label;
    private final double startMoveSeconds;
    private final double minMoveSeconds;
    private final double speedupPerFood;
    private final int startLength;

    Difficulty(String label, double startMoveSeconds, double minMoveSeconds, double speedupPerFood,
               int startLength) {
        this.label = label;
        this.startMoveSeconds = startMoveSeconds;
        this.minMoveSeconds = minMoveSeconds;
        this.speedupPerFood = speedupPerFood;
        this.startLength = startLength;
    }

    public String label() {
        return label;
    }

    /** Seconds the snake waits between cells on a fresh run. */
    public double startMoveSeconds() {
        return startMoveSeconds;
    }

    /** The floor the ramp bottoms out at, however much food has been eaten. */
    public double minMoveSeconds() {
        return minMoveSeconds;
    }

    /** Seconds shaved off the interval for every piece of food eaten. */
    public double speedupPerFood() {
        return speedupPerFood;
    }

    /** How many segments the snake starts with. */
    public int startLength() {
        return startLength;
    }

    /** @return the interval for a run that has eaten {@code foods} pieces, never below the floor. */
    public double moveSecondsAfter(int foods) {
        return Math.max(minMoveSeconds, startMoveSeconds - foods * speedupPerFood);
    }

    public Difficulty next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public Difficulty previous() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }

    /** @return the matching option, or {@link #NORMAL} when the name is missing or unrecognised. */
    public static Difficulty fromName(String name) {
        if (name != null) {
            for (Difficulty difficulty : values()) {
                if (difficulty.name().equalsIgnoreCase(name.trim())) {
                    return difficulty;
                }
            }
        }
        return NORMAL;
    }
}
