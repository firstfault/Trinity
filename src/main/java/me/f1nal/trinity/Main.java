package me.f1nal.trinity;

import imgui.app.Application;
import me.f1nal.trinity.appdata.AppDataManager;
import me.f1nal.trinity.appdata.PreferencesFile;
import me.f1nal.trinity.appdata.shutdown.ShutdownHook;
import me.f1nal.trinity.adapter.LiveTrinityApplication;
import me.f1nal.trinity.database.semaphore.DatabaseSaveShutdownHook;
import me.f1nal.trinity.gui.DisplayManager;
import me.f1nal.trinity.gui.backend.ImGuiApplication;
import me.f1nal.trinity.gui.windows.WindowManager;
import me.f1nal.trinity.keybindings.KeyBindManager;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.mcp.McpActivityLog;
import me.f1nal.trinity.mcp.TrinityMcpServer;
import me.f1nal.trinity.theme.ThemeManager;
import me.f1nal.trinity.update.UpdateChecker;
import com.google.common.collect.Queues;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.lwjgl.system.Configuration;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final String VERSION = "0.0.1";
    private static final String MACOS_FIRST_THREAD_PROPERTY = "trinity.macos.firstThread";

    /**
     * Manages the graphical user interface (GUI) elements and interactions.
     */
    private static DisplayManager displayManager;
    private static EventBus eventBus;
    private static AppDataManager appDataManager;
    private static ScheduledThreadPoolExecutor scheduler;
    private static KeyBindManager keyBindManager;
    private static ThemeManager themeManager;
    private static TrinityMcpServer mcpServer;
    private static McpActivityLog mcpActivityLog;
    private static final List<ShutdownHook> shutdownHooks = new ArrayList<>();
    private static final Queue<FutureTask<?>> scheduledTasks = Queues.newArrayDeque();
    private static final Object updateCheckLock = new Object();
    private static boolean updateCheckRunning;
    private static boolean reportUpdateCheckResult;
    private static Thread renderThread;

    public static void main(String[] args) throws IOException {
        if (args.length == 1 && "--version".equals(args[0])) {
            System.out.println(VERSION);
            return;
        }
        if (relaunchPackagedJarOnMacOsFirstThread(args)) {
            return;
        }

        Configuration.GLFW_CHECK_THREAD0.set(false);

        renderThread = Thread.currentThread();
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
        mcpActivityLog = new McpActivityLog();
        startMcpServer();
        appDataManager.getState().setLastLaunchedVersion(VERSION);
        addShutdownHook(new ShutdownHook("Database Save", new DatabaseSaveShutdownHook()));
        ImGuiApplication.launch(displayManager);
        System.out.println("see you later!");
        Main.exit();
    }

    private static boolean relaunchPackagedJarOnMacOsFirstThread(String[] args) throws IOException {
        if (!System.getProperty("os.name", "").startsWith("Mac")
                || Boolean.getBoolean(MACOS_FIRST_THREAD_PROPERTY)) {
            return false;
        }

        Path applicationPath;
        try {
            applicationPath = Path.of(Main.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI());
        } catch (URISyntaxException | RuntimeException exception) {
            return false;
        }
        if (!Files.isRegularFile(applicationPath)
                || !applicationPath.getFileName().toString().endsWith(".jar")) {
            return false;
        }

        List<String> command = new ArrayList<>(args.length + 5);
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-XstartOnFirstThread");
        command.add("-D" + MACOS_FIRST_THREAD_PROPERTY + "=true");
        command.add("-jar");
        command.add(applicationPath.toString());
        command.addAll(List.of(args));

        Process process = new ProcessBuilder(command).inheritIO().start();
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.exit(exitCode);
            }
        } catch (InterruptedException exception) {
            process.destroy();
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while launching Trinity on the macOS first thread.",
                    exception);
        }
        return true;
    }

    private static void startMcpServer() {
        if (!Boolean.parseBoolean(System.getProperty("trinity.mcp.enabled", "true"))) {
            Logging.info("Built-in MCP server is disabled");
            return;
        }

        String host = System.getProperty("trinity.mcp.host", "127.0.0.1");
        int port = Integer.getInteger("trinity.mcp.port", 7331);
        try {
            mcpServer = new TrinityMcpServer(new LiveTrinityApplication(), host, port, mcpActivityLog);
            mcpServer.start();
            Logging.info("MCP server listening at {}", mcpServer.endpoint());
        } catch (Exception exception) {
            mcpServer = null;
            Logging.error("Unable to start MCP server: {}", exception.getMessage());
        }
    }

    public static McpActivityLog getMcpActivityLog() {
        return mcpActivityLog;
    }

    public static TrinityMcpServer getMcpServer() {
        return mcpServer;
    }

    public static void checkForUpdatesOnStartup() {
        if (!getPreferences().isCheckForUpdates()) return;
        checkForUpdates(false);
    }

    public static void checkForUpdatesNow() {
        checkForUpdates(true);
    }

    private static void checkForUpdates(boolean reportResult) {
        boolean alreadyRunning;
        synchronized (updateCheckLock) {
            if (reportResult) reportUpdateCheckResult = true;
            alreadyRunning = updateCheckRunning;
            updateCheckRunning = true;
        }
        if (alreadyRunning) {
            if (reportResult) displayManager.showUpdateCheckInProgress();
            return;
        }

        UpdateChecker.checkAsync(VERSION).whenComplete((update, throwable) -> {
            boolean shouldReport;
            synchronized (updateCheckLock) {
                shouldReport = reportUpdateCheckResult;
                reportUpdateCheckResult = false;
                updateCheckRunning = false;
            }
            if (throwable != null) {
                Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
                Logging.debug("Unable to check for updates: {}", cause.getMessage());
                if (shouldReport) Main.runLater(displayManager::showUpdateCheckFailed);
                return;
            }

            Main.runLater(() -> {
                if (update.isPresent()) {
                    if (shouldReport || getPreferences().isCheckForUpdates()) {
                        displayManager.showUpdateAvailable(update.get());
                    }
                } else if (shouldReport) {
                    displayManager.showUpToDate();
                }
            });
        });
    }

    public static ListenableFuture<Object> runLater(Runnable task) {
        return callLater(Executors.callable(task));
    }

    public static <T> ListenableFuture<T> callLater(Callable<T> task) {
        ListenableFutureTask<T> future = ListenableFutureTask.create(task);
        synchronized (scheduledTasks) {
            scheduledTasks.add(future);
        }
        return future;
    }

    public static void addShutdownHook(ShutdownHook hook) {
        shutdownHooks.add(hook);

        Runtime.getRuntime().addShutdownHook(new Thread(hook, hook.getName()));
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
                task.run();
            }
        }
    }

    private static void executeShutdownHooks() {
        for (ShutdownHook shutdownHook : shutdownHooks) {
            shutdownHook.run();
        }
    }

    public static void exit() {
        executeShutdownHooks();
        Runtime.getRuntime().exit(0);
    }

    public static void assertRenderThread() {
        if (Thread.currentThread() != renderThread) {
            throw new RuntimeException("Not on render thread");
        }
    }

    public static boolean isRenderThread() {
        return Thread.currentThread() == renderThread;
    }
}
