package me.f1nal.trinity.execution.loading;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.logging.Logging;

public abstract class ProgressiveLoadTask {
    private int startWorkingCount = -1;
    private int workingCount;
    private final String name;
    private Trinity trinity;

    protected ProgressiveLoadTask(String name) {
        this.name = name;
    }

    public void setTrinity(Trinity trinity) {
        this.trinity = trinity;
    }

    public Trinity getTrinity() {
        return trinity;
    }

    protected final void startWork(int workSize) {
        this.startWorkingCount = this.workingCount = workSize;
    }

    protected final void finishedWork() {
        if (this.startWorkingCount == -1) throw new IllegalStateException("Must call startWork()!");
        if (--this.workingCount < 0) {
            Logging.warn("Progressive load task went below zero: {}", this.workingCount);
        }
    }

    public final String getName() {
        return name;
    }

    public abstract void runImpl();

    public int getProgress() {
        return 100 - (int) ((float) workingCount / (float) this.startWorkingCount * 100.F);
    }
}
