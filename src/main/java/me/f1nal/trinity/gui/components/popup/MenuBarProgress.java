package me.f1nal.trinity.gui.components.popup;

public class MenuBarProgress {
    private final String routineName;
    private final String taskName;
    private final String progressText;
    private int progress;

    public MenuBarProgress(String routineName, String taskName, int progress) {
        this.routineName = routineName;
        this.taskName = taskName;
        this.progress = progress;
        this.progressText = null;
    }

    public MenuBarProgress(String routineName, String taskName, int completed, int total) {
        this.routineName = routineName;
        this.taskName = taskName;
        this.progress = -1;
        this.progressText = completed + "/" + total;
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

    public String getDisplayText() {
        if (progressText != null) return taskName + " (" + progressText + ")";
        return progress == -1 ? taskName : String.format("%s (%s%%)", taskName, progress);
    }
}
