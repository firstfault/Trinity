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
import me.f1nal.trinity.gui.windows.WindowManager;
import me.f1nal.trinity.gui.windows.impl.AboutWindow;
import me.f1nal.trinity.gui.windows.impl.LoadingDatabasePopup;
import me.f1nal.trinity.gui.windows.impl.SavingDatabasePopup;
import me.f1nal.trinity.gui.windows.impl.cp.ProjectBrowserFrame;
import me.f1nal.trinity.gui.windows.impl.entryviewer.ArchiveEntryViewerWindow;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerWindow;
import me.f1nal.trinity.gui.windows.impl.project.create.NewProjectFrame;
import me.f1nal.trinity.gui.viewport.FontManager;
import me.f1nal.trinity.gui.viewport.MainMenuBar;
import me.f1nal.trinity.gui.viewport.NotificationRenderer;
import me.f1nal.trinity.gui.viewport.dnd.DragAndDropHandler;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.Stopwatch;
import me.f1nal.trinity.util.SystemUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWWindowCloseCallback;

import java.io.File;
import java.util.*;
import java.util.zip.ZipFile;

public final class DisplayManager extends ImGuiApplication {
    /**
     * Initial window title to be set when first creating the display.
     */
    private final String windowTitle;
    private final MainMenuBar mainMenuBar = new MainMenuBar(this);
    private Trinity trinity;
    private final NotificationRenderer notificationRenderer = new NotificationRenderer();
    private final DragAndDropHandler dragAndDropHandler = new DragAndDropHandler();
    private final PopupMenu popupMenu = new PopupMenu();
    private final FontManager fontManager = new FontManager();
    private final WindowManager windowManager = new WindowManager(this);
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

    public void setDatabase(Trinity trinity) {
        Main.runLater(this.windowManager::resetAllWindows);

        if (this.trinity != null) {
            this.trinity.getEventManager().setRegistered(false);
        }

        this.trinity = trinity;

        if (this.trinity != null) {
            this.trinity.getEventManager().setRegistered(true);
        } else return;

        Main.getAppDataManager().getRecentDatabases().addDatabase(new RecentDatabaseEntry(trinity.getDatabase().getName(), trinity.getDatabase().getPath().getAbsolutePath(), System.currentTimeMillis()));

        Main.runLater(() -> this.windowManager.addStaticWindow(ProjectBrowserFrame.class));
    }

    @Override
    protected void startFrame() {
        if (!this.initialized) {
            this.initializeWindow();
            this.initialized = true;
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
        ImGuiStyle style = ImGui.getStyle();
        style.setWindowMenuButtonPosition(ImGuiDir.Right);
        style.setColor(ImGuiCol.WindowBg, 0.12f, 0.12f, 0.12f, 1.00f);
        style.setColor(ImGuiCol.FrameBg, 0.21f, 0.21f, 0.21f, 0.54f);
        style.setColor(ImGuiCol.TitleBgActive, 0.24f, 0.24f, 0.24f, 1.00f);
        style.setColor(ImGuiCol.Button, 0.68f, 0.68f, 0.68f, 0.40f);
        style.setColor(ImGuiCol.ButtonHovered, 0.66f, 0.66f, 0.66f, 1.00f);
        style.setColor(ImGuiCol.DockingEmptyBg, 25, 25, 25, 255);
        style.setColor(ImGuiCol.PopupBg, 30, 30, 30, 255);
        style.setColor(ImGuiCol.Border, 89, 89, 89, 140);
        style.setColor(ImGuiCol.Text, 185, 185, 185, 255);
        style.setColor(ImGuiCol.TableRowBgAlt, 0,0,0,0);
        CodeColorScheme.enableColorListeners();

        style.setTabRounding(0.F);
        style.setIndentSpacing(6.F);
        style.setScrollbarSize(10.F);
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

        this.setupDockspace();
        if (this.trinity == null && this.windowManager.getPopups().isEmpty()) this.homepage();
        Main.executeScheduledTasks();
        this.mainMenuBar.draw();
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

        if (ImGui.beginListBox("###TrinityHomepageRecentProjects", 400.F, 164.F)) {
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
        ArchiveEntryViewerWindow<?> viewerWindow = ArchiveEntryViewerType.DECOMPILER.getWindow(input.getOwningClass().getClassTarget());
        this.windowManager.addClosableWindow(viewerWindow);
        ((DecompilerWindow) Objects.requireNonNull(viewerWindow)).setDecompileTarget(input);
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
