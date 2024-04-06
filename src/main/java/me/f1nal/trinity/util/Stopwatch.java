package me.f1nal.trinity.util;

public final class Stopwatch {
    private long millis;

    public Stopwatch() {
        this.reset();
    }

    public Stopwatch(long millis) {
        this.millis = millis;
    }

    public long getDifference() {
        return System.currentTimeMillis() - millis;
    }

    public long getMillis() {
        return millis;
    }

    public boolean hasPassed(long time) {
        return System.currentTimeMillis() >= this.millis + time;
    }

    public void reset() {
        this.millis = System.currentTimeMillis();
    }
}