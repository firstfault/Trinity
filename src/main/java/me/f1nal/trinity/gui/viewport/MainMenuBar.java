package me.f1nal.trinity.gui.viewport;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiColorEditFlags;
import imgui.flag.ImGuiStyleVar;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.appdata.RecentDatabaseEntry;
import me.f1nal.trinity.database.DatabaseLoader;
import me.f1nal.trinity.execution.loading.AsynchronousLoad;
import me.f1nal.trinity.execution.loading.ProgressiveLoadTask;
import me.f1nal.trinity.gui.DisplayManager;
import me.f1nal.trinity.gui.components.CodiconIcons;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.IconFamily;
import me.f1nal.trinity.gui.components.general.FileSelectorComponent;
import me.f1nal.trinity.gui.windows.WindowManager;
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.gui.windows.impl.*;
import me.f1nal.trinity.gui.windows.impl.classstructure.ClassStructureWindow;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantSearchFrame;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewCache;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewFrame;
import me.f1nal.trinity.gui.windows.impl.constant.search.ConstantSearchTypeString;
import me.f1nal.trinity.gui.windows.impl.cp.ProjectBrowserFrame;
import me.f1nal.trinity.gui.windows.impl.project.create.NewProjectFrame;
import me.f1nal.trinity.gui.windows.impl.project.settings.ProjectSettingsWindow;
import me.f1nal.trinity.gui.windows.impl.refactor.GlobalRenameWindow;
import me.f1nal.trinity.gui.windows.impl.navigation.NavigationHistoryWindow;
import me.f1nal.trinity.gui.windows.impl.themes.ThemeEditorFrame;
import me.f1nal.trinity.gui.windows.impl.xref.search.XrefSearchFrame;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.theme.AccentColor;
import me.f1nal.trinity.theme.Theme;
import me.f1nal.trinity.theme.ThemeManager;
import me.f1nal.trinity.util.GuiUtil;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// TODO: Refactor via PopupMenuBar
public class MainMenuBar {
    private static final float NAVIGATION_BAND_SPACE_RATIO = 0.4F;
    private static final float NAVIGATION_BAND_MIN_WIDTH = 40.F;
    private final DisplayManager displayManager;
    private static final Map<Class<? extends StaticWindow>, String> windowsToolbar = new LinkedHashMap<>();
    private FileSelectorComponent databaseOpenFileSelector;

    public MainMenuBar(DisplayManager displayManager) {
        this.displayManager = displayManager;

        windowsToolbar.put(ProjectBrowserFrame.class, "Project Browser");
        windowsToolbar.put(ClassStructureWindow.class, "Class Structure");
        windowsToolbar.put(NavigationHistoryWindow.class, "Navigation History");
    }

    public void draw(ProjectNavigationBand navigationBand) {
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 4.F, 4.F);
        ImGui.beginMainMenuBar();

        if (ImGui.beginMenu("File")) {
            this.fileMenu();
            ImGui.endMenu();
        }

        if (ImGui.beginMenu("View")) {
            if (ImGui.menuItem(FontAwesomeIcons.Cog + " Preferences")) {
                getWindowManager().addStaticWindow(PreferencesFrame.class);
            }
            ImGui.separator();
            if (ImGui.beginMenu( "Themes...")) {
                ThemeManager themeManager = Main.getThemeManager();
                for (Theme theme : themeManager.getThemes()) {
                    if (ImGui.menuItem(theme.getName(), "", themeManager.getCurrentTheme() == theme)) {
                        themeManager.setCurrentTheme(theme);
                    }
                }
                ImGui.endMenu();
            }
            if (ImGui.beginMenu("Accent...")) {
                AccentColor selectedAccent = Main.getPreferences().getAccentColor();
                for (AccentColor accentColor : AccentColor.values()) {
                    boolean swatchClicked = ImGui.colorButton(
                            "###MainMenuAccent." + accentColor.name(), accentColor.getRgba(),
                            ImGuiColorEditFlags.NoTooltip, 14.F, 14.F);
                    ImGui.sameLine(0.F, 6.F);
                    boolean itemClicked = ImGui.menuItem(accentColor.getName(), "", selectedAccent == accentColor);
                    if (swatchClicked || itemClicked) {
                        Main.getPreferences().setAccentColor(accentColor);
                        if (swatchClicked) ImGui.closeCurrentPopup();
                    }
                }
                ImGui.endMenu();
            }
            if (ImGui.menuItem(FontAwesomeIcons.PaintBrush + " Theme Editor")) {
                getWindowManager().addStaticWindow(ThemeEditorFrame.class);
            }
            ImGui.endMenu();
        }
        if (displayManager.getTrinity() != null) {
            if (ImGui.beginMenu("Inspect")) {/*
                if (ImGui.menuItem(FontAwesomeIcons.StreetView + " Visualize Universe")) {
                    Main.getWindowManager().addStaticWindow(ConstantSearchFrame.class);
                }

                ImGui.separator();
*/
                if (ImGui.menuItem(FontAwesomeIcons.CodeBranch + " Cross-Reference Search")) {
                    Main.getWindowManager().addStaticWindow(XrefSearchFrame.class);
                }
                if (ImGui.menuItem(FontAwesomeIcons.Search + " Constant Search")) {
                    Main.getWindowManager().addStaticWindow(ConstantSearchFrame.class);
                }
                ImGui.separator();
                if (ImGui.menuItem("View All Strings")) {
                    ConstantSearchTypeString typeString = new ConstantSearchTypeString(displayManager.getTrinity());
                    List<ConstantViewCache> constantList = new ArrayList<>();
                    typeString.populate(constantList);
                    Main.getWindowManager().addClosableWindow(new ConstantViewFrame(
                            displayManager.getTrinity(), constantList, typeString.getSearchDescription()));
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Windows")) {
                windowsToolbar.forEach((type, name) -> {
                    boolean open = getWindowManager().isStaticWindowOpen(type);
                    if (ImGui.menuItem(name, null, open)) {
                        if (open) {
                            getWindowManager().getStaticWindow(type).close();
                        } else {
                            getWindowManager().addStaticWindow(type);
                        }
                    }
                });
                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Refactor")) {
                if (ImGui.menuItem(FontAwesomeIcons.Gavel + " Global Rename")) {
                    getWindowManager().addStaticWindow(GlobalRenameWindow.class);
                }
                ImGui.endMenu();
            }
        }

        if (ImGui.beginMenu("Help")) {
            if (ImGui.menuItem("About")) {
                Main.getWindowManager().addStaticWindow(AboutWindow.class);
            }
            ImGui.endMenu();
        }

//        this.drawSaveButton();

        if (displayManager.getTrinity() != null) {
            AsynchronousLoad asynchronousLoad = displayManager.getTrinity().getExecution().getAsynchronousLoad();

            if (!asynchronousLoad.isFinished()) {
                ProgressiveLoadTask currentTask = asynchronousLoad.getCurrentTask();

                if (currentTask != null) {
                    ImGui.separator();

                    ImGui.textColored(CodeColorScheme.TEXT, "Loading Database: ");
                    ImGui.sameLine(0.F, 0.F);
                    ImGui.textColored(CodeColorScheme.DISABLED, String.format("%s (%s%%)", currentTask.getName(), currentTask.getProgress()));

                    // Slow the rendering thread to allow loading more resources.
                    try {
                        Thread.sleep(5L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        this.drawNavigationBand(navigationBand);

        ImGui.endMainMenuBar();
        ImGui.popStyleVar();
    }

    private void drawNavigationBand(ProjectNavigationBand navigationBand) {
        if (navigationBand == null) return;

        float spacing = ImGui.getStyle().getItemSpacingX();
        float separatorWidth = spacing * 2.F + 1.F;
        float rightPadding = ImGui.getStyle().getWindowPaddingX();
        float emptySpace = ImGui.getWindowWidth() - ImGui.getCursorPosX() - rightPadding - separatorWidth;
        float width = emptySpace * NAVIGATION_BAND_SPACE_RATIO;
        if (width < NAVIGATION_BAND_MIN_WIDTH) return;

        float bandStart = ImGui.getWindowWidth() - rightPadding - width;
        ImGui.sameLine(bandStart - separatorWidth, 0.F);
        ImGui.separator();
        ImGui.sameLine(bandStart, 0.F);
        navigationBand.draw(width);
    }

    private void drawSaveButton() {
        ImGui.separator();

        boolean disabled = displayManager.getTrinity() == null;
        ImGui.beginDisabled(disabled);
        ImGui.pushStyleColor(ImGuiCol.Button,
                CodeColorScheme.setAlpha(CodeColorScheme.WIDGET_BACKGROUND, 255));
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered,
                CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 72));
        ImGui.pushStyleColor(ImGuiCol.ButtonActive,
                CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 100));
        float buttonSize = ImGui.getFrameHeight();
        IconFamily.CODICON.pushFont();
        boolean save = ImGui.button(CodiconIcons.SAVE + "###MainMenuSave", buttonSize, buttonSize);
        IconFamily.CODICON.popFont();
        ImGui.popStyleColor(3);
        ImGui.endDisabled();
        GuiUtil.tooltip("Save Project");

        if (save) this.saveDatabase();
    }

    private void saveDatabase() {
        if (displayManager.getTrinity() == null) return;
        getWindowManager().addPopup(new SavingDatabasePopup(displayManager.getTrinity(), status -> {
            DatabaseLoader.save.clear();
        }));
    }

    private void fileMenu() {
        if (ImGui.menuItem(FontAwesomeIcons.Plus + " New Project...")) {
            getWindowManager().addStaticWindow(NewProjectFrame.class);
        }
        ImGui.separator();
        if (ImGui.menuItem(FontAwesomeIcons.FolderPlus + " Open Project...")) {
            this.openLocalProject();
        }

        if (ImGui.beginMenu(FontAwesomeIcons.Clock + " Recently Opened")) {
            this.recentlyOpened();
            ImGui.endMenu();
        }

        if (displayManager.getTrinity() != null) {
            ImGui.separator();
            if (ImGui.menuItem(FontAwesomeIcons.FileExport + " Export JAR...")) {
                getWindowManager().addStaticWindow(ExportJarWindow.class);
            }
            ImGui.beginDisabled();
            ImGui.menuItem("Quick Export");
            ImGui.endDisabled();

            ImGui.separator();

            if (ImGui.menuItem(FontAwesomeIcons.Cogs + " Project Settings")) {
                getWindowManager().addStaticWindow(ProjectSettingsWindow.class);
            }

            if (ImGui.menuItem(FontAwesomeIcons.Save + " Save")) this.saveDatabase();

            if (ImGui.menuItem(FontAwesomeIcons.TimesCircle + " Close")) {
                displayManager.closeDatabase(() -> {
                    getWindowManager().addStaticWindow(NewProjectFrame.class);
                    Main.getAppDataManager().getState().setDatabaseLoaded(false);
                });
            }
        }

        ImGui.separator();
        if (ImGui.menuItem("Quit Trinity")) {
            displayManager.closeDatabase(() -> Main.exit());
        }
    }

    public void openLocalProject() {
        File file = this.getDatabaseOpenFileSelector().openFileChooser();
        if (file != null) {
            displayManager.openDatabase(file.getAbsolutePath());
        }
    }

    private void recentlyOpened() {
        List<RecentDatabaseEntry> databases = Main.getAppDataManager().getRecentDatabases().getSortedDatabases();
        for (int i = 0, databasesSize = databases.size(); i < databasesSize; i++) {
            RecentDatabaseEntry database = databases.get(i);
            if (Main.getTrinity() != null) {
                File path = Main.getTrinity().getDatabase().getPath();
                if (path != null && database.getPath().equals(path.getAbsolutePath())) {
                    continue;
                }
            }
            if (ImGui.menuItem(database.getName())) {
                displayManager.openDatabase(database.getPath());
            }
            if (ImGui.isItemHovered()) {
                database.setHoveringText();
            }
            if (i > 10) break;
        }
    }

    public FileSelectorComponent getDatabaseOpenFileSelector() {
        if (databaseOpenFileSelector == null) {
            File path = displayManager.getTrinity() == null ? new File("") : displayManager.getTrinity().getDatabase().getPath();
            if (!path.isDirectory() && path.getParentFile() != null) path = path.getParentFile();
            databaseOpenFileSelector = new FileSelectorComponent("Database File Selector", path.getAbsolutePath(), FileSelectorComponent.TDB_FILE_FILTER, FileDialog.LOAD);
        }
        return databaseOpenFileSelector;
    }

    public WindowManager getWindowManager() {
        return displayManager.getWindowManager();
    }
}
