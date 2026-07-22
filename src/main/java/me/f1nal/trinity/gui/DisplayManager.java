package me.f1nal.trinity.gui;

import imgui.*;
import imgui.app.Application;
import imgui.app.Configuration;
import imgui.callback.ImStrConsumer;
import imgui.callback.ImStrSupplier;
import imgui.flag.*;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.appdata.RecentDatabaseEntry;
import me.f1nal.trinity.appdata.RecentDatabasesFile;
import me.f1nal.trinity.database.DatabaseLoader;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.packages.ArchiveEntryViewerType;
import me.f1nal.trinity.gui.backend.ImGuiApplication;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.FontSettings;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.popup.PopupMenu;
import me.f1nal.trinity.gui.navigation.NavigationAction;
import me.f1nal.trinity.gui.navigation.NavigationEntry;
import me.f1nal.trinity.gui.navigation.NavigationHistory;
import me.f1nal.trinity.gui.navigation.NavigationTarget;
import me.f1nal.trinity.gui.windows.WindowManager;
import me.f1nal.trinity.gui.windows.impl.AboutWindow;
import me.f1nal.trinity.gui.windows.impl.LoadingDatabasePopup;
import me.f1nal.trinity.gui.windows.impl.SavingDatabasePopup;
import me.f1nal.trinity.gui.windows.impl.cp.ProjectBrowserFrame;
import me.f1nal.trinity.gui.windows.impl.entryviewer.ArchiveEntryViewerWindow;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerWindow;
import me.f1nal.trinity.gui.windows.impl.navigation.NavigationHistoryWindow;
import me.f1nal.trinity.gui.windows.impl.project.create.NewProjectFrame;
import me.f1nal.trinity.gui.viewport.FontManager;
import me.f1nal.trinity.gui.viewport.MainMenuBar;
import me.f1nal.trinity.gui.viewport.NotificationRenderer;
import me.f1nal.trinity.gui.viewport.ProjectNavigationBand;
import me.f1nal.trinity.gui.viewport.dnd.DragAndDropHandler;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.gui.viewport.notifications.SimpleCaption;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.theme.TrinityStyle;
import me.f1nal.trinity.update.UpdateRelease;
import me.f1nal.trinity.util.Stopwatch;
import me.f1nal.trinity.util.SystemUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.io.File;
import java.util.*;
import java.util.zip.ZipFile;

public final class DisplayManager extends ImGuiApplication {
    /**
     * Initial window title to be set when first creating the display.
     */
    private final String windowTitle;
    private final MainMenuBar mainMenuBar = new MainMenuBar(this);
    private ProjectNavigationBand projectNavigationBand;
    private Trinity trinity;
    private final NotificationRenderer notificationRenderer = new NotificationRenderer();
    private final DragAndDropHandler dragAndDropHandler = new DragAndDropHandler();
    private final PopupMenu popupMenu = new PopupMenu();
    private final FontManager fontManager = new FontManager();
    private final WindowManager windowManager = new WindowManager(this);
    private final NavigationHistory navigationHistory = new NavigationHistory(this::persistNavigationHistory);
    private NavigationTarget currentDecompilerTarget;
    private boolean initialized;

    public DisplayManager(String windowTitle) {
        this.windowTitle = windowTitle;
        this.setDatabase(null);
        String mostRecentDatabasePath = Main.getAppDataManager().getRecentDatabases().getMostRecentDatabasePath();
        if (Main.getAppDataManager().getState().isDatabaseLoaded() && mostRecentDatabasePath != null) {
            this.windowManager.addPopup(new LoadingDatabasePopup(null, new File(mostRecentDatabasePath)));
        } else {
            this.windowManager.addStaticWindow(NewProjectFrame.class);
        }
    }

    @Override
    protected void onWindowCreated() {
        this.setWindowIcon("img/icon_64.png");
    }

    public void setDatabase(Trinity trinity) {
        Main.runLater(this.windowManager::resetAllWindows);
        this.navigationHistory.reset();
        this.currentDecompilerTarget = null;

        if (this.trinity != null) {
            this.trinity.getEventManager().setRegistered(false);
        }

        this.trinity = trinity;
        this.projectNavigationBand = trinity == null ? null : new ProjectNavigationBand(trinity);

        if (this.trinity != null) {
            this.trinity.getEventManager().setRegistered(true);
        } else return;

        Main.getAppDataManager().getRecentDatabases().addDatabase(new RecentDatabaseEntry(trinity.getDatabase().getName(), trinity.getDatabase().getPath().getAbsolutePath(), System.currentTimeMillis()));

        Main.runLater(() -> this.windowManager.addStaticWindow(ProjectBrowserFrame.class));
        if (Main.getPreferences().isNavigationHistoryVisible()) {
            Main.runLater(() -> this.windowManager.addStaticWindow(NavigationHistoryWindow.class));
        }
    }

    @Override
    protected void startFrame() {
        if (!this.initialized) {
            this.initializeWindow();
            this.initialized = true;
        }

        if (this.fontManager.consumeRebuildRequest()) {
            this.rebuildFontAtlas(this.fontManager::rebuildFonts);
        }
        
        super.startFrame();
    }

    @Override
    protected void endFrame() {
        super.endFrame();
    }

    @Override
    protected void initImGui(Configuration config) {
        super.initImGui(config);
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(new File(Main.getAppDataManager().getDirectory(), "gui.ini").getAbsolutePath());
        io.setConfigFlags(io.getConfigFlags() | ImGuiConfigFlags.DockingEnable);
        fontManager.setupFonts();
        CodeColorScheme.enableColorListeners();
        TrinityStyle.initialize(Main.getPreferences().getAccentColor());
        io.setGetClipboardTextFn(new ImStrSupplier() {
            @Override
            public String get() {
                return SystemUtil.getClipboard();
            }
        });
        io.setSetClipboardTextFn(new ImStrConsumer() {
            @Override
            public void accept(String str) {
                SystemUtil.copyToClipboard(str);
            }
        });
    }

    @Override
    protected void postRun() {
        super.postRun();
    }

    @Override
    protected void preRun() {
        super.preRun();
        Main.checkForUpdatesOnStartup();
    }

    @Override
    protected void preProcess() {
        super.preProcess();
    }

    @Override
    protected void configure(Configuration config) {
        config.setTitle(this.windowTitle);
    }

    @Override
    public void process() {
        FontSettings font = Main.getPreferences().getDefaultFont();
        font.pushFont();

        Main.executeScheduledTasks();
        this.mainMenuBar.draw(this.projectNavigationBand);
        this.setupDockspace();
        if (this.trinity == null && this.windowManager.getPopups().isEmpty()) this.homepage();
        this.popupMenu.draw();
        this.windowManager.draw();
        this.notificationRenderer.draw();
        this.dragAndDropHandler.draw();

        font.popFont();
    }

    private void setupDockspace() {
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.F);
        ImGuiViewport viewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(viewport.getWorkPosX(), viewport.getWorkPosY());
        ImGui.setNextWindowSize(viewport.getWorkSizeX(), viewport.getWorkSizeY());
        ImGui.setNextWindowViewport(viewport.getID());
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.F, 0.F);
        ImGui.begin("DockSpace", ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus);
        ImGui.dockSpace(123);
        ImGui.end();
        ImGui.popStyleVar(2);
    }

    private void initializeWindow() {
        GLFW.glfwMaximizeWindow(getHandle());
        GLFW.glfwSetDropCallback(getHandle(), GLFWDropCallback.create(this.dragAndDropHandler));
        GLFW.glfwSetWindowCloseCallback(getHandle(), GLFWWindowCloseCallback.create((hnd) -> {
            GLFW.glfwSetWindowShouldClose(getHandle(), false);
            Main.runLater(() -> this.closeDatabase(Main::exit));
        }));
    }

    private void homepage() {
        ImGuiViewport viewport = ImGui.getMainViewport();

        ImGui.setNextWindowPos(viewport.getWorkCenterX(), viewport.getWorkPosY() + (viewport.getWorkSizeY() / 2.14F), ImGuiCond.Always, 0.5F, 0.5F);
        ImGui.begin("Quickstart", ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoCollapse);

        ColoredString.drawText(ColoredStringBuilder.create().fmt("Welcome to {}! You are running version {}.", "Trinity", Main.VERSION).get());
        ImGui.separator();

        if (ImGui.beginListBox("###TrinityHomepageRecentProjects", ImGui.getContentRegionAvailX(), 164.F)) {
            RecentDatabasesFile recentDatabases = Main.getAppDataManager().getRecentDatabases();
            List<RecentDatabaseEntry> sortedDatabases = recentDatabases.getSortedDatabases();
            for (int i = 0; i < sortedDatabases.size(); i++) {
                RecentDatabaseEntry database = sortedDatabases.get(i);

                if (ImGui.selectable(database.getName() + "###TrinityHomepageDatabase" + i)) {
                    this.openDatabase(database.getPath());
                }
                if (ImGui.isItemHovered()) database.setHoveringText();
            }
            ImGui.endListBox();
        }

        ImGui.separator();
        if (ImGui.button(FontAwesomeIcons.Plus + " New Project")) {
            this.windowManager.addStaticWindow(NewProjectFrame.class);
        }
        ImGui.sameLine();
        if (ImGui.button(FontAwesomeIcons.FolderPlus + " Open Local Project")) {
            mainMenuBar.openLocalProject();
        }
        ImGui.sameLine();
        if (ImGui.button(FontAwesomeIcons.Question + " About")) {
            this.windowManager.addStaticWindow(AboutWindow.class);
        }
        ImGui.sameLine();
        ImGui.end();
    }

    public void addNotification(Notification notification) {
        notification.setStopwatch(new Stopwatch());
        this.notificationRenderer.add(notification);
    }

    public void showUpdateAvailable(UpdateRelease release) {
        Notification notification = new Notification(NotificationType.INFO,
                new SimpleCaption("Update Available"), ColoredStringBuilder.create()
                .fmt("Trinity {} is available. You are running {}. Click to open the release.",
                        release.version(), Main.VERSION).get());
        notification.setExpireTime(5_000L);
        notification.setClickAction(() -> SystemUtil.browseURL(release.url()));
        this.addNotification(notification);
    }

    public void showUpToDate() {
        this.showUpdateStatus(NotificationType.SUCCESS, "Trinity is up to date",
                "You are running the latest available version (" + Main.VERSION + ").");
    }

    public void showUpdateCheckFailed() {
        this.showUpdateStatus(NotificationType.WARNING, "Update Check Failed",
                "Unable to reach GitHub Releases. Check your connection and try again.");
    }

    public void showUpdateCheckInProgress() {
        this.showUpdateStatus(NotificationType.INFO, "Checking for Updates",
                "An update check is already in progress.");
    }

    private void showUpdateStatus(NotificationType type, String title, String message) {
        Notification notification = new Notification(type, new SimpleCaption(title),
                ColoredStringBuilder.create().fmt("{}", message).get());
        notification.setExpireTime(5_000L);
        this.addNotification(notification);
    }

    public void openDatabase(String path) {
        this.closeDatabase(() -> this.windowManager.addPopup(new LoadingDatabasePopup(null, new File(path))));
    }

    public void closeDatabase(Runnable after) {
        if (trinity == null) {
            after.run();
            return;
        }
        this.windowManager.addPopup(new SavingDatabasePopup(trinity, (status) -> {
            DatabaseLoader.save.clear();
            DatabaseLoader.load.clear();
            setDatabase(null);
            after.run();
        }));
    }

    public void openDecompilerView(Input<?> input) {
        this.navigateDecompilerView(input, null, NavigationAction.NAVIGATE, null);
    }

    public void openDecompilerView(Input<?> input, AbstractInsnNode instruction) {
        this.navigateDecompilerView(input, instruction, NavigationAction.NAVIGATE, null);
    }

    public void followDecompilerView(Input<?> input, NavigationAction action) {
        this.navigateDecompilerView(input, null, action, null);
    }

    public void followDecompilerView(Input<?> input, AbstractInsnNode instruction, NavigationAction action) {
        this.navigateDecompilerView(input, instruction, action, null);
    }

    public void followDecompilerView(Input<?> input, AbstractInsnNode instruction,
                                     NavigationAction action, String displayText) {
        this.navigateDecompilerView(input, instruction, action, displayText);
    }

    private void navigateDecompilerView(Input<?> input, AbstractInsnNode instruction,
                                        NavigationAction action, String displayText) {
        NavigationTarget target = NavigationTarget.capture(input, instruction);
        boolean track = trinity != null && !trinity.getDatabase().isLoading();
        if (track) {
            if (action != NavigationAction.FOLLOW_CONSTANT && action != NavigationAction.FOLLOW_PATTERN) {
                ensureCurrentNavigationRecorded();
            }
            this.navigationHistory.record(target, action, displayText);
        }
        this.openDecompilerViewDirect(input, instruction);
        this.currentDecompilerTarget = target;
        if (track) {
            showNavigationNotification(action.getNotificationPrefix() + " " + target.describe(trinity));
        }
    }

    private void openDecompilerViewDirect(Input<?> input, AbstractInsnNode instruction) {
        ArchiveEntryViewerWindow<?> viewerWindow = ArchiveEntryViewerType.DECOMPILER.getWindow(input.getOwningClass().getClassTarget());
        this.windowManager.addClosableWindow(viewerWindow);
        ((DecompilerWindow) Objects.requireNonNull(viewerWindow)).setDecompileTarget(input, instruction);
    }

    public void trackCurrentDecompilerView(Input<?> input, AbstractInsnNode instruction) {
        if (input != null) this.currentDecompilerTarget = NavigationTarget.capture(input, instruction);
    }

    public boolean navigateBack() {
        ensureCurrentNavigationRecorded();
        return this.navigationHistory.back()
                .map(entry -> replayNavigation(entry, "Navigated back to"))
                .orElse(false);
    }

    public boolean navigateForward() {
        return this.navigationHistory.forward()
                .map(entry -> replayNavigation(entry, "Navigated forward to"))
                .orElse(false);
    }

    public boolean replayNavigation(int index) {
        return this.navigationHistory.select(index)
                .map(entry -> replayNavigation(entry, "Reopened"))
                .orElse(false);
    }

    private boolean replayNavigation(NavigationEntry entry, String prefix) {
        NavigationTarget.ResolvedNavigation resolved = entry.target().resolve(trinity);
        if (resolved == null) {
            showNavigationNotification("Navigation target is no longer available");
            return false;
        }
        this.openDecompilerViewDirect(resolved.input(), resolved.instruction());
        this.currentDecompilerTarget = entry.target();
        showNavigationNotification(prefix + " " + entry.target().describe(trinity));
        return true;
    }

    private void ensureCurrentNavigationRecorded() {
        if (currentDecompilerTarget == null) return;
        NavigationEntry current = navigationHistory.getCurrent();
        if (current == null || !current.target().equals(currentDecompilerTarget)) {
            navigationHistory.record(currentDecompilerTarget, NavigationAction.NAVIGATE);
        }
    }

    private void showNavigationNotification(String message) {
        if (!Main.getPreferences().isNavigationNotifications()) return;
        Notification notification = new Notification(NotificationType.INFO,
                new SimpleCaption("Navigation"), ColoredStringBuilder.create().fmt("{}", message).get());
        notification.setExpireTime(2500L);
        this.addNotification(notification);
    }

    public NavigationHistory getNavigationHistory() {
        return navigationHistory;
    }

    private void persistNavigationHistory() {
        Trinity activeTrinity = this.trinity;
        if (activeTrinity == null || activeTrinity.getDatabase().isLoading()) return;
        activeTrinity.getDatabase().save(this.navigationHistory);
    }

    public PopupMenu getPopupMenu() {
        return popupMenu;
    }

    public void showPopup(PopupItemBuilder popup) {
        getPopupMenu().show(popup);
    }

    public FontManager getFontManager() {
        return fontManager;
    }

    public Trinity getTrinity() {
        return trinity;
    }

    public WindowManager getWindowManager() {
        return windowManager;
    }
}
