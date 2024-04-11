package me.f1nal.trinity.gui.components.popup;

public class MenuBarProgress {
    private final String routineName;
    private final String taskName;
    private int progress;

    public MenuBarProgress(String routineName, String taskName, int progress) {
        this.routineName = routineName;
        this.taskName = taskName;
        this.progress = progress;
    }

    public String getRoutineName() {
        return routineName;
    }

    public String getTaskName() {
        return taskName;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}
