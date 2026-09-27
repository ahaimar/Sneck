package org.pack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Best score, persisted between runs in {@code ~/.snake/highscore}.
 *
 * <p>Every failure is swallowed: a missing, empty, corrupt or read-only file must never stop
 * the game from starting.
 */
public class HighScore {

    private static final String DIRECTORY = ".snake";
    private static final String FILE_NAME = "highscore";

    private final Path file;
    private final int initialValue;
    private int value;

    public HighScore(Path file, int initialValue) {
        this.file = file;
        this.initialValue = initialValue;
        this.value = read();
    }

    public static HighScore loadDefault() {
        return new HighScore(Path.of(System.getProperty("user.home"), DIRECTORY, FILE_NAME), 0);
    }

    public int value() {
        return value;
    }

    public boolean isNewRecord(int score) {
        return score > value;
    }

    /** Records the score if it beats the stored best, and persists when it does. */
    public boolean submit(int score) {
        if (!isNewRecord(score)) {
            return false;
        }
        value = score;
        write();
        return true;
    }

    private int read() {
        try {
            return Math.max(initialValue, Integer.parseInt(Files.readString(file, StandardCharsets.UTF_8).trim()));
        } catch (Exception e) {
            return initialValue;
        }
    }

    private void write() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, Integer.toString(value), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // A high score that cannot be saved still counts for this session.
        }
    }
}
