package org.pack;

public class Time {

    public static double timeStarted = System.nanoTime();

    public static double getTime() {
        // Calculate the current time in seconds from the start time in nanoseconds.
        return (System.nanoTime() - timeStarted) / 1_000_000_000.0;
    }
}
