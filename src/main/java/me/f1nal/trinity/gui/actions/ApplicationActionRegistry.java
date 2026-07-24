package me.f1nal.trinity.gui.actions;

import me.f1nal.trinity.gui.DisplayManager;
import me.f1nal.trinity.database.inputs.ProjectInputImporter;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.gui.windows.impl.AboutWindow;
import me.f1nal.trinity.gui.windows.impl.PreferencesFrame;
import me.f1nal.trinity.gui.windows.impl.classstructure.ClassStructureWindow;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantSearchFrame;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewCache;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewFrame;
import me.f1nal.trinity.gui.windows.impl.constant.search.ConstantSearchTypeString;
import me.f1nal.trinity.gui.windows.impl.cp.ProjectBrowserFrame;
import me.f1nal.trinity.gui.windows.impl.navigation.NavigationHistoryWindow;
import me.f1nal.trinity.gui.windows.impl.pattern.PatternSearchFrame;
import me.f1nal.trinity.gui.windows.impl.pattern.PatternReplaceFrame;
import me.f1nal.trinity.gui.windows.impl.project.create.NewProjectFrame;
import me.f1nal.trinity.gui.windows.impl.project.settings.ProjectSettingsWindow;
import me.f1nal.trinity.gui.windows.impl.refactor.GlobalRenameWindow;
import me.f1nal.trinity.gui.windows.impl.themes.ThemeEditorFrame;
import me.f1nal.trinity.gui.windows.impl.xref.search.XrefSearchFrame;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Defines application commands once so every launcher invokes identical behavior. */
public final class ApplicationActionRegistry {
    public static final String NEW_PROJECT = "project.new";
    public static final String OPEN_PROJECT = "project.open";
    public static final String SAVE_PROJECT = "project.save";
    public static final String ADD_INPUT = "project.add_input";
    public static final String PROJECT_SETTINGS = "project.settings";
    public static final String PREFERENCES = "application.preferences";
    public static final String THEME_EDITOR = "application.theme_editor";
    public static final String ABOUT = "application.about";
    public static final String CHECK_UPDATES = "application.check_updates";
    public static final String PROJECT_BROWSER = "window.project_browser";
    public static final String CLASS_STRUCTURE = "window.class_structure";
    public static final String NAVIGATION_HISTORY = "window.navigation_history";
    public static final String XREF_SEARCH = "inspect.xref_search";
    public static final String CONSTANT_SEARCH = "inspect.constant_search";
    public static final String PATTERN_SEARCH = "inspect.pattern_search";
    public static final String PATTERN_REPLACE = "inspect.pattern_replace";
    public static final String VIEW_ALL_STRINGS = "inspect.all_strings";
    public static final String GLOBAL_RENAME = "refactor.global_rename";
    public static final String NAVIGATE_BACK = "navigation.back";
    public static final String NAVIGATE_FORWARD = "navigation.forward";

    private final DisplayManager displayManager;
    private final Map<String, ApplicationAction> actions = new LinkedHashMap<>();

    public ApplicationActionRegistry(DisplayManager displayManager) {
        this.displayManager = displayManager;
        this.registerActions();
    }

    private void registerActions() {
        this.register(NEW_PROJECT, "New Project", "Create a new Trinity project", "Project",
                FontAwesomeIcons.Plus, List.of("create project"), () -> true,
                () -> this.openStatic(NewProjectFrame.class));
        this.register(OPEN_PROJECT, "Open Project", "Open a Trinity database from disk", "Project",
                FontAwesomeIcons.FolderPlus, List.of("load database", "open database"), () -> true,
                displayManager::openLocalProject);
        this.register(SAVE_PROJECT, "Save Project", "Save the active Trinity database", "Project",
                FontAwesomeIcons.Save, List.of("save database"), this::hasProject,
                displayManager::saveDatabase);
        this.register(ADD_INPUT, "Add Input", "Add JAR, ZIP, or class files to the active project", "Project",
                FontAwesomeIcons.FileImport, List.of("import jar", "add jar", "add class"), this::canAddInput,
                () -> ProjectInputImporter.chooseAndImport(displayManager.getTrinity()));
        this.register(PROJECT_SETTINGS, "Project Settings", "Configure the active project", "Project",
                FontAwesomeIcons.Cogs, List.of("database settings"), this::hasProject,
                () -> this.openStatic(ProjectSettingsWindow.class));

        this.register(PREFERENCES, "Preferences", "Configure Trinity", "Application",
                FontAwesomeIcons.Cog, List.of("settings", "options"), () -> true,
                () -> this.openStatic(PreferencesFrame.class));
        this.register(THEME_EDITOR, "Theme Editor", "Create and edit Trinity themes", "Appearance",
                FontAwesomeIcons.PaintBrush, List.of("colors", "appearance"), () -> true,
                () -> this.openStatic(ThemeEditorFrame.class));
        this.register(ABOUT, "About Trinity", "Show version and application information", "Help",
                FontAwesomeIcons.QuestionCircle, List.of("version", "license"), () -> true,
                () -> this.openStatic(AboutWindow.class));
        this.register(CHECK_UPDATES, "Check for Updates", "Check GitHub for a newer Trinity release", "Help",
                FontAwesomeIcons.SyncAlt, List.of("update trinity", "latest version"), () -> true,
                me.f1nal.trinity.Main::checkForUpdatesNow);

        this.register(PROJECT_BROWSER, "Project Browser", "Show and focus the project file tree", "Window",
                FontAwesomeIcons.ProjectDiagram, List.of("files", "classes", "resources"), this::hasProject,
                () -> this.openStatic(ProjectBrowserFrame.class));
        this.register(CLASS_STRUCTURE, "Class Structure", "Show and focus the current class structure", "Window",
                FontAwesomeIcons.ListAlt, List.of("members", "methods", "fields"), this::hasProject,
                () -> this.openStatic(ClassStructureWindow.class));
        this.register(NAVIGATION_HISTORY, "Navigation History", "Show previous decompiler navigations", "Window",
                FontAwesomeIcons.History, List.of("recent navigation", "back forward"), this::hasProject,
                () -> this.openStatic(NavigationHistoryWindow.class));

        this.register(XREF_SEARCH, "Cross-Reference Search", "Search bytecode cross-references", "Inspect",
                FontAwesomeIcons.CodeBranch, List.of("xref", "references", "usages"), this::hasProject,
                () -> this.openStatic(XrefSearchFrame.class));
        this.register(CONSTANT_SEARCH, "Constant Search", "Find strings, numbers, and other constants", "Inspect",
                FontAwesomeIcons.Search, List.of("constant viewer", "find constant", "literal search"), this::hasProject,
                () -> this.openStatic(ConstantSearchFrame.class));
        this.register(PATTERN_SEARCH, "Pattern Search", "Find bytecode instruction patterns", "Inspect",
                FontAwesomeIcons.Code, List.of("instruction search", "bytecode pattern"), this::hasProject,
                () -> this.openStatic(PatternSearchFrame.class));
        this.register(PATTERN_REPLACE, "Search & Replace", "Find and replace bytecode instruction patterns", "Inspect",
                FontAwesomeIcons.ExchangeAlt, List.of("bytecode replace", "pattern replace", "instruction replace"), this::hasProject,
                () -> this.openStatic(PatternReplaceFrame.class));
        this.register(VIEW_ALL_STRINGS, "View All Strings", "List every string constant in the project", "Inspect",
                FontAwesomeIcons.Font, List.of("strings", "string constants"), this::hasProject,
                this::viewAllStrings);
        this.register(GLOBAL_RENAME, "Global Rename", "Rename obfuscated classes and members in bulk", "Refactor",
                FontAwesomeIcons.Gavel, List.of("bulk rename", "rename all"), this::hasProject,
                () -> this.openStatic(GlobalRenameWindow.class));

        this.register(NAVIGATE_BACK, "Navigate Back", "Return to the previous decompiler location", "Navigation",
                FontAwesomeIcons.ArrowLeft, List.of("history back", "previous location"),
                () -> this.hasProject() && displayManager.getNavigationHistory().canGoBack(),
                displayManager::navigateBack);
        this.register(NAVIGATE_FORWARD, "Navigate Forward", "Go to the next decompiler location", "Navigation",
                FontAwesomeIcons.ArrowRight, List.of("history forward", "next location"),
                () -> this.hasProject() && displayManager.getNavigationHistory().canGoForward(),
                displayManager::navigateForward);
    }

    private void register(String id, String title, String description, String category, String icon,
                          List<String> aliases, java.util.function.BooleanSupplier availability,
                          Runnable executor) {
        ApplicationAction action = new ApplicationAction(id, title, description, category, icon,
                aliases, availability, executor);
        if (actions.putIfAbsent(id, action) != null) {
            throw new IllegalArgumentException("Duplicate application action: " + id);
        }
    }

    private boolean hasProject() {
        return displayManager.getTrinity() != null;
    }

    private boolean canAddInput() {
        return hasProject() && displayManager.getTrinity().getExecution().getAsynchronousLoad().isFinished();
    }

    private <T extends StaticWindow> void openStatic(Class<T> type) {
        T window = displayManager.getWindowManager().addStaticWindow(type);
        displayManager.getWindowManager().requestFocus(window);
    }

    private void viewAllStrings() {
        ConstantSearchTypeString typeString = new ConstantSearchTypeString(displayManager.getTrinity());
        List<ConstantViewCache> constants = new ArrayList<>();
        typeString.populate(constants);
        ConstantViewFrame window = new ConstantViewFrame(displayManager.getTrinity(), constants,
                typeString.getSearchDescription());
        displayManager.getWindowManager().addClosableWindow(window);
        displayManager.getWindowManager().requestFocus(window);
    }

    public ApplicationAction get(String id) {
        ApplicationAction action = actions.get(id);
        if (action == null) throw new IllegalArgumentException("Unknown application action: " + id);
        return action;
    }

    public void execute(String id) {
        this.get(id).execute();
    }

    public List<ApplicationAction> getAvailableActions() {
        return actions.values().stream().filter(ApplicationAction::isAvailable).toList();
    }
}
