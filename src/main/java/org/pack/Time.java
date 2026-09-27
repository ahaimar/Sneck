package org.pack;

/** Monotonic seconds since the class was loaded, used to drive the fixed timestep loop. */
public final class Time {

    private static final long STARTED_AT = System.nanoTime();

    private Time() {
    }

    public static double getTime() {
        return (System.nanoTime() - STARTED_AT) / 1_000_000_000.0;
    }
}
