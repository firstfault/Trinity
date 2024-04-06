package me.f1nal.trinity.util;

public enum ModifyPriority {
    LOW(5000L),
    MEDIUM(2500L),
    HIGH(0L);

    private final long time;

    ModifyPriority(long time) {
        this.time = time;
    }

    public boolean isHigherPriority(ModifyPriority priority) {
        return this.time < priority.time;
    }

    public long getTime() {
        return time;
    }
}
