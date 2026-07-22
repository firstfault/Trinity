package me.f1nal.trinity.gui.search;

/** Small, clock-independent detector used by global keyboard gestures. */
public final class DoubleTapDetector {
    private final long maximumDelayMillis;
    private long previousTapMillis = Long.MIN_VALUE;

    public DoubleTapDetector(long maximumDelayMillis) {
        if (maximumDelayMillis <= 0L) throw new IllegalArgumentException("maximumDelayMillis");
        this.maximumDelayMillis = maximumDelayMillis;
    }

    public boolean tap(long nowMillis) {
        boolean matched = previousTapMillis != Long.MIN_VALUE
                && nowMillis >= previousTapMillis
                && nowMillis - previousTapMillis <= maximumDelayMillis;
        previousTapMillis = matched ? Long.MIN_VALUE : nowMillis;
        return matched;
    }

    public void reset() {
        previousTapMillis = Long.MIN_VALUE;
    }
}
