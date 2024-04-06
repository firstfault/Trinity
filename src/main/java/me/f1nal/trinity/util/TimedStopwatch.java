package me.f1nal.trinity.util;

public class TimedStopwatch {
    private final Stopwatch stopwatch = new Stopwatch();
    private long time;

    /**
     * Initializes a new {@link TimedStopwatch} object.
     * @param time Time until the stopwatch finishes.
     */
    public TimedStopwatch(long time) {
        this.time = time;
    }

    /**
     * Resets the internal {@link Stopwatch} and sets a new time.
     * @param time Time until the stopwatch finishes.
     */
    public void reset(long time) {
        this.stopwatch.reset();
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public boolean isEnabled() {
        return time >= 0;
    }

    public boolean hasPassed() {
        return isEnabled() && stopwatch.hasPassed(this.time);
    }

    public long getPassed() {
        return stopwatch.getDifference();
    }

    /**
     * @param timeAddition Time to add to check.
     */
    public boolean hasPassed(long timeAddition) {
        return isEnabled() && stopwatch.hasPassed(this.time + timeAddition);
    }
}
