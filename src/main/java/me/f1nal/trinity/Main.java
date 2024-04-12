package me.f1nal.trinity;

import imgui.app.Application;
import me.f1nal.trinity.appdata.AppDataManager;
import me.f1nal.trinity.appdata.PreferencesFile;
import me.f1nal.trinity.database.semaphore.DatabaseSaveShutdownHook;
import me.f1nal.trinity.gui.DisplayManager;
import me.f1nal.trinity.gui.windows.WindowManager;
import me.f1nal.trinity.keybindings.KeyBindManager;
import me.f1nal.trinity.theme.ThemeManager;
import com.google.common.collect.Queues;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

import java.io.*;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final String VERSION = "0.0.1-alpha0";

    /**
     * Manages the graphical user interface (GUI) elements and interactions.
     */
    private static DisplayManager displayManager;
    private static EventBus eventBus;
    private static AppDataManager appDataManager;
    private static ScheduledThreadPoolExecutor scheduler;
    private static KeyBindManager keyBindManager;
    private static ThemeManager themeManager;
    private static final Queue<FutureTask<?>> scheduledTasks = Queues.newArrayDeque();

    public static void main(String[] args) throws IOException {
        scheduler = new ScheduledThreadPoolExecutor(1);
        scheduler.setRemoveOnCancelPolicy(true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException ex) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));

        eventBus = new EventBus();
        themeManager = new ThemeManager();
        keyBindManager = new KeyBindManager();
        appDataManager = new AppDataManager();
        appDataManager.load();
        displayManager = new DisplayManager("Trinity: " + VERSION);
        appDataManager.getState().setLastLaunchedVersion(VERSION);
        Runtime.getRuntime().addShutdownHook(new DatabaseSaveShutdownHook());
        Application.launch(displayManager);
        System.out.println("see you later!");
        Runtime.getRuntime().exit(0);
    }

    public static ListenableFuture<Object> runLater(Runnable task) {
        ListenableFutureTask<Object> future = ListenableFutureTask.create(Executors.callable(task));
        synchronized (scheduledTasks) {
            scheduledTasks.add(future);
        }
        return future;
    }

    public static KeyBindManager getKeyBindManager() {
        return keyBindManager;
    }
    public static ScheduledThreadPoolExecutor getScheduler() {
        return scheduler;
    }
    public static PreferencesFile getPreferences() {
        return appDataManager.getPreferences();
    }
    public static AppDataManager getAppDataManager() {
        return appDataManager;
    }
    public static EventBus getEventBus() {
        return eventBus;
    }
    public static DisplayManager getDisplayManager() {
        return displayManager;
    }
    public static Trinity getTrinity() {
        return displayManager.getTrinity();
    }
    public static ThemeManager getThemeManager() {
        return themeManager;
    }
    public static WindowManager getWindowManager() {
        return getDisplayManager().getWindowManager();
    }

    public static void executeScheduledTasks() {
        synchronized (scheduledTasks) {
            while (!scheduledTasks.isEmpty()) {
                FutureTask<?> task = scheduledTasks.poll();
                try {
                    task.run();
                    task.get();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}