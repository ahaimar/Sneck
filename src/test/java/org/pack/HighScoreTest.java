package org.pack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HighScoreTest {

    @Test
    void startsAtTheInitialValueWhenThereIsNoFile(@TempDir Path dir) {
        HighScore highScore = new HighScore(dir.resolve("highscore"), 0);

        assertEquals(0, highScore.value());
        assertTrue(highScore.isNewRecord(1));
    }

    @Test
    void recordsAndPersistsTheBestScore(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("highscore");

        HighScore first = new HighScore(file, 0);
        assertTrue(first.submit(120));
        assertEquals(120, first.value());
        assertEquals("120", Files.readString(file));

        HighScore reloaded = new HighScore(file, 0);
        assertEquals(120, reloaded.value());
    }

    @Test
    void keepsTheHighestScoreOnly(@TempDir Path dir) {
        HighScore highScore = new HighScore(dir.resolve("highscore"), 0);

        assertTrue(highScore.submit(50));
        assertFalse(highScore.submit(20));
        assertFalse(highScore.submit(50), "beating the record requires strictly more");
        assertEquals(50, highScore.value());

        assertTrue(highScore.submit(90));
        assertEquals(90, highScore.value());
    }

    @Test
    @DisplayName("a corrupt or missing file falls back instead of throwing")
    void survivesUnreadableState(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("highscore");
        Files.writeString(file, "not a number");

        assertEquals(0, new HighScore(file, 0).value());
        assertEquals(7, new HighScore(file, 7).value(), "a bad value falls back to the initial one");
        assertEquals(0, new HighScore(dir.resolve("missing").resolve("deep").resolve("f"), 0).value());
    }

    @Test
    @DisplayName("an unwritable location still counts for this session")
    void survivesAnUnwritableLocation(@TempDir Path dir) throws IOException {
        // A directory where the score file is expected makes every read and write fail.
        Path blocked = Files.createDirectory(dir.resolve("blocked"));
        HighScore highScore = new HighScore(blocked, 0);

        assertEquals(0, highScore.value());
        assertTrue(highScore.submit(30));
        assertEquals(30, highScore.value());
        assertEquals(0, new HighScore(blocked, 0).value());
    }
}
