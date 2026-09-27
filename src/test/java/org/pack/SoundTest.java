package org.pack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundTest {

    @Test
    @DisplayName("a silent sound is inert and never opens an audio device")
    void silentIsInert() {
        Sound sound = Sound.silent();

        for (Sound.Effect effect : Sound.Effect.values()) {
            assertDoesNotThrow(() -> sound.play(effect));
        }
        assertDoesNotThrow(sound::shutdown);
    }

    @Test
    void mutingTogglesAndReportsTheNewState() {
        Sound sound = Sound.silent();

        assertFalse(sound.isMuted(), "sound starts on");
        assertTrue(sound.toggleMuted());
        assertTrue(sound.isMuted());
        assertFalse(sound.toggleMuted());
        assertFalse(sound.isMuted());
    }

    @Test
    @DisplayName("a muted sound swallows every effect instead of queueing it")
    void mutedSwallowsEffects() {
        Sound sound = Sound.silent();
        sound.toggleMuted();

        for (Sound.Effect effect : Sound.Effect.values()) {
            assertDoesNotThrow(() -> sound.play(effect));
        }
    }

    @Test
    @DisplayName("every effect is synthesised to PCM, so a broken one cannot pass as silence")
    void everyEffectIsAudible() {
        for (Sound.Effect effect : Sound.Effect.values()) {
            byte[] samples = Sound.render(effect);

            assertTrue(samples.length > 100, effect + " is too short to hear");
            assertTrue(peak(samples) > 20, effect + " is inaudible, it never leaves zero");
            assertTrue(Math.abs(samples[0]) < 8 && Math.abs(samples[samples.length - 1]) < 8,
                    effect + " must start and end near zero, or it clicks");
        }
    }

    @Test
    void effectsAreDistinctFromEachOther() {
        for (Sound.Effect a : Sound.Effect.values()) {
            for (Sound.Effect b : Sound.Effect.values()) {
                if (a == b) {
                    continue;
                }
                assertFalse(java.util.Arrays.equals(Sound.render(a), Sound.render(b)),
                        a + " and " + b + " sound identical");
            }
        }
    }

    @Test
    @DisplayName("a rising effect really does climb in pitch")
    void risingEffectsClimb() {
        assertTrue(pitchClimbs(Sound.render(Sound.Effect.EAT)),
                "EAT should slide upwards, not sit on one note");
        assertTrue(pitchClimbs(Sound.render(Sound.Effect.DEATH)) == false,
                "DEATH should slide downwards");
    }

    @Test
    @DisplayName("opening on a machine with no audio device degrades to silence")
    void openingWithoutADevice() {
        Sound sound = assertDoesNotThrow(Sound::open);
        try {
            for (Sound.Effect effect : Sound.Effect.values()) {
                assertDoesNotThrow(() -> sound.play(effect));
            }
        } finally {
            sound.shutdown();
        }
    }

    @Test
    @DisplayName("a saved mute preference is honoured when the sound is opened")
    void openingHonoursTheSavedMutePreference(@TempDir Path dir) {
        Settings settings = new Settings(dir.resolve("settings"));

        Sound unmuted = Sound.openFor(settings);
        try {
            assertFalse(unmuted.isMuted());
        } finally {
            unmuted.shutdown();
        }

        settings.setMuted(true);
        Sound muted = Sound.openFor(settings);
        try {
            assertTrue(muted.isMuted(), "the game must come back up muted");
        } finally {
            muted.shutdown();
        }
    }

    private static int peak(byte[] samples) {
        int loudest = 0;
        for (byte sample : samples) {
            loudest = Math.max(loudest, Math.abs(sample));
        }
        return loudest;
    }

    /** @return true when the note crosses zero more often in its second half than its first. */
    private static boolean pitchClimbs(byte[] samples) {
        return zeroCrossings(samples, samples.length / 2, samples.length)
                > zeroCrossings(samples, 0, samples.length / 2);
    }

    private static int zeroCrossings(byte[] samples, int from, int to) {
        int crossings = 0;
        for (int i = Math.max(1, from); i < to && i < samples.length; i++) {
            if ((samples[i - 1] < 0) != (samples[i] < 0)) {
                crossings++;
            }
        }
        return crossings;
    }
}
