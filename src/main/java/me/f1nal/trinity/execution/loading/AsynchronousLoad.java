package me.f1nal.trinity.execution.loading;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerWindow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class AsynchronousLoad extends Thread {
    private final List<ProgressiveLoadTask> taskQueue = new ArrayList<>();
    private boolean started;
    private boolean finished;
    private ProgressiveLoadTask currentTask;
    private final Trinity trinity;

    public AsynchronousLoad(Trinity trinity) {
        this.trinity = trinity;
    }

    @Override
    public void run() {
        while (!taskQueue.isEmpty()) {
            ProgressiveLoadTask task = null;
            try {
                task = this.currentTask = Objects.requireNonNull(taskQueue.remove(0));
                task.setTrinity(trinity);
                task.runImpl();
            } catch (Throwable throwable) {
                new RuntimeException("Running load task '" + (task == null ? "null" : task.getName()) + "'", throwable).printStackTrace();
                Main.exit();
            }
        }

        this.finished = true;
        this.currentTask = null;

        Main.runLater(() -> {
            Collection<ClassTarget> classTargets = trinity.getExecution().getClassTargetMap().values();
            for (ClassTarget classTarget : classTargets) {
                classTarget.resetKind();
            }
            Main.getWindowManager().getWindowsOfType(DecompilerWindow.class).forEach(DecompilerWindow::forceRefreshDecompiler);
        });
    }

    public boolean isFinished() {
        return finished;
    }

    public AsynchronousLoad add(ProgressiveLoadTask task) {
        assertNotStarted();
        taskQueue.add(task);
        return this;
    }

    public AsynchronousLoad execute() {
        assertNotStarted();
        this.started = true;
        this.start();
        return this;
    }

    private void assertNotStarted() {
        if (this.started) {
            throw new IllegalStateException();
        }
    }

    public ProgressiveLoadTask getCurrentTask() {
        return currentTask;
    }
}
