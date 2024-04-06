package me.f1nal.trinity.database.semaphore;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.gui.viewport.notifications.ICaption;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class DatabaseSemaphore implements ICaption {
    private final DatabaseLoaderRunnable runnable;
    private final Object semaphore = new Object();
    private Future<Boolean> future;
    private boolean save;

    public DatabaseSemaphore(DatabaseLoaderRunnable runnable, boolean save) {
        this.runnable = runnable;
        this.save = save;
    }

    public Future<Boolean> get(File path) {
        synchronized (semaphore) {
            if (future == null) {
                CompletableFuture<Boolean> future = new CompletableFuture<>();
                DatabaseSemaphore.this.future = future;
                new Thread(() -> {
                    try {
                        runnable.run(path);
                    } catch (Throwable throwable) {
                        Main.getDisplayManager().addNotification(new Notification(NotificationType.ERROR, DatabaseSemaphore.this,
                                ColoredStringBuilder.create()
                                        .fmt("Failed to %s database: {}".formatted(save ? "save" : "load"), throwable).get()));
                        future.complete(false);
                        return;
                    }
                    future.complete(true);
                }).start();
            }
        }
        return future;
    }

    public Boolean getStatus() {
        synchronized (semaphore) {
            if (future == null || !future.isDone()) {
                return null;
            }
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void clear() {
        synchronized (semaphore) {
            future = null;
        }
    }

    @Override
    public String getCaption() {
        return "Database Loader";
    }
}
