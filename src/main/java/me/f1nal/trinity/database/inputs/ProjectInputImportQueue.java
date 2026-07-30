package me.f1nal.trinity.database.inputs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serializes active-project imports and exposes whether work is running or queued.
 */
final class ProjectInputImportQueue {
    private final ExecutorService executor;
    private final AtomicInteger pendingTasks = new AtomicInteger();

    ProjectInputImportQueue(String threadName) {
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * @return {@code true} when this task was queued behind another import
     */
    boolean submit(Runnable task) {
        boolean queued = pendingTasks.getAndIncrement() != 0;
        try {
            executor.execute(() -> {
                try {
                    task.run();
                } finally {
                    pendingTasks.decrementAndGet();
                }
            });
        } catch (RuntimeException exception) {
            pendingTasks.decrementAndGet();
            throw exception;
        }
        return queued;
    }

    boolean isBusy() {
        return pendingTasks.get() != 0;
    }

    void shutdownNow() {
        executor.shutdownNow();
    }
}
