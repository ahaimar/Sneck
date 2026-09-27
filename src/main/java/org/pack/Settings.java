package org.pack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * The player's lasting preferences, kept in {@code ~/.snake/settings}.
 *
 * <p>As with {@link HighScore}, a missing, empty, corrupt or read-only file must never stop
 * the game from starting, so every read and write failure falls back to the defaults.
 */
public class Settings {

    private static final String DIRECTORY = ".snake";
    private static final String FILE_NAME = "settings";
    private static final String KEY_DIFFICULTY = "difficulty";
    private static final String KEY_MUTED = "muted";

    private final Path file;
    private Difficulty difficulty = Difficulty.NORMAL;
    private boolean muted;

    public Settings(Path file) {
        this.file = file;
        read();
    }

    public static Settings loadDefault() {
        return new Settings(Path.of(System.getProperty("user.home"), DIRECTORY, FILE_NAME));
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        if (difficulty == null) {
            return;
        }
        this.difficulty = difficulty;
        save();
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        save();
    }

    private void read() {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (Exception e) {
            return;
        }
        difficulty = Difficulty.fromName(properties.getProperty(KEY_DIFFICULTY));
        muted = Boolean.parseBoolean(properties.getProperty(KEY_MUTED, "false"));
    }

    private void save() {
        Properties properties = new Properties();
        properties.setProperty(KEY_DIFFICULTY, difficulty.name());
        properties.setProperty(KEY_MUTED, Boolean.toString(muted));
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                properties.store(out, "Snake settings");
            }
        } catch (IOException ignored) {
            // A preference that cannot be saved still applies for this session.
        }
    }
}
