package me.f1nal.trinity.gui;

import com.google.common.io.Resources;
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
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.popup.PopupMenu;
import me.f1nal.trinity.gui.frames.ClosableWindow;
import me.f1nal.trinity.gui.frames.Popup;
import me.f1nal.trinity.gui.frames.StaticWindow;
import me.f1nal.trinity.gui.frames.impl.AboutWindow;
import me.f1nal.trinity.gui.frames.impl.LoadingDatabasePopup;
import me.f1nal.trinity.gui.frames.impl.SavingDatabasePopup;
import me.f1nal.trinity.gui.frames.impl.cp.ProjectBrowserFrame;
import me.f1nal.trinity.gui.frames.impl.entryviewer.ArchiveEntryViewerFacade;
import me.f1nal.trinity.gui.frames.impl.entryviewer.ArchiveEntryViewerWindow;
import me.f1nal.trinity.gui.frames.impl.entryviewer.impl.decompiler.DecompilerWindow;
import me.f1nal.trinity.gui.frames.impl.project.create.NewProjectFrame;
import me.f1nal.trinity.gui.viewport.MainMenuBar;
import me.f1nal.trinity.gui.viewport.NotificationRenderer;
import me.f1nal.trinity.gui.viewport.dnd.DragAndDropHandler;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.util.Stopwatch;
import me.f1nal.trinity.util.SystemUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWWindowCloseCallback;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class DisplayManager extends Application {
    /**
     * Initial window title to be set when first creating the display.
     */
    private final String windowTitle;
    private final List<ClosableWindow> closableWindows = new ArrayList<>(5);
    private final List<Popup> popups = new ArrayList<>();
    private final Map<Class<? extends StaticWindow>, StaticWindow> staticWindowMap = new HashMap<>();
    private final MainMenuBar mainMenuBar = new MainMenuBar(this);
    private Trinity trinity;
    private final ArchiveEntryViewerFacade archiveEntryViewerFacade = new ArchiveEntryViewerFacade();
    private final NotificationRenderer notificationRenderer = new NotificationRenderer();
    private final DragAndDropHandler dragAndDropHandler = new DragAndDropHandler();
    private final PopupMenu popupMenu = new PopupMenu();
    private final Queue<FutureTask<?>> scheduledTasks;
    private boolean initialized;

    public DisplayManager(String windowTitle, Queue<FutureTask<?>> scheduledTasks) {
        this.windowTitle = windowTitle;
        this.scheduledTasks = scheduledTasks;
        this.setDatabase(null);
        String mostRecentDatabasePath = Main.getAppDataManager().getRecentDatabases().getMostRecentDatabasePath();
        if (Main.getAppDataManager().getState().isDatabaseLoaded() && mostRecentDatabasePath != null) {
            this.addPopup(new LoadingDatabasePopup(null, new File(mostRecentDatabasePath)));
        } else {
            this.addStaticWindow(NewProjectFrame.class);
        }
    }

    public void setDatabase(Trinity trinity) {
        Main.runLater(() -> {
            closableWindows.forEach(ClosableWindow::close);
            closableWindows.clear();
            staticWindowMap.values().forEach(StaticWindow::close);
            staticWindowMap.clear();
        });

        if (this.trinity != null) {
            this.trinity.getEventManager().setRegistered(false);
        }

        this.trinity = trinity;

        if (this.trinity != null) {
            this.trinity.getEventManager().setRegistered(true);
        } else return;

        Main.getAppDataManager().getRecentDatabases().addDatabase(new RecentDatabaseEntry(trinity.getDatabase().getName(), trinity.getDatabase().getPath().getAbsolutePath(), System.currentTimeMillis()));
        trinity.runDeobf();

        Main.runLater(() -> {
            this.addStaticWindow(ProjectBrowserFrame.class);
        });
    }

    @Override
    protected void postProcess() {
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
    protected void initImGui(Configuration config) {
        super.initImGui(config);
        ImGuiIO io = ImGui.getIO();

        ImFont regular = io.getFonts().addFontFromMemoryTTF(loadFromResources("fonts/inter-regular.ttf"), 14.F);

        final ImFontConfig fontConfig = new ImFontConfig();
        fontConfig.setMergeMode(true);
        io.setIniFilename(new File(Main.getAppDataManager().getDirectory(), "gui.ini").getAbsolutePath());

        final ImFontGlyphRangesBuilder rangesBuilder = new ImFontGlyphRangesBuilder();
        rangesBuilder.addRanges(io.getFonts().getGlyphRangesDefault());
        rangesBuilder.addRanges(FontAwesomeIcons._IconRange);

        final short[] glyphRanges = rangesBuilder.buildRanges();

        io.getFonts().addFontFromMemoryTTF(loadFromResources("fonts/fa-solid-900.ttf"), 14, fontConfig, glyphRanges);

        io.getFonts().addFontDefault(fontConfig);
        io.setConfigFlags(io.getConfigFlags() | ImGuiConfigFlags.DockingEnable);
        io.setConfigFlags(io.getConfigFlags() | ImGuiConfigFlags.NavEnableKeyboard);

        ImGuiStyle style = ImGui.getStyle();
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

    private static byte[] loadFromResources(String name) {
        try {
            return Resources.toByteArray(Resources.getResource(name));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        ImGui.getIO().setFontGlobalScale(Main.getPreferences().getGlobalScale());

//        ImGui.showDemoWindow();
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

        if (this.trinity == null && this.popups.isEmpty()) this.homepage(viewport);

        synchronized (this.scheduledTasks) {
            while (!this.scheduledTasks.isEmpty()) {
                FutureTask<?> task = this.scheduledTasks.poll();
                try {
                    task.run();
                    task.get();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        }

        this.mainMenuBar.draw();
        this.popupMenu.draw();

        ClosableWindow[] windows = closableWindows.toArray(new ClosableWindow[0]);
        for (ClosableWindow frame : windows) {
            frame.render();
        }

        for (StaticWindow staticWindow : staticWindowMap.values()) {
            staticWindow.render();
        }

        this.notificationRenderer.draw();
        this.drawPopups();
        this.dragAndDropHandler.draw();
    }

    private void initializeWindow() {
        GLFW.glfwMaximizeWindow(getHandle());
        GLFW.glfwSetWindowCloseCallback(getHandle(), GLFWWindowCloseCallback.create((hnd) -> {
            if (trinity != null) {
                addPopup(new SavingDatabasePopup(trinity, (status) -> System.exit(0)));
                GLFW.glfwSetWindowShouldClose(getHandle(), false);
            }
        }));
        GLFW.glfwSetDropCallback(getHandle(), GLFWDropCallback.create(this.dragAndDropHandler));
    }

    private void homepage(ImGuiViewport viewport) {
        ImGui.setNextWindowPos(viewport.getWorkCenterX(), viewport.getWorkPosY() + (viewport.getWorkSizeY() / 2.14F), ImGuiCond.Always, 0.5F, 0.5F);
        ImGui.begin("Trinity Homepage", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.AlwaysAutoResize| ImGuiWindowFlags.NoCollapse);

        ColoredString.drawText(ColoredStringBuilder.create().fmt("Welcome to {}, Java SRE tool developed by {}.", "Trinity " + Main.VERSION, "final").get());
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
            this.addStaticWindow(NewProjectFrame.class);
        }
        ImGui.sameLine();
        if (ImGui.button(FontAwesomeIcons.FolderPlus + " Open Local Project")) {
            mainMenuBar.openLocalProject();
        }
        ImGui.sameLine();
        if (ImGui.button(FontAwesomeIcons.Question + " About")) {
            this.addStaticWindow(AboutWindow.class);
        }
        ImGui.sameLine();
        ImGui.end();
    }

    public void addNotification(Notification notification) {
        notification.setStopwatch(new Stopwatch());
        this.notificationRenderer.add(notification);
    }

    public void openDatabase(String path) {
        this.closeDatabase(() -> addPopup(new LoadingDatabasePopup(null, new File(path))));
    }

    public void closeDatabase(Runnable after) {
        if (trinity == null) {
            after.run();
            return;
        }
        addPopup(new SavingDatabasePopup(trinity, (status) -> {
            DatabaseLoader.save.clear();
            DatabaseLoader.load.clear();
            setDatabase(null);
            closableWindows.clear();
            after.run();
        }));
    }

    private void drawPopups() {
        if (this.popups.isEmpty()) {
            return;
        }

        int pops = 0;
        Popup[] popups = this.popups.toArray(new Popup[0]);
        for (Popup popup : popups) {
            popup.render();
        }
        Popup last = popups[popups.length - 1];

        for (Popup popup : popups) {
            if (popup == last && !ImGui.isPopupOpen(popup.getPopupId())) {
                ImGui.openPopup(popup.getPopupId());
            }
            if (ImGui.beginPopupModal(popup.getPopupId(), ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoSavedSettings)) {
                popup.renderPopup();
                ++pops;
            }
        }

        while (pops-- != 0) ImGui.endPopup();

        if (ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.Escape))) {
            if (last.canCloseOnEscapeNow()) {
                last.close();
            }
        }
    }

    public void addClosableWindow(ClosableWindow window) {
        if (this.closableWindows.contains(window)) {
            window.setVisible(true);
            return;
        }
        this.closableWindows.add(window);
        window.setVisible(true);
        window.setCloseEvent(() -> this.closableWindows.remove(window));
    }

    public void addPopup(Popup popup) {
        this.popups.add(popup);
        popup.setCloseEvent(() -> this.popups.remove(popup));
    }

    public <T extends StaticWindow> T addStaticWindow(Class<T> type) {
        T wnd = getStaticWindow(type);
        wnd.setVisible(true);
        return wnd;
    }

    public <T extends StaticWindow> T getStaticWindow(Class<T> type) {
        //noinspection unchecked
        return (T) staticWindowMap.computeIfAbsent(type, c -> {
            try {
                Constructor<? extends StaticWindow> constructor = c.getDeclaredConstructor(Trinity.class);
                return constructor.newInstance(trinity);
            } catch (Throwable throwable) {
                throw new RuntimeException("Creating static window instance", throwable);
            }
        });
    }

    public boolean isStaticWindowOpen(Class<? extends StaticWindow> type) {
        StaticWindow window = staticWindowMap.get(type);
        return window != null && window.isVisible();
    }

    public Trinity getTrinity() {
        return trinity;
    }

    public void closeAll(Predicate<ClosableWindow> predicate) {
        this.getWindows(predicate).forEach(ClosableWindow::close);
    }

    public List<ClosableWindow> getWindows(Predicate<ClosableWindow> predicate) {
        return this.closableWindows.stream().filter(predicate).collect(Collectors.toList());
    }

    public void openDecompilerView(Input input) {
        ArchiveEntryViewerWindow<?> viewerWindow = ArchiveEntryViewerType.DECOMPILER.getWindow(input.getOwningClass().getClassTarget());
        this.addClosableWindow(viewerWindow);
        ((DecompilerWindow) Objects.requireNonNull(viewerWindow)).setDecompileTarget(input);
    }

    public ArchiveEntryViewerFacade getArchiveEntryViewerFacade() {
        return archiveEntryViewerFacade;
    }

    public PopupMenu getPopupMenu() {
        return popupMenu;
    }

    public void showPopup(PopupItemBuilder popup) {
        getPopupMenu().show(popup);
    }
}
