package org.pack;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Short synthesised blips for game events. No audio files are shipped: each effect is a
 * frequency sweep written straight into 8-bit PCM.
 *
 * <p>Playback owns a {@link SourceDataLine} on a dedicated daemon thread, so {@link #play}
 * only ever enqueues and the game thread is never blocked by the sound card. Only the most
 * recent effect is kept, because a burst of events should play the latest one rather than
 * fall behind the action.
 *
 * <p>Every failure is absorbed: a machine with no audio device, or a {@link Sound} built by
 * {@link #silent()}, simply makes no sound.
 */
public class Sound {

    public enum Effect {MENU_CLICK, START, EAT, DEATH, WIN}

    private static final int SAMPLE_RATE = 22050;
    private static final AudioFormat FORMAT =
            new AudioFormat(SAMPLE_RATE, 8, 1, true, false);

    private final BlockingQueue<Effect> queue = new LinkedBlockingQueue<>();
    private final boolean enabled;

    private volatile boolean running;
    private volatile boolean muted;

    private Sound(boolean enabled) {
        this.enabled = enabled;
    }

    /** A sound that never touches audio hardware, which is what tests and headless runs use. */
    public static Sound silent() {
        return new Sound(false);
    }

    /** Opens the audio line on a background thread. A device failure only means silence. */
    public static Sound open() {
        Sound sound = new Sound(true);
        sound.running = true;
        Thread thread = new Thread(sound::pump, "sound-pump");
        thread.setDaemon(true);
        thread.start();
        return sound;
    }

    /** Opens a sound that starts muted if that is the saved preference. */
    public static Sound openFor(Settings settings) {
        Sound sound = open();
        if (settings.isMuted()) {
            sound.toggleMuted();
        }
        return sound;
    }

    public boolean isMuted() {
        return muted;
    }

    /** @return true when sound is now off, so a caller can report or play the change. */
    public boolean toggleMuted() {
        muted = !muted;
        return muted;
    }

    public void play(Effect effect) {
        if (!enabled || muted || !running) {
            return;
        }
        queue.clear();
        queue.offer(effect);
    }

    /** Stops the audio thread; already queued effects are discarded. */
    public void shutdown() {
        running = false;
        queue.clear();
    }

    private void pump() {
        try (SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT)) {
            line.open(FORMAT);
            while (running) {
                byte[] samples = render(queue.take());
                line.flush();
                line.write(samples, 0, samples.length);
                line.drain();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (LineUnavailableException | IllegalArgumentException | IllegalStateException e) {
            shutdown();
        }
    }

    /** Package visible so a test can check the synthesised waveform without an audio device. */
    static byte[] render(Effect effect) {
        return switch (effect) {
            case MENU_CLICK -> chirp(660, 990, 0.05, 0.30, true);
            case START -> chirp(523.25, 783.99, 0.13, 0.35, true);
            case EAT -> chirp(880, 1318.50, 0.09, 0.40, false);
            case DEATH -> chirp(440, 110, 0.45, 0.40, false);
            case WIN -> chirp(523.25, 1567.98, 0.55, 0.40, false);
        };
    }

    /**
     * A tone that slides between two pitches. The envelope is a half sine, which starts and
     * ends on zero and so avoids the click a hard cut-off would produce.
     */
    private static byte[] chirp(double fromHz, double toHz, double seconds, double gain, boolean square) {
        int frames = Math.max(1, (int) (seconds * SAMPLE_RATE));
        byte[] samples = new byte[frames];
        double phase = 0;
        for (int i = 0; i < frames; i++) {
            double t = (i + 0.5) / frames;
            phase += 2 * Math.PI * (fromHz + (toHz - fromHz) * t) / SAMPLE_RATE;
            double envelope = Math.sin(Math.PI * t) * gain;
            double wave = square ? Math.signum(Math.sin(phase)) : Math.sin(phase);
            samples[i] = (byte) (wave * envelope * 127);
        }
        return samples;
    }
}
