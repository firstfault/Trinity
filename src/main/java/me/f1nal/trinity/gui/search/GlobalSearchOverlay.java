package me.f1nal.trinity.gui.search;

import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiChildFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiPopupFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.packages.ArchiveEntry;
import me.f1nal.trinity.gui.DisplayManager;
import me.f1nal.trinity.gui.actions.ApplicationAction;
import me.f1nal.trinity.gui.actions.ApplicationActionRegistry;
import me.f1nal.trinity.gui.components.CodiconIcons;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.search.GlobalSearchFileIndex.SearchFile;
import me.f1nal.trinity.gui.windows.impl.entryviewer.ArchiveEntryViewerWindow;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerWindow;
import me.f1nal.trinity.gui.windows.impl.bytecode.BytecodeEditorLauncher;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.animation.Animation;
import me.f1nal.trinity.util.animation.Easing;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Set;

/** A transient command palette drawn above the normal window system. */
public final class GlobalSearchOverlay {
    private static final int RESULT_LIMIT = 100;
    private static final int DOUBLE_SHIFT_DELAY_MILLIS = 350;
    private static final float DESIRED_WIDTH = 680.F;
    private static final float DESIRED_HEIGHT = 420.F;
    private static final float VIEWPORT_MARGIN = 28.F;
    private static final float PANEL_ROUNDING = 8.F;
    private static final float ROW_HEIGHT = 44.F;
    private static final float DIM_ALPHA = 44.F;

    private final DisplayManager displayManager;
    private final ApplicationActionRegistry actionRegistry;
    private final GlobalSearchFileIndex fileIndex = new GlobalSearchFileIndex();
    private final DoubleTapDetector shiftDetector = new DoubleTapDetector(DOUBLE_SHIFT_DELAY_MILLIS);
    private final Animation panelAnimation = new Animation(Easing.EASE_OUT_QUAD, 150L);
    private final Animation dimAnimation = new Animation(Easing.EASE_IN_OUT_QUAD, 210L);
    private final ImString query = new ImString(512);
    private final List<SearchResult> results = new ArrayList<>();

    private boolean open;
    private boolean focusSearch;
    private boolean forceRefresh = true;
    private boolean scrollToSelection;
    private boolean resultsRefreshedThisFrame;
    private boolean keyboardNavigatedThisFrame;
    private int selectedIndex;
    private int hoveredResultIndex = -1;
    private int totalMatchCount;
    private String lastQuery = "";
    private long lastFileRevision = Long.MIN_VALUE;
    private Runnable pendingAction;
    private ClassInput contextualClass;

    public GlobalSearchOverlay(DisplayManager displayManager,
                               ApplicationActionRegistry actionRegistry) {
        this.displayManager = displayManager;
        this.actionRegistry = actionRegistry;
    }

    public void setTrinity(Trinity trinity) {
        this.fileIndex.setTrinity(trinity);
        this.forceRefresh = true;
        if (open) this.close();
    }

    public void handleActivationShortcut() {
        if (this.isVisible()) return;

        boolean blockingPopup = ImGui.isPopupOpen(null, ImGuiPopupFlags.AnyPopup);
        boolean blockingWindow = displayManager.getWindowManager().hasBlockingWindow();
        if (blockingPopup || blockingWindow) {
            shiftDetector.reset();
            return;
        }

        boolean shiftPressed = ImGui.isKeyPressed(ImGuiKey.LeftShift, false)
                || ImGui.isKeyPressed(ImGuiKey.RightShift, false);
        if (shiftPressed && !ImGui.getIO().getKeyCtrl() && !ImGui.getIO().getKeyAlt()
                && !ImGui.getIO().getKeySuper()
                && shiftDetector.tap(System.currentTimeMillis())) {
            this.open();
            return;
        }

        boolean contextAllowsTab = !ImGui.isAnyItemActive() && !ImGui.getIO().getWantTextInput();
        if (contextAllowsTab && Main.getKeyBindManager().GLOBAL_SEARCH.isPressed()) this.open();
    }

    public void open() {
        DecompilerWindow decompilerWindow = displayManager.getWindowManager()
                .getFocusedWindow(DecompilerWindow.class);
        this.contextualClass = decompilerWindow == null ? null : decompilerWindow.getSelectedClass();
        this.open = true;
        this.pendingAction = null;
        this.query.set("");
        this.selectedIndex = 0;
        this.hoveredResultIndex = -1;
        this.focusSearch = true;
        this.forceRefresh = true;
        this.shiftDetector.reset();
    }

    public void close() {
        this.open = false;
        this.focusSearch = false;
        this.shiftDetector.reset();
    }

    public boolean isVisible() {
        return open || panelAnimation.getValue() > 0.01F || dimAnimation.getValue() > 0.5F;
    }

    public void draw() {
        this.panelAnimation.run(open ? 1.F : 0.F);
        this.dimAnimation.run(open ? DIM_ALPHA : 0.F);

        float opacity = panelAnimation.getValue();
        if (!open && opacity <= 0.01F && dimAnimation.getValue() <= 0.5F) {
            this.finishPendingAction();
            return;
        }

        ImGuiViewport viewport = ImGui.getMainViewport();
        float viewportX = viewport.getPosX();
        float viewportY = viewport.getPosY();
        float viewportWidth = viewport.getSizeX();
        float viewportHeight = viewport.getSizeY();
        float width = Math.max(320.F, Math.min(DESIRED_WIDTH, viewportWidth - VIEWPORT_MARGIN * 2.F));
        float height = Math.max(240.F, Math.min(DESIRED_HEIGHT, viewportHeight - VIEWPORT_MARGIN * 2.F));
        float panelX = viewportX + (viewportWidth - width) * 0.5F;
        float panelY = viewportY + (viewportHeight - height) * 0.5F + (1.F - opacity) * 10.F;

        ImGui.setNextWindowPos(viewportX, viewportY);
        ImGui.setNextWindowSize(viewportWidth, viewportHeight);
        ImGui.setNextWindowViewport(viewport.getID());
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.F, 0.F);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.F);
        ImGui.setNextWindowBgAlpha(0.F);
        int hostFlags = ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.NoDocking
                | ImGuiWindowFlags.NoNavFocus | ImGuiWindowFlags.NoScrollbar
                | ImGuiWindowFlags.NoScrollWithMouse;
        ImGui.begin("###TrinityGlobalSearchHost", hostFlags);
        ImGui.popStyleVar(2);

        ImDrawList hostDrawList = ImGui.getWindowDrawList();
        int dim = Math.max(0, Math.min(255, Math.round(dimAnimation.getValue())));
        hostDrawList.addRectFilled(viewportX, viewportY, viewportX + viewportWidth,
                viewportY + viewportHeight, ImColor.rgba(0, 0, 0, dim));

        if (open && ImGui.isMouseClicked(ImGuiMouseButton.Left)
                && !containsMouse(panelX, panelY, width, height)) {
            this.close();
        }

        if (opacity > 0.01F) {
            this.drawPanelShadow(hostDrawList, panelX, panelY, width, height, opacity);
            ImGui.setCursorScreenPos(panelX, panelY);
            ImGui.pushStyleVar(ImGuiStyleVar.Alpha, opacity);
            ImGui.pushStyleVar(ImGuiStyleVar.ChildRounding, PANEL_ROUNDING);
            ImGui.pushStyleVar(ImGuiStyleVar.ChildBorderSize, 1.F);
            ImGui.pushStyleColor(ImGuiCol.ChildBg, CodeColorScheme.POPUP_BACKGROUND);
            ImGui.pushStyleColor(ImGuiCol.Border,
                    CodeColorScheme.setAlpha(Main.getPreferences().getAccentColor().getColor(), 105));
            if (ImGui.beginChild("###TrinityGlobalSearchPanel", width, height,
                    ImGuiChildFlags.Borders, ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse)) {
                this.drawPanel(width, height, opacity);
            }
            ImGui.endChild();
            ImGui.popStyleColor(2);
            ImGui.popStyleVar(3);
        }
        ImGui.end();
    }

    private void drawPanel(float width, float height, float opacity) {
        ImDrawList drawList = ImGui.getWindowDrawList();
        float panelX = ImGui.getWindowPosX();
        float panelY = ImGui.getWindowPosY();
//        drawList.addRectFilled(panelX + 1.F, panelY + 1.F, panelX + width - 1.F,
//                panelY + 3.F, withOpacity(Main.getPreferences().getAccentColor().getColor(), opacity),
//                PANEL_ROUNDING);

        ImGui.setCursorPos(10.F, 10.F);
        boolean changed = this.drawSearchBox(width);
        if (changed) {
            this.forceRefresh = true;
            if (this.hoveredResultIndex < 0) this.selectedIndex = 0;
            this.scrollToSelection = this.hoveredResultIndex < 0;
        }

        String currentQuery = query.get();
        GlobalSearchFileIndex.Snapshot snapshot = currentQuery.isBlank()
                ? fileIndex.revisionOnly() : fileIndex.snapshot();
        if (forceRefresh || snapshot.revision() != lastFileRevision || !currentQuery.equals(lastQuery)) {
            this.refreshResults(currentQuery, snapshot);
        }

        this.handleKeyboard();

        float footerHeight = ImGui.getTextLineHeightWithSpacing() + 17.F;
        float resultHeight = Math.max(70.F, height - ImGui.getCursorPosY() - footerHeight - 13.F);
        ImGui.setCursorPosX(8.F);
        ImGui.separator();
        ImGui.setCursorPosX(8.F);
        if (ImGui.beginChild("###TrinityGlobalSearchResults", width - 16.F, resultHeight,
                ImGuiChildFlags.None, ImGuiWindowFlags.None)) {
            this.drawResults(opacity);
        }
        ImGui.endChild();

        ImGui.setCursorPosX(8.F);
        ImGui.separator();
        this.drawFooter(width);
    }

    private boolean drawSearchBox(float width) {
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 9.F, 8.F);

        float clearWidth = query.isEmpty() ? 0.F : ImGui.getFrameHeight() + 6.F;
        ImGui.setNextItemWidth(Math.max(80.F, width - ImGui.getCursorPosX() - clearWidth - 10.F));
        if (focusSearch) {
            ImGui.setKeyboardFocusHere();
            focusSearch = false;
        }
        boolean changed = ImGui.inputTextWithHint("###TrinityGlobalSearchInput",
                "Search actions, members, and project files...", query);

        if (!query.isEmpty()) {
            ImGui.sameLine(0.F, 5.F);
            if (ImGui.button("×###TrinityGlobalSearchClear", ImGui.getFrameHeight(), ImGui.getFrameHeight())) {
                query.set("");
                focusSearch = true;
                changed = true;
            }
        }
        ImGui.popStyleVar();
        return changed;
    }

    private void refreshResults(String search, GlobalSearchFileIndex.Snapshot snapshot) {
        this.results.clear();
        this.totalMatchCount = 0;
        String trimmed = search.trim();

        List<SearchResult> actions = new ArrayList<>();
        List<ApplicationAction> availableActions = new ArrayList<>(actionRegistry.getAvailableActions());
        availableActions.addAll(this.getContextualClassActions());
        for (ApplicationAction action : availableActions) {
            List<String> additional = new ArrayList<>(action.aliases());
            additional.add(action.description());
            additional.add(action.category());
            GlobalSearchMatcher.match(trimmed, action.title(), additional).ifPresent(match ->
                    actions.add(SearchResult.action(action, match)));
        }
        if (!trimmed.isEmpty()) actions.sort(RESULT_COMPARATOR);

        List<SearchResult> methods = new ArrayList<>();
        List<SearchResult> fields = new ArrayList<>();
        if (!trimmed.isEmpty() && contextualClass != null) {
            for (MethodInput method : contextualClass.getMethodMap().values()) {
                String detail = methodDetail(method);
                GlobalSearchMatcher.match(trimmed, method.getDisplayName().getName(),
                        memberAliases(method.getName(), method.getDescriptor(), detail))
                        .ifPresent(match -> methods.add(SearchResult.method(method, detail, match,
                                () -> this.openMember(method))));
            }
            for (FieldInput field : contextualClass.getFieldMap().values()) {
                String detail = fieldDetail(field);
                GlobalSearchMatcher.match(trimmed, field.getDisplayName().getName(),
                        memberAliases(field.getDetails().getName(), field.getDescriptor(), detail))
                        .ifPresent(match -> fields.add(SearchResult.field(field, detail, match,
                                () -> this.openMember(field))));
            }
            methods.sort(RESULT_COMPARATOR);
            fields.sort(RESULT_COMPARATOR);
        }

        int memberAndActionCount = actions.size() + methods.size() + fields.size();
        int remaining = RESULT_LIMIT - Math.min(RESULT_LIMIT, memberAndActionCount);
        PriorityQueue<SearchResult> bestFiles = new PriorityQueue<>(
                Math.max(1, remaining), RESULT_COMPARATOR.reversed());
        int fileMatchCount = 0;
        if (!trimmed.isEmpty()) {
            String normalizedQuery = GlobalSearchMatcher.normalize(trimmed);
            for (SearchFile file : snapshot.files()) {
                var match = GlobalSearchMatcher.matchNormalized(normalizedQuery, file.normalizedTitle(),
                        file.normalizedSearchableText());
                if (match.isEmpty()) continue;
                fileMatchCount++;
                SearchResult result = SearchResult.file(file, match.get(), () -> this.openFile(file));
                if (remaining > 0 && (bestFiles.size() < remaining
                        || RESULT_COMPARATOR.compare(result, bestFiles.peek()) < 0)) {
                    if (bestFiles.size() == remaining) bestFiles.poll();
                    bestFiles.add(result);
                }
            }
        }

        List<SearchResult> files = new ArrayList<>(bestFiles);
        files.sort(RESULT_COMPARATOR);

        this.totalMatchCount = memberAndActionCount + fileMatchCount;
        int actionLimit = Math.min(RESULT_LIMIT, actions.size());
        this.results.addAll(actions.subList(0, actionLimit));
        this.addWithinLimit(methods);
        this.addWithinLimit(fields);
        int renderedFileSlots = RESULT_LIMIT - this.results.size();
        if (renderedFileSlots > 0) {
            this.results.addAll(files.subList(0, Math.min(renderedFileSlots, files.size())));
        }

        this.selectedIndex = Math.max(0, Math.min(selectedIndex, results.size() - 1));
        this.lastQuery = search;
        this.lastFileRevision = snapshot.revision();
        this.forceRefresh = false;
        this.resultsRefreshedThisFrame = true;
    }

    private List<ApplicationAction> getContextualClassActions() {
        if (contextualClass == null) return List.of();
        String className = contextualClass.getDisplaySimpleName();
        return List.of(
                new ApplicationAction("current_class.edit", "Edit Class",
                        "Edit bytecode details for " + className, "Current Class",
                        FontAwesomeIcons.Edit, List.of("class editor", "modify class", className),
                        () -> true, () -> BytecodeEditorLauncher.edit(contextualClass)),
                new ApplicationAction("current_class.add_field", "Add Field",
                        "Add a field to " + className, "Current Class",
                        FontAwesomeIcons.Plus, List.of("new field", "create field", className),
                        () -> true, () -> BytecodeEditorLauncher.addField(contextualClass)),
                new ApplicationAction("current_class.add_method", "Add Method",
                        "Add a method to " + className, "Current Class",
                        FontAwesomeIcons.Plus, List.of("new method", "create method", className),
                        () -> true, () -> BytecodeEditorLauncher.addMethod(contextualClass))
        );
    }

    private void handleKeyboard() {
        this.keyboardNavigatedThisFrame = false;
        if (ImGui.isKeyPressed(ImGuiKey.Escape, false)) {
            this.close();
            return;
        }
        if (results.isEmpty()) return;

        int oldSelection = selectedIndex;
        if (ImGui.isKeyPressed(ImGuiKey.DownArrow, true)
                || (ImGui.isKeyPressed(ImGuiKey.Tab, false) && !ImGui.getIO().getKeyShift())) {
            selectedIndex = (selectedIndex + 1) % results.size();
            focusSearch = true;
        } else if (ImGui.isKeyPressed(ImGuiKey.UpArrow, true)
                || (ImGui.isKeyPressed(ImGuiKey.Tab, false) && ImGui.getIO().getKeyShift())) {
            selectedIndex = (selectedIndex - 1 + results.size()) % results.size();
            focusSearch = true;
        } else if (ImGui.isKeyPressed(ImGuiKey.Home, false)
                || ImGui.isKeyPressed(ImGuiKey.PageUp, false)) {
            selectedIndex = 0;
        } else if (ImGui.isKeyPressed(ImGuiKey.End, false)
                || ImGui.isKeyPressed(ImGuiKey.PageDown, false)) {
            selectedIndex = results.size() - 1;
        }
        if (oldSelection != selectedIndex) {
            scrollToSelection = true;
            keyboardNavigatedThisFrame = true;
        }

        if (ImGui.isKeyPressed(ImGuiKey.Enter, false)
                || ImGui.isKeyPressed(ImGuiKey.KeypadEnter, false)) {
            this.choose(results.get(selectedIndex));
        }
    }

    private void drawResults(float opacity) {
        int previouslyHoveredIndex = this.hoveredResultIndex;
        this.hoveredResultIndex = -1;
        boolean mouseInsideResults = ImGui.isWindowHovered();
        boolean prioritizeMouse = resultsRefreshedThisFrame && previouslyHoveredIndex >= 0
                && mouseInsideResults && !keyboardNavigatedThisFrame;
        boolean mouseMoved = Math.abs(ImGui.getIO().getMouseDeltaX()) > 0.F
                || Math.abs(ImGui.getIO().getMouseDeltaY()) > 0.F;
        boolean allowMouseSelection = prioritizeMouse || mouseMoved
                || ImGui.isMouseClicked(ImGuiMouseButton.Left);
        boolean mouseSelectionResolved = false;

        if (results.isEmpty()) {
            float y = ImGui.getCursorScreenPosY() + 26.F;
            String message = query.get().isBlank() ? "No actions are currently available"
                    : "No actions, members, or files match “" + query.get().trim() + "”";
            float x = ImGui.getWindowPosX() + (ImGui.getWindowWidth() - ImGui.calcTextSize(message).x) * 0.5F;
            ImGui.getWindowDrawList().addText(x, y,
                    withOpacity(CodeColorScheme.DISABLED, opacity), message);
            this.resultsRefreshedThisFrame = false;
            return;
        }

        ResultKind previousKind = null;
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            if (result.kind() != previousKind) {
                this.drawGroupHeading(result.kind());
                previousKind = result.kind();
            }
            boolean suppressSelection = prioritizeMouse && !mouseSelectionResolved;
            if (this.drawResultRow(result, i, opacity, allowMouseSelection, suppressSelection)) {
                mouseSelectionResolved = true;
            }
        }
        this.resultsRefreshedThisFrame = false;
    }

    private void drawGroupHeading(ResultKind kind) {
        ImGui.setCursorPosX(ImGui.getCursorPosX() + 8.F);
        ImGui.textDisabled(switch (kind) {
            case ACTION -> "ACTIONS";
            case METHOD -> "METHODS IN CURRENT CLASS";
            case FIELD -> "FIELDS IN CURRENT CLASS";
            case FILE -> "PROJECT FILES";
        });
    }

    private boolean drawResultRow(SearchResult result, int index, float opacity,
                                  boolean allowMouseSelection, boolean suppressSelection) {
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        float width = Math.max(1.F, ImGui.getContentRegionAvailX());
        float rowHeight = Math.max(ROW_HEIGHT, ImGui.getTextLineHeight() * 2.F + 10.F);
        boolean clicked = ImGui.invisibleButton("###TrinityGlobalSearchResult." + result.id(), width, rowHeight);
        boolean hovered = ImGui.isItemHovered();
        if (hovered) {
            this.hoveredResultIndex = index;
            if (allowMouseSelection) {
                selectedIndex = index;
                suppressSelection = false;
            }
        }

        ImDrawList drawList = ImGui.getWindowDrawList();
        int accent = Main.getPreferences().getAccentColor().getColor();
        if (index == selectedIndex && !suppressSelection) {
            drawList.addRectFilled(x + 2.F, y, x + width - 2.F, y + rowHeight,
                    withOpacity(CodeColorScheme.setAlpha(accent, 42), opacity), 4.F);
            drawList.addRectFilled(x + 2.F, y + 7.F, x + 4.F, y + rowHeight - 7.F,
                    withOpacity(CodeColorScheme.setAlpha(accent, 205), opacity), 2.F);
        } else if (hovered) {
            drawList.addRectFilled(x + 2.F, y, x + width - 2.F, y + rowHeight,
                    withOpacity(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 24), opacity), 4.F);
        }

        float iconX = x + 12.F;
        float iconY = y + (rowHeight - ImGui.getFontSize()) * 0.5F;
        if (result.codicon()) {
            ImFont codicon = displayManager.getFontManager().getCodiconFont();
            if (codicon != null) {
                drawList.addText(codicon, Math.round(ImGui.getFontSize()), iconX, iconY,
                        withOpacity(result.iconColor(), opacity), result.icon());
            }
        } else {
            drawList.addText(iconX, iconY, withOpacity(result.iconColor(), opacity), result.icon());
        }

        float textX = x + 39.F;
        float badgeWidth = ImGui.calcTextSize(result.badge()).x;
        float badgeX = x + width - badgeWidth - 12.F;
        float maximumTextWidth = Math.max(30.F, badgeX - textX - 14.F);
        String title = ellipsize(result.title(), maximumTextWidth);
        String detail = ellipsize(result.detail(), maximumTextWidth);
        int highlightLimit = title.equals(result.title()) ? title.length() : Math.max(0, title.length() - 3);
        float titleY = y + 5.F;
        float detailY = y + 7.F + ImGui.getTextLineHeight();

        drawList.pushClipRect(textX, y, badgeX - 6.F, y + rowHeight, true);
        this.drawHighlightedTitle(drawList, textX, titleY, title, highlightLimit,
                result.matchedCharacters(), opacity);
        drawList.addText(textX, detailY, withOpacity(CodeColorScheme.DISABLED, opacity), detail);
        drawList.popClipRect();
        drawList.addText(badgeX, y + (rowHeight - ImGui.getTextLineHeight()) * 0.5F,
                withOpacity(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 185), opacity), result.badge());

        if (scrollToSelection && index == selectedIndex) {
            ImGui.setScrollHereY(0.5F);
            scrollToSelection = false;
        }
        if (clicked) this.choose(result);
        return hovered;
    }

    private void drawHighlightedTitle(ImDrawList drawList, float x, float y, String text,
                                      int highlightLimit, List<Integer> matchedCharacters, float opacity) {
        Set<Integer> highlighted = new HashSet<>(matchedCharacters);
        int textColor = withOpacity(CodeColorScheme.TEXT, opacity);
        int accentColor = withOpacity(Main.getPreferences().getAccentColor().getColor(), opacity);
        float cursorX = x;
        int start = 0;
        while (start < text.length()) {
            boolean accent = start < highlightLimit && highlighted.contains(start);
            int end = start + 1;
            while (end < text.length()) {
                boolean nextAccent = end < highlightLimit && highlighted.contains(end);
                if (nextAccent != accent) break;
                end++;
            }
            String segment = text.substring(start, end);
            drawList.addText(cursorX, y, accent ? accentColor : textColor, segment);
            cursorX += ImGui.calcTextSize(segment).x;
            start = end;
        }
    }

    private void drawFooter(float width) {
        ImGui.setCursorPosX(14.F);
        ImGui.textDisabled(contextualClass == null
                ? "Actions, classes, and project files"
                : "Actions, members of " + contextualClass.getDisplaySimpleName() + ", and project files");
        String count = totalMatchCount > results.size()
                ? results.size() + " of " + totalMatchCount + " matches"
                : totalMatchCount + (totalMatchCount == 1 ? " match" : " matches");
        float x = width - ImGui.calcTextSize(count).x - 14.F;
        if (x > ImGui.getCursorPosX() + 24.F) {
            ImGui.sameLine(x);
            ImGui.textDisabled(count);
        }
    }

    private void choose(SearchResult result) {
        if (!open) return;
        this.pendingAction = result.executor();
        this.close();
    }

    private void openFile(SearchFile file) {
        ArchiveEntry entry = file.entry();
        if (entry instanceof ClassTarget classTarget && classTarget.getInput() != null) {
            displayManager.openDecompilerView(classTarget.getInput());
            return;
        }
        ArchiveEntryViewerWindow<?> window = entry.getDefaultViewer();
        displayManager.getWindowManager().addClosableWindow(window);
        displayManager.getWindowManager().requestFocus(window);
    }

    private void openMember(me.f1nal.trinity.execution.MemberInput<?> member) {
        displayManager.openDecompilerView(member);
    }

    private void addWithinLimit(List<SearchResult> candidates) {
        int slots = RESULT_LIMIT - this.results.size();
        if (slots > 0) this.results.addAll(candidates.subList(0, Math.min(slots, candidates.size())));
    }

    private List<String> memberAliases(String originalName, String descriptor, String detail) {
        String ownerDisplayName = contextualClass.getDisplayName().getName();
        return List.of(originalName, descriptor, detail, contextualClass.getRealName(), ownerDisplayName,
                ownerDisplayName + "." + originalName + descriptor);
    }

    private static String methodDetail(MethodInput method) {
        try {
            Type methodType = Type.getMethodType(method.getDescriptor());
            StringBuilder detail = new StringBuilder(method.getOwningClass().getDisplaySimpleName()).append("  ·  (");
            Type[] arguments = methodType.getArgumentTypes();
            for (int i = 0; i < arguments.length; i++) {
                if (i > 0) detail.append(", ");
                detail.append(simpleTypeName(arguments[i]));
            }
            return detail.append(") -> ").append(simpleTypeName(methodType.getReturnType())).toString();
        } catch (IllegalArgumentException ignored) {
            return method.getOwningClass().getDisplaySimpleName() + "  ·  " + method.getDescriptor();
        }
    }

    private static String fieldDetail(FieldInput field) {
        try {
            return field.getOwningClass().getDisplaySimpleName() + "  ·  "
                    + simpleTypeName(Type.getType(field.getDescriptor()));
        } catch (IllegalArgumentException ignored) {
            return field.getOwningClass().getDisplaySimpleName() + "  ·  " + field.getDescriptor();
        }
    }

    private static String simpleTypeName(Type type) {
        String className = type.getClassName();
        int arraySuffix = className.indexOf('[');
        String component = arraySuffix < 0 ? className : className.substring(0, arraySuffix);
        int packageSeparator = component.lastIndexOf('.');
        String simpleName = packageSeparator < 0 ? component : component.substring(packageSeparator + 1);
        return arraySuffix < 0 ? simpleName : simpleName + className.substring(arraySuffix);
    }

    private void finishPendingAction() {
        if (pendingAction == null) return;
        Runnable action = pendingAction;
        pendingAction = null;
        Main.runLater(action);
    }

    private void drawPanelShadow(ImDrawList drawList, float x, float y, float width, float height,
                                 float opacity) {
        drawList.addRectFilled(x - 9.F, y + 7.F, x + width + 9.F, y + height + 15.F,
                ImColor.rgba(0, 0, 0, Math.round(34.F * opacity)), PANEL_ROUNDING + 6.F);
        drawList.addRectFilled(x - 3.F, y + 3.F, x + width + 3.F, y + height + 7.F,
                ImColor.rgba(0, 0, 0, Math.round(48.F * opacity)), PANEL_ROUNDING + 2.F);
    }

    private static boolean containsMouse(float x, float y, float width, float height) {
        float mouseX = ImGui.getMousePosX();
        float mouseY = ImGui.getMousePosY();
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static String ellipsize(String text, float maximumWidth) {
        if (text == null || text.isEmpty() || ImGui.calcTextSize(text).x <= maximumWidth) return text;
        String ellipsis = "...";
        int low = 0;
        int high = text.length();
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (ImGui.calcTextSize(text.substring(0, middle) + ellipsis).x <= maximumWidth) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low == 0 ? ellipsis : text.substring(0, low) + ellipsis;
    }

    private static int withOpacity(int color, float opacity) {
        int alpha = color >>> 24;
        int fadedAlpha = Math.max(0, Math.min(255, Math.round(alpha * opacity)));
        return color & 0x00FFFFFF | fadedAlpha << 24;
    }

    private static final Comparator<SearchResult> RESULT_COMPARATOR = Comparator
            .comparingInt(SearchResult::score).reversed()
            .thenComparing(SearchResult::title, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(SearchResult::id);

    private enum ResultKind {
        ACTION,
        METHOD,
        FIELD,
        FILE
    }

    private record SearchResult(String id, ResultKind kind, String title, String detail,
                                String badge, String icon, boolean codicon, int iconColor,
                                int score, List<Integer> matchedCharacters, Runnable executor) {
        private static SearchResult action(ApplicationAction action, GlobalSearchMatcher.Match match) {
            return new SearchResult(action.id(), ResultKind.ACTION, action.title(), action.description(),
                    action.category(), action.icon(), false,
                    Main.getPreferences().getAccentColor().getColor(), match.score(),
                    match.matchedCharacters(), action::execute);
        }

        private static SearchResult file(SearchFile file, GlobalSearchMatcher.Match match,
                                         Runnable executor) {
            return new SearchResult("file:" + file.realPath(), ResultKind.FILE, file.title(),
                    file.parentPath(), file.type() + "  ·  " + file.size(), file.icon(), true,
                    file.color(), match.score(), match.matchedCharacters(), executor);
        }

        private static SearchResult method(MethodInput method, String detail,
                                           GlobalSearchMatcher.Match match, Runnable executor) {
            return new SearchResult("method:" + method.getDetails().getKey(), ResultKind.METHOD,
                    method.getDisplayName().getName(), detail, "Method", CodiconIcons.SYMBOL_METHOD, true,
                    CodeColorScheme.METHOD_REF, match.score(), match.matchedCharacters(), executor);
        }

        private static SearchResult field(FieldInput field, String detail,
                                          GlobalSearchMatcher.Match match, Runnable executor) {
            return new SearchResult("field:" + field.getDetails().getKey(), ResultKind.FIELD,
                    field.getDisplayName().getName(), detail, "Field", CodiconIcons.SYMBOL_FIELD, true,
                    CodeColorScheme.FIELD_REF, match.score(), match.matchedCharacters(), executor);
        }
    }
}
