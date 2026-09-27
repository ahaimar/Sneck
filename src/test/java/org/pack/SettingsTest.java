package org.pack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsTest {

    @Test
    void aMissingFileFallsBackToTheDefaults() {
        Settings settings = new Settings(Path.of("no", "such", "settings"));

        assertEquals(Difficulty.NORMAL, settings.difficulty());
        assertFalse(settings.isMuted());
    }

    @Test
    void changesSurviveAReload(@TempDir Path dir) {
        Path file = dir.resolve("settings");
        new Settings(file).setDifficulty(Difficulty.INSANE);

        assertEquals(Difficulty.INSANE, new Settings(file).difficulty());
    }

    @Test
    void mutingSurvivesAReload(@TempDir Path dir) {
        Path file = dir.resolve("settings");
        new Settings(file).setMuted(true);

        assertTrue(new Settings(file).isMuted());
    }

    @Test
    @DisplayName("both preferences persist together")
    void bothPreferencesPersist(@TempDir Path dir) {
        Path file = dir.resolve("nested").resolve("settings");
        new Settings(file).setDifficulty(Difficulty.HARD);
        new Settings(file).setMuted(true);

        Settings reloaded = new Settings(file);
        assertEquals(Difficulty.HARD, reloaded.difficulty());
        assertTrue(reloaded.isMuted());
    }

    @Test
    @DisplayName("a corrupt file is ignored rather than stopping the game")
    void corruptFileFallsBack(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("settings");
        Files.writeString(file, "not a properties file at all", StandardCharsets.UTF_8);

        Settings settings = new Settings(file);
        assertEquals(Difficulty.NORMAL, settings.difficulty());
        assertFalse(settings.isMuted());
    }

    @Test
    @DisplayName("an unrecognised difficulty falls back rather than failing")
    void unknownDifficultyFallsBack(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("settings");
        Files.writeString(file, "difficulty=nightmare\nmuted=true\n", StandardCharsets.UTF_8);

        Settings settings = new Settings(file);
        assertEquals(Difficulty.NORMAL, settings.difficulty());
        assertTrue(settings.isMuted(), "the readable half of the file should still be used");
    }

    @Test
    @DisplayName("a null difficulty is ignored rather than stored")
    void nullDifficultyIsIgnored(@TempDir Path dir) {
        Path file = dir.resolve("settings");
        Settings settings = new Settings(file);

        settings.setDifficulty(Difficulty.EASY);
        settings.setDifficulty(null);

        assertEquals(Difficulty.EASY, settings.difficulty());
    }

    @Test
    @DisplayName("a file that cannot be written still applies for the session")
    void unwritableFileStillAppliesInMemory(@TempDir Path dir) throws IOException {
        // A regular file where the settings directory should be makes every write fail.
        Path blocked = dir.resolve("blocked");
        Files.writeString(blocked, "in the way", StandardCharsets.UTF_8);
        Settings settings = new Settings(blocked.resolve("settings"));

        settings.setDifficulty(Difficulty.HARD);
        settings.setMuted(true);

        assertEquals(Difficulty.HARD, settings.difficulty());
        assertTrue(settings.isMuted());
    }
}
