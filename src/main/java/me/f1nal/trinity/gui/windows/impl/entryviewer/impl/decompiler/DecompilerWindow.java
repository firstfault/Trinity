package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import com.google.common.eventbus.Subscribe;
import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiListClipper;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiFocusedFlags;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiMouseCursor;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.object.DatabaseDecompiler;
import me.f1nal.trinity.decompiler.DecompiledClass;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MemberInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.packages.other.ExtractArchiveEntryRunnable;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.FontSettings;
import me.f1nal.trinity.gui.components.popup.MenuBarProgress;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.popup.PopupMenuBar;
import me.f1nal.trinity.gui.navigation.NavigationAction;
import me.f1nal.trinity.gui.navigation.NavigationTarget;
import me.f1nal.trinity.gui.navigation.NavigationViewState;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.gui.viewport.notifications.SimpleCaption;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.gui.windows.impl.classstructure.ClassStructure;
import me.f1nal.trinity.gui.windows.impl.classstructure.ClassStructureWindow;
import me.f1nal.trinity.gui.windows.impl.bytecode.BytecodeEditorLauncher;
import me.f1nal.trinity.gui.windows.impl.entryviewer.ArchiveEntryViewerWindow;
import me.f1nal.trinity.keybindings.KeyBindManager;
import me.f1nal.trinity.keybindings.HoveredInputKeyBindings;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.GuiUtil;
import me.f1nal.trinity.util.Stopwatch;
import me.f1nal.trinity.util.SystemUtil;
import me.f1nal.trinity.util.animation.Animation;
import me.f1nal.trinity.util.animation.Easing;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class DecompilerWindow extends ArchiveEntryViewerWindow<ClassTarget> implements IEventListener, IDatabaseSavable<DatabaseDecompiler> {
    private static final int MIN_LINE_NUMBER_DIGITS = 4;
    private static final int SELECTION_MATCH_BORDER = ImColor.rgba(145, 145, 145, 220);
    private static final float STICKY_HOVER_ALPHA = 28.F;
    private static final float ENUM_CARD_WIDTH = 174.F;
    private static final float ENUM_CARD_HEIGHT = 41.F;
    private static final float ENUM_CARD_MARGIN = 11.F;
    private static final float COLLAPSED_IMPORT_ALPHA = 0.30F;
    private static final long IMPORT_ALPHA_ANIMATION_TIME = 120L;
    private static final long METHOD_PRIORITY_DWELL_NANOS = 50_000_000L;
    private ClassInput selectedClass;
    private Input<?> navigationTarget;
    private AbstractInsnNode navigationInstruction;
    /**
     * Notifies the selected class must be refreshed.
     */
    private boolean forceRefresh = true;
    /** Whether this window decompiles enum bytecode as an ordinary class. */
    private boolean treatEnumAsClass;
    /** Import declarations are collapsed whenever this window opens another class. */
    private boolean importsExpanded;
    /**
     * Text component that is currently hovered.
     */
    private DecompilerComponent hoveredComponent;
    private final ImString searchText = new ImString(256);
    private final ImBoolean searchCaseSensitive = new ImBoolean();
    private final ImBoolean searchWords = new ImBoolean();
    private final ImBoolean searchRegex = new ImBoolean();
    private final List<DecompilerSearchResult> searchResults = new ArrayList<>();
    private final Map<DecompilerLine, List<DecompilerSearchResult>> searchResultsByLine =
            new IdentityHashMap<>();
    private DecompiledClass searchedClass;
    private String searchError;
    private int searchResultIndex = -1;
    private boolean searchVisible;
    private boolean focusSearch;
    private boolean selectSearchText;
    private boolean searchDirty = true;
    private boolean searchBarFocused;
    private float stickyHeaderHeight;
    private float stickyFooterHeight;
    private final Animation stickyClassHover = new Animation(Easing.EASE_OUT_QUAD, 110L);
    private final Animation stickyMethodHover = new Animation(Easing.EASE_OUT_QUAD, 110L);
    private final Animation stickyDelimiterHover = new Animation(Easing.EASE_OUT_QUAD, 110L);
    private final Animation importAlpha = new Animation(
            Easing.EASE_OUT_QUAD, IMPORT_ALPHA_ANIMATION_TIME, COLLAPSED_IMPORT_ALPHA);
    private final List<DecompilerSearchResult> selectionMatches = new ArrayList<>();
    private final Map<DecompilerLine, List<DecompilerSearchResult>> selectionMatchesByLine =
            new IdentityHashMap<>();
    private DecompiledClass selectionMatchesClass;
    private String selectionMatchText = "";
    private boolean selectionMatchesDirty = true;
    /** Visible-row indexes submitted during the current frame. */
    private final BitSet renderedVisibleRows = new BitSet();
    private List<DecompilerLine> renderedSourceLines = List.of();
    private DecompilerImportSection renderedImportSection;
    private boolean renderedImportsExpanded;
    private int viewportFirstVisibleRow;
    private int viewportEndVisibleRow;
    private DecompiledClass prioritizedViewportClass;
    private long prioritizedViewportLayoutVersion = -1L;
    private int prioritizedViewportFirstLine = -1;
    private int prioritizedViewportLastLine = -1;
    private List<MethodInput> prioritizedViewportMethods = List.of();
    private long prioritizedViewportDwellStartNanos;
    private boolean viewportPriorityPublished;
    private DecompiledClass measuredWidthClass;
    private long measuredWidthLayoutVersion = -1L;
    private float measuredWidthFontSize = -1.F;
    private float measuredSourceWidth;
    /**
     * Selection cursor.
     */
    public final DecompilerCursor cursor = new DecompilerCursor(this);
    private DecompilerAutoScroll autoscrollTo;
    private DecompilerHighlight navigationHighlight;
    private DecompilerDelimiterMatcher.Match delimiterMatch;
    private DecompiledClass delimiterMatchClass;
    private NavigationViewState pendingNavigationViewState;
    private float lastDecompilerScrollX;
    private float lastDecompilerScrollY;
    private float restoreScrollX;
    private float restoreScrollY;
    private int restoreScrollFrames;
    private final Stopwatch focusTime = new Stopwatch();
    private static Stopwatch viewMember = new Stopwatch();

    public DecompilerWindow(ClassTarget classTarget, Trinity trinity) {
        super(trinity, classTarget);
        this.treatEnumAsClass = Main.getPreferences().isDecompilerEnumClass();
        trinity.getEventManager().registerListener(this);
        this.setDecompileTarget(Objects.requireNonNull(classTarget.getInput()));
        this.setMenuBar(new PopupMenuBar(PopupItemBuilder.create().
                menu("File", file -> {
                    file
                            .menuItem("Refresh", () -> this.forceRefresh = true)
                            .predicate(() -> getDecompiledClass() != null, b -> b.separator()
                                    .menuItem("Copy", this::copyToClipboard)
                                    .menuItem("Save", () -> new ExtractArchiveEntryRunnable(classTarget.getDisplaySimpleName() + ".java", getDecompiledClass().getText().getBytes()).run()))
                    ;
                }).
                menu("Find", find -> find.menuItem("Search Text", "Ctrl+F", this::openSearch))));
    }

    @Override
    protected void onDispose() {
        trinity.getEventManager().unregisterListener(this);
        this.clearVisibleMethodPriorities();
        this.selectedClass = null;
        this.navigationTarget = null;
        this.navigationInstruction = null;
        this.searchedClass = null;
        this.selectionMatchesClass = null;
        this.clearDelimiterMatch();
        this.searchResults.clear();
        this.searchResultsByLine.clear();
        this.selectionMatches.clear();
        this.selectionMatchesByLine.clear();
        super.onDispose();
    }

    private void openSearch() {
        this.searchVisible = true;
        this.focusSearch = true;
        this.selectSearchText = true;
    }

    private void closeSearch() {
        this.searchVisible = false;
        this.focusSearch = false;
        this.searchBarFocused = false;
    }

    private void copyToClipboard() {
        final String text = cursor.hasTextSelection() ? cursor.getSelectionText() : this.getDecompiledClass().getText();
        SystemUtil.copyToClipboard(text);
    }

    @Override
    public String getTitle() {
        String simpleName = getArchiveEntry().getDisplaySimpleName();
        ClosableWindow[] windows = Main.getWindowManager().getClosableWindows().toArray(new ClosableWindow[0]);
        for (ClosableWindow window : windows) {
            if (window == this || !window.isVisible() || !(window instanceof DecompilerWindow decompilerWindow)) {
                continue;
            }
            if (simpleName.equals(decompilerWindow.getArchiveEntry().getDisplaySimpleName())) {
                return getArchiveEntry().getDisplayOrRealName() + ".java";
            }
        }
        return simpleName + ".java";
    }

    public void setDecompileTarget(ClassInput classInput) {
        this.pendingNavigationViewState = null;
        this.restoreScrollFrames = 0;
        this.navigationTarget = classInput;
        this.navigationInstruction = null;
        this.autoscrollTo = null;
        this.navigationHighlight = null;
        if (classInput == selectedClass) {
            return;
        }
        this.clearVisibleMethodPriorities();
        selectedClass = classInput;
        this.clearDelimiterMatch();
        this.importsExpanded = false;
        this.importAlpha.setValue(COLLAPSED_IMPORT_ALPHA);
        this.searchDirty = true;
        this.selectionMatchesDirty = true;
        if (classInput != null && !trinity.getDatabase().isLoading()) this.save();
        if (this.isFocusGained()) this.updateClassStructure();
    }

    @Override
    protected void onFocusGain() {
        this.focusTime.reset();
        Main.getDisplayManager().trackCurrentDecompilerView(
                this, this.navigationTarget, this.navigationInstruction);
        this.updateClassStructure();
    }

    public boolean isShowing(NavigationTarget target) {
        return target != null && this.selectedClass != null
                && target.getClassTarget() == this.selectedClass.getClassTarget();
    }

    public NavigationViewState captureNavigationViewState() {
        DecompilerCoordinates caret = this.cursor.coordinates;
        DecompilerCoordinates selection = this.cursor.selectionEnd;
        return new NavigationViewState(
                caret == null ? -1 : caret.getLine().getLineNumber() - 1,
                caret == null ? 0 : caret.getCharacter(),
                selection == null ? -1 : selection.getLine().getLineNumber() - 1,
                selection == null ? 0 : selection.getCharacter(),
                this.cursor.isSelectionUsingBoundaries(),
                this.lastDecompilerScrollX, this.lastDecompilerScrollY,
                this.importsExpanded);
    }

    public void restoreNavigationViewState(NavigationViewState viewState) {
        if (viewState == null) return;
        this.pendingNavigationViewState = viewState;
        this.autoscrollTo = null;
        this.navigationHighlight = null;
    }

    private void updateClassStructure() {
        this.updateClassStructure(false);
    }

    private void updateClassStructure(boolean force) {
        if (this.selectedClass != null) {
            ClassStructureWindow window = Main.getWindowManager()
                    .addStaticWindow(ClassStructureWindow.class);
            if (!force && window.getClassStructure() != null
                    && window.getClassStructure().getClassInput() == this.selectedClass) {
                return;
            }
            window.setClassStructure(new ClassStructure(this.selectedClass));
        }
    }

    public ClassInput getSelectedClass() {
        return selectedClass;
    }

    /** Restores a saved enum presentation before this window's first rendered decompilation. */
    public void restoreEnumPresentation(boolean treatEnumAsClass) {
        if (this.treatEnumAsClass == treatEnumAsClass) return;
        this.treatEnumAsClass = treatEnumAsClass;
        this.forceRefresh = true;
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    protected void renderFrame() {
        if (selectedClass != null) {
            this.drawDecompileTab();
        } else {
            ImGui.text("No class selected");
        }
        DecompiledClass decompiledClass = this.getDecompiledClass();
        boolean decompiling = trinity.getDecompiler().isDecompiling(selectedClass);
        boolean progressive = decompiledClass != null && decompiledClass.isProgressive();
        int completedMethods = decompiledClass == null
                ? 0 : decompiledClass.getProcessedMethodCount();
        int totalMethods = decompiledClass == null
                ? selectedClass == null ? 0 : selectedClass.getMethodMap().size()
                : decompiledClass.getTotalMethodCount();
        getMenuBar().setProgress(decompiling || progressive
                ? new MenuBarProgress("Decompiler",
                decompiling ? "Decompiling Methods" : "Rendering Methods",
                completedMethods, totalMethods)
                : null);
    }

    @Subscribe
    public void onClassModified(EventClassModified event) {
        if (event.getClassInput() == this.selectedClass) {
            this.forceRefreshDecompiler();
            this.updateClassStructure(true);
        }
    }

    @Subscribe
    public void onMemberModified(EventMemberModified event) {
        if (event.getClassInput() == this.selectedClass) {
            this.updateClassStructure(true);
        }
    }

    public void forceRefreshDecompiler() {
        this.forceRefresh = true;
    }

    private ClassInput decompilingInput;

    private void drawDecompileTab() {
        this.runControls();

        if (ImGui.isWindowFocused(ImGuiFocusedFlags.RootAndChildWindows) && ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(ImGuiKey.F)) {
            this.openSearch();
        }

        DecompiledClass decompiledClass = this.getDecompiledClass();
        if (decompiledClass != null && decompiledClass.applyPendingOutput()) {
            this.clearDelimiterMatch();
            this.searchDirty = true;
            this.selectionMatchesDirty = true;
            if (this.autoscrollTo != null) this.autoscrollTo.invalidate();
        }
        if (decompiledClass != null && trinity.getDecompiler().refreshRenderedText(decompiledClass)) {
            this.clearDelimiterMatch();
            this.searchDirty = true;
            this.selectionMatchesDirty = true;
        }

        if (this.searchVisible) {
            this.drawSearchBar(decompiledClass);
        } else {
            this.searchBarFocused = false;
        }

        ImGui.setCursorPosY(ImGui.getCursorPosY() - 3.F);
        if (ImGui.beginChild("DecompilerWindowChild", 0.F, 0.F, false,
                ImGuiWindowFlags.HorizontalScrollbar)) {
            EnumCardBounds enumCard = this.getEnumCardBounds();
            boolean enumCardHovered = enumCard != null && enumCard.isHovered();

            if (decompiledClass == null) {
                ImGui.textUnformatted("...");
            } else {
                FontSettings decompilerFont = Main.getPreferences().getDecompilerFont();
                decompilerFont.pushFont();
                this.drawDecompiledOutput(decompiledClass, enumCardHovered);
                decompilerFont.popFont();
            }
            if (enumCard != null) this.drawEnumPresentationCard(enumCard);
        }
        ImGui.endChild();
        this.handleNavigationKeyMappings();
    }

    private void runControls() {
        if (this.forceRefresh) {
            this.forceRefresh = false;
            this.clearDelimiterMatch();

            try {
                trinity.getDecompiler().decompile(selectedClass, this.treatEnumAsClass, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (trinity.getDecompiler().isDecompileFailed(selectedClass)) {
            ImGui.textColored(ImColor.rgb(245, 80, 80), "Decompilation failed");
        }
    }

    private EnumCardBounds getEnumCardBounds() {
        if (this.selectedClass == null
                || (this.selectedClass.getNode().access & Opcodes.ACC_ENUM) == 0) {
            return null;
        }

        float availableWidth = ImGui.getWindowWidth()
                - ENUM_CARD_MARGIN * 2.F - ImGui.getStyle().getScrollbarSize();
        if (availableWidth < 126.F) return null;

        float width = Math.min(ENUM_CARD_WIDTH, availableWidth);
        float right = ImGui.getWindowPosX() + ImGui.getWindowWidth()
                - ENUM_CARD_MARGIN - ImGui.getStyle().getScrollbarSize();
        float top = ImGui.getWindowPosY() + ENUM_CARD_MARGIN;
        return new EnumCardBounds(right - width, top, width, ENUM_CARD_HEIGHT);
    }

    private void drawEnumPresentationCard(EnumCardBounds bounds) {
        ImVec2 previousCursor = ImGui.getCursorScreenPos();
        ImDrawList drawList = ImGui.getWindowDrawList();
        drawList.addRectFilled(bounds.left() + 2.F, bounds.top() + 3.F,
                bounds.right() + 2.F, bounds.bottom() + 3.F,
                ImColor.rgba(0, 0, 0, 32));
        drawList.addRectFilled(bounds.left(), bounds.top(), bounds.right(), bounds.bottom(),
                CodeColorScheme.setAlpha(CodeColorScheme.POPUP_BACKGROUND, 135));
        drawList.addRect(bounds.left(), bounds.top(), bounds.right(), bounds.bottom(),
                CodeColorScheme.setAlpha(Main.getPreferences().getAccentColor().getColor(), 62),
                0.F, 0, 1.F);

        float horizontalPadding = 9.F;
        String label = this.treatEnumAsClass ? "View as enum" : "View as class";
        ImGui.setCursorScreenPos(bounds.left() + horizontalPadding, bounds.top() + 8.F);
        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, 0.68F);
        boolean toggle = ImGui.button(label + "###" + this.getId("ToggleEnumPresentation"),
                bounds.width() - horizontalPadding * 2.F, 0.F);
        ImGui.popStyleVar();
        GuiUtil.tooltip("Change how this enum is shown in this decompiler window.");
        ImGui.setCursorScreenPos(previousCursor);
        ImGui.dummy(0.F, 0.F);

        if (toggle) this.setTreatEnumAsClass(!this.treatEnumAsClass);
    }

    private void setTreatEnumAsClass(boolean treatEnumAsClass) {
        if (this.treatEnumAsClass == treatEnumAsClass) return;
        this.treatEnumAsClass = treatEnumAsClass;
        this.forceRefresh = true;
        this.searchDirty = true;
        this.selectionMatchesDirty = true;
        if (!this.trinity.getDatabase().isLoading()) this.save();
    }

    private void drawSearchBar(DecompiledClass decompiledClass) {
        if (ImGui.beginChild(this.getId("DecompilerSearch"), 0.F, 64.F, true)) {
            ImGui.textUnformatted("Search");
            ImGui.sameLine();
            ImGui.setNextItemWidth(Math.max(120.F, ImGui.getContentRegionAvailX() - 28.F));

            if (this.focusSearch) {
                ImGui.setKeyboardFocusHere();
                this.focusSearch = false;
            }

            int searchInputFlags = this.selectSearchText ? ImGuiInputTextFlags.AutoSelectAll : ImGuiInputTextFlags.None;
            this.selectSearchText = false;
            boolean searchChanged = ImGui.inputText("###" + this.getId("DecompilerSearchText"), this.searchText, searchInputFlags);

            ImGui.sameLine();
            if (ImGui.smallButton(FontAwesomeIcons.Times + "###" + this.getId("CloseDecompilerSearch"))) {
                this.closeSearch();
            }
            GuiUtil.tooltip("Close search (Esc)");

            boolean optionsChanged = GuiUtil.smallCheckbox("Case Sensitive###" + this.getId("DecompilerSearchCase"), this.searchCaseSensitive);
            ImGui.sameLine();
            optionsChanged |= GuiUtil.smallCheckbox("Words###" + this.getId("DecompilerSearchWords"), this.searchWords);
            GuiUtil.tooltip("Match whole Java identifier words");
            ImGui.sameLine();
            optionsChanged |= GuiUtil.smallCheckbox("Regex###" + this.getId("DecompilerSearchRegex"), this.searchRegex);

            if (searchChanged || optionsChanged) {
                this.searchDirty = true;
            }
            this.refreshSearchResults(decompiledClass);

            ImGui.sameLine();
            if (this.searchError == null) {
                ImGui.textDisabled(this.getSearchStatus());
            } else {
                ImGui.textColored(CodeColorScheme.NOTIFY_ERROR, "Invalid regex");
                GuiUtil.tooltip(this.searchError);
            }

            ImGui.sameLine();
            if (GuiUtil.disabledWidget(this.searchResults.isEmpty(), () -> ImGui.smallButton(FontAwesomeIcons.ChevronUp + "###" + this.getId("PreviousDecompilerSearch")))) {
                this.moveSearchResult(-1);
            }
            GuiUtil.tooltip("Previous match (Up or Enter)");
            ImGui.sameLine();
            if (GuiUtil.disabledWidget(this.searchResults.isEmpty(), () -> ImGui.smallButton(FontAwesomeIcons.ChevronDown + "###" + this.getId("NextDecompilerSearch")))) {
                this.moveSearchResult(1);
            }
            GuiUtil.tooltip("Next match (Down)");

            this.searchBarFocused = ImGui.isWindowFocused();
            boolean enterPressed = ImGui.isKeyPressed(ImGuiKey.Enter);
            if (this.searchBarFocused && (ImGui.isKeyPressed(ImGuiKey.UpArrow) || enterPressed)) {
                this.moveSearchResult(-1);
                if (enterPressed) this.focusSearch = true;
            } else if (this.searchBarFocused && ImGui.isKeyPressed(ImGuiKey.DownArrow)) {
                this.moveSearchResult(1);
            }
            if (this.searchBarFocused && ImGui.isKeyPressed(ImGuiKey.Escape)) {
                this.closeSearch();
            }
        }
        ImGui.endChild();
    }

    private String getSearchStatus() {
        if (this.searchResults.isEmpty()) {
            return this.searchText.get().isEmpty() ? "0 results" : "No matches";
        }
        return String.format("%d/%d", this.searchResultIndex + 1, this.searchResults.size());
    }

    private void refreshSearchResults(DecompiledClass decompiledClass) {
        if (!this.searchDirty && this.searchedClass == decompiledClass) {
            return;
        }

        this.searchDirty = false;
        this.searchedClass = decompiledClass;
        this.searchResults.clear();
        this.searchResultsByLine.clear();
        this.searchResultIndex = -1;
        this.searchError = null;

        String query = this.searchText.get();
        if (decompiledClass == null || query.isEmpty()) {
            this.cursor.selectionEnd = null;
            return;
        }

        int flags = this.searchCaseSensitive.get() ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        Pattern pattern;
        try {
            pattern = Pattern.compile(this.searchRegex.get() ? query : Pattern.quote(query), flags);
        } catch (PatternSyntaxException exception) {
            this.searchError = exception.getDescription();
            this.cursor.selectionEnd = null;
            return;
        }

        for (DecompilerLine line : decompiledClass.getLines()) {
            String text = line.getText();
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                if (start == end || this.searchWords.get() && !this.isWholeWord(text, start, end)) {
                    continue;
                }
                DecompilerSearchResult result = new DecompilerSearchResult(line, start, end);
                this.searchResults.add(result);
                this.searchResultsByLine.computeIfAbsent(line, ignored -> new ArrayList<>())
                        .add(result);
            }
        }

        if (this.searchResults.isEmpty()) {
            this.cursor.selectionEnd = null;
        } else {
            this.selectSearchResult(0);
        }
    }

    private boolean isWholeWord(String text, int start, int end) {
        return (start == 0 || !Character.isJavaIdentifierPart(text.charAt(start - 1))) &&
                (end == text.length() || !Character.isJavaIdentifierPart(text.charAt(end)));
    }

    private void moveSearchResult(int delta) {
        if (!this.searchResults.isEmpty()) {
            this.selectSearchResult(this.searchResultIndex + delta);
        }
    }

    private void selectSearchResult(int index) {
        this.searchResultIndex = Math.floorMod(index, this.searchResults.size());
        DecompilerSearchResult result = this.searchResults.get(this.searchResultIndex);
        if (this.isDecompilerLineHidden(result.line())) this.importsExpanded = true;
        this.cursor.selectRange(new DecompilerCoordinates(result.line(), result.start()),
                new DecompilerCoordinates(result.line(), result.end() - 1), false);
        this.cursor.setScrollToCursor();
    }

    public DecompiledClass getDecompiledClass() {
        return trinity.getDecompiler().getFromCache(selectedClass);
    }

    private void drawDecompiledOutput(DecompiledClass decompiledClass, boolean enumCardHovered) {
        this.applyPendingNavigationViewState(decompiledClass);
        if (this.restoreScrollFrames > 0) {
            ImGui.setScrollX(this.restoreScrollX);
            ImGui.setScrollY(this.restoreScrollY);
        }
        // Capture the live child viewport before input handling. Ctrl-click navigation can
        // leave this window later in the same frame.
        this.lastDecompilerScrollX = ImGui.getScrollX();
        this.lastDecompilerScrollY = ImGui.getScrollY();
        this.validateDelimiterMatch(decompiledClass);
        this.hoveredComponent = null;
        this.cursor.updateScrollAnimation();

        float mousePosY = ImGui.getMousePosY() + ImGui.getScrollY() - ImGui.getWindowPosY();
        float mousePosX = ImGui.getMousePosX();

        int lineNumberDigits = Math.max(MIN_LINE_NUMBER_DIGITS,
                String.valueOf(decompiledClass.getLines().size() + 1).length());
        ImVec2 textSize = ImGui.calcTextSize("0".repeat(lineNumberDigits));
        float lineNumberSpacing = 3.F + textSize.x;
        float cursorPosX = ImGui.getCursorPosX();
        boolean decompilerInputBlocked = enumCardHovered || this.blockStickyPreviewInput();
        DecompilerGhostTextRenderer.setInteractionBlocked(decompilerInputBlocked);

        if (!this.searchBarFocused && !decompilerInputBlocked) cursor.handleInputs(mousePosX, mousePosY);

        DecompilerAutoScroll pendingAutoScroll = this.autoscrollTo != null
                && this.autoscrollTo.isNavigationPending() ? this.autoscrollTo : null;
        DecompilerComponent autoScrollComponent = pendingAutoScroll == null
                ? null : pendingAutoScroll.findComponent(decompiledClass);

        List<DecompilerLine> lines = decompiledClass.getLines();
        DecompilerImportSection importSection = decompiledClass.getImportSection();
        boolean importsFoldable = importSection != null && importSection.isFoldable();
        boolean importsExpandedForFrame = this.importsExpanded;
        int visibleLineCount = importSection == null ? lines.size()
                : importSection.visibleLineCount(lines.size(), importsExpandedForFrame);
        float lineHeight = ImGui.getTextLineHeightWithSpacing();
        float contentStartY = ImGui.getCursorPosY();

        if (pendingAutoScroll != null && pendingAutoScroll.isNavigationPending()
                && autoScrollComponent != null) {
            DecompiledClass.ComponentLocation location = decompiledClass
                    .getComponentLocation(autoScrollComponent);
            if (location != null) {
                this.completeAutoScroll(pendingAutoScroll, decompiledClass,
                        location.line(), location.characterOffset());
            }
        }
        DecompilerCoordinates caret = this.cursor.getActiveCoordinates();
        if (caret != null) {
            int sourceIndex = caret.getLine().getLineNumber() - 1;
            int visibleRow = this.visibleRowForSourceIndex(
                    sourceIndex, importSection, importsExpandedForFrame);
            this.cursor.prepareScrollToVisibleRow(visibleRow, contentStartY, lineHeight,
                    visibleLineCount);
        }

        this.renderedVisibleRows.clear();
        this.renderedSourceLines = lines;
        this.renderedImportSection = importSection;
        this.renderedImportsExpanded = importsExpandedForFrame;
        this.viewportFirstVisibleRow = Math.max(0, Math.min(visibleLineCount,
                (int) Math.floor(Math.max(0.F, ImGui.getScrollY() - contentStartY) / lineHeight)));
        this.viewportEndVisibleRow = Math.max(this.viewportFirstVisibleRow,
                Math.min(visibleLineCount, (int) Math.ceil(
                        (ImGui.getScrollY() + ImGui.getWindowHeight() - contentStartY) / lineHeight) + 1));
        this.updateVisibleMethodPriorities(decompiledClass, importSection,
                importsExpandedForFrame, visibleLineCount);

        ImGuiListClipper clipper = new ImGuiListClipper();
        try {
            clipper.begin(visibleLineCount, lineHeight);
            while (clipper.step()) {
                int displayStart = Math.max(0, clipper.getDisplayStart());
                int displayEnd = Math.min(visibleLineCount, clipper.getDisplayEnd());
                this.renderedVisibleRows.set(displayStart, displayEnd);
                for (int visibleRow = displayStart; visibleRow < displayEnd; visibleRow++) {
                    int lineIndex = this.sourceIndexForVisibleRow(
                            visibleRow, importSection, importsExpandedForFrame);
                    DecompilerLine line = lines.get(lineIndex);
                    boolean firstImportLine = importsFoldable
                            && lineIndex == importSection.firstLineIndex();
                    final float cursorScreenPosX = ImGui.getCursorScreenPosX();
                    float currentLineNumberSpacing = this.getLineNumberSpacing(
                            line, lineNumberSpacing, firstImportLine);
                    boolean collapsedImportHovered = firstImportLine && !importsExpandedForFrame
                            && !decompilerInputBlocked && this.isCollapsedImportHovered(
                            line, cursorScreenPosX, currentLineNumberSpacing, textSize);
                    boolean collapsedImportTextHovered = firstImportLine && !importsExpandedForFrame
                            && !decompilerInputBlocked && this.isCollapsedImportTextHovered(
                            line, cursorScreenPosX, currentLineNumberSpacing, textSize);
                    if (collapsedImportHovered) ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
                    if (firstImportLine) {
                        this.importAlpha.run(importsExpandedForFrame || collapsedImportHovered
                                ? 1.F : COLLAPSED_IMPORT_ALPHA);
                        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, this.importAlpha.getValue());
                    }

                    ImGui.setCursorPosX(cursorPosX + currentLineNumberSpacing);
                    line.posY = ImGui.getCursorScreenPosY();
                    line.positioned = true;
                    this.drawNavigationHighlight(line, cursorScreenPosX, textSize);

                    boolean textPositioned = false;
                    for (DecompilerLineText text : line.getComponents()) {
                        boolean customRendered = text.getComponent().render();
                        if (!customRendered) {
                            if (!textPositioned) {
                                line.posY = ImGui.getCursorScreenPosY();
                                textPositioned = true;
                            }
                            text.render(decompiledClass.isComponentHighlighted(text.getComponent()));
                            ImGui.sameLine(0.F, 0.F);
                        } else {
                            text.captureRenderedBounds();
                            textPositioned = false;
                        }

                        if (!decompilerInputBlocked && !collapsedImportTextHovered
                                && this.hoveredComponent == null && ImGui.isItemHovered()) {
                            this.hoveredComponent = text.getComponent();
                        }
                    }

                    if (firstImportLine && !importsExpandedForFrame) {
                        this.drawCollapsedImportEllipsis(collapsedImportTextHovered);
                    }
                    if (firstImportLine) ImGui.popStyleVar();
                    if (collapsedImportTextHovered && ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
                        this.toggleImportSection(importSection, lines);
                    }

                    float cursorPosY = ImGui.getCursorPosY();
                    boolean disclosureHovered = this.drawLineGutter(line, cursorPosX,
                            currentLineNumberSpacing, firstImportLine, importSection, lines);
                    final boolean hovered = !decompilerInputBlocked && ImGui.isWindowHovered()
                            && !disclosureHovered && !collapsedImportHovered
                            && mousePosY >= cursorPosY && mousePosY < cursorPosY + lineHeight;

                    boolean memberNavigationClick = hoveredComponent != null
                            && hoveredComponent.getViewMember() != null
                            && ImGui.getIO().getKeyCtrl()
                            && ImGui.isMouseClicked(ImGuiMouseButton.Left)
                            && focusTime.hasPassed(150L) && viewMember.hasPassed(250L);
                    if (hovered && !memberNavigationClick) {
                        this.cursor.handleHoveredLineInputs(
                                cursorScreenPosX, currentLineNumberSpacing, mousePosX, line);
                    }

                    ImGui.sameLine(cursorPosX + currentLineNumberSpacing, 0.F);
                    this.cursor.handleLineDrawing(line, cursorScreenPosX,
                            currentLineNumberSpacing, mousePosX, cursorPosY, textSize);
                    ImGui.newLine();
                }
            }
        } finally {
            clipper.destroy();
        }
        ImGui.setCursorPosX(cursorPosX);
        ImGui.dummy(lineNumberSpacing + this.measureSourceWidth(decompiledClass) + 12.F, 0.F);

        if (pendingAutoScroll != null && pendingAutoScroll.isNavigationPending()
                && pendingAutoScroll.isFallbackToClass() && autoScrollComponent == null
                && !decompiledClass.isProgressive()) {
            DecompilerLine classLine = decompiledClass.getStickyHeaders().classLine();
            if (classLine == null && !decompiledClass.getLines().isEmpty()) {
                classLine = decompiledClass.getLines().get(0);
            }
            if (classLine != null) {
                this.completeAutoScroll(pendingAutoScroll, decompiledClass, classLine, 0);
            }
        }
        DecompilerGhostTextRenderer.setInteractionBlocked(false);

        this.drawSearchResults();
        this.cursor.drawSelectionBox();
        this.drawSelectionMatches(decompiledClass);
        this.drawDelimiterHighlights();
        boolean stickyHovered = this.drawStickyHeaders(
                decompiledClass, lineNumberSpacing, textSize, enumCardHovered);

        boolean rightClick = !stickyHovered && !enumCardHovered && ImGui.isWindowHovered()
                && ImGui.isMouseClicked(ImGuiMouseButton.Right);
        boolean leftClick = !stickyHovered && !enumCardHovered && !rightClick && ImGui.isWindowHovered()
                && ImGui.isMouseClicked(ImGuiMouseButton.Left);

        if (this.hoveredComponent != null) {
            List<ColoredString> tooltip = this.hoveredComponent.createTooltip();
            ClassInput previewClass = this.hoveredComponent.getPreviewClass();
            MethodInput previewMethod = this.hoveredComponent.getPreviewMethod();
            FieldInput previewField = this.hoveredComponent.getPreviewField();
            DecompilerComponent.VariablePreview previewVariable = this.hoveredComponent.getPreviewVariable();

            if (tooltip != null || previewClass != null || previewMethod != null
                    || previewField != null || previewVariable != null) {
                ImGui.beginTooltip();
                DecompilerPreviewRenderer previewRenderer = new DecompilerPreviewRenderer(trinity);

                if (tooltip != null) {
                    previewRenderer.drawDetails(tooltip);
                }
                if (previewClass != null) {
                    previewRenderer.drawClassPreview(previewClass, tooltip != null);
                } else if (previewMethod != null) {
                    previewRenderer.drawMethodPreview(previewMethod, tooltip != null);
                } else if (previewField != null) {
                    previewRenderer.drawFieldPreview(previewField, tooltip != null);
                } else if (previewVariable != null) {
                    previewRenderer.drawVariablePreview(decompiledClass, previewVariable, tooltip != null);
                }
                previewRenderer.finish();

                ImGui.endTooltip();
            }

            if (this.hoveredComponent.getViewMember() != null) {
                if (ImGui.getIO().getKeyCtrl()) {
                    ImGui.setMouseCursor(ImGuiMouseCursor.Hand);

                    if (focusTime.hasPassed(150L) && viewMember.hasPassed(250L) && (ImGui.isKeyPressed(ImGuiKey.B) || leftClick)) {
                        Main.getDisplayManager().followDecompilerView(
                                this.hoveredComponent.getViewMember(), NavigationAction.FOLLOW_MEMBER);
                        viewMember.reset();
                    }
                }
            }

            if (rightClick) {
                PopupItemBuilder popup = this.hoveredComponent.createPopup();

                if (!popup.isEmpty()) {
                    Main.getDisplayManager().showPopup(popup);
                    rightClick = false;
                }
            }
        }

        if (rightClick) {
            Main.getDisplayManager().showPopup(PopupItemBuilder.create().disabled(() -> !cursor.hasTextSelection(), items -> {
                items.menuItem("Copy", this::copyToClipboard);
            }));
        }

        if (cursor.hasTextSelection() && ImGui.isWindowFocused() && ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(ImGuiKey.C)) {
            this.copyToClipboard();
        }

        this.handleMemberKeyMappings();
        if (this.restoreScrollFrames > 0) {
            // Submit the full virtualized extent before restoring the viewport. New windows
            // do not have a useful scroll maximum until the end of their first frame.
            ImGui.setScrollX(this.restoreScrollX);
            ImGui.setScrollY(this.restoreScrollY);
            this.restoreScrollFrames--;
        }
        this.lastDecompilerScrollX = ImGui.getScrollX();
        this.lastDecompilerScrollY = ImGui.getScrollY();
    }

    private void applyPendingNavigationViewState(DecompiledClass decompiledClass) {
        NavigationViewState viewState = this.pendingNavigationViewState;
        if (viewState == null || decompiledClass.isProgressive()) return;

        List<DecompilerLine> lines = decompiledClass.getLines();
        DecompilerCoordinates caret = this.navigationCoordinates(
                lines, viewState.cursorLine(), viewState.cursorCharacter());
        DecompilerCoordinates selection = caret == null ? null : this.navigationCoordinates(
                lines, viewState.selectionLine(), viewState.selectionCharacter());
        this.cursor.restoreNavigationState(
                caret, selection, viewState.selectionUsesBoundaries());
        this.importsExpanded = viewState.importsExpanded();
        this.restoreScrollX = Math.max(0.F, viewState.scrollX());
        this.restoreScrollY = Math.max(0.F, viewState.scrollY());
        this.restoreScrollFrames = 2;
        this.pendingNavigationViewState = null;
        this.updateDelimiterMatch(this.cursor.getActiveCoordinates());
    }

    private DecompilerCoordinates navigationCoordinates(List<DecompilerLine> lines,
                                                        int lineIndex, int character) {
        if (lineIndex < 0 || lines.isEmpty()) return null;
        DecompilerLine line = lines.get(Math.min(lineIndex, lines.size() - 1));
        return new DecompilerCoordinates(line,
                Math.max(0, Math.min(character, line.getText().length())));
    }

    void updateDelimiterMatch(DecompilerCoordinates caret) {
        this.clearDelimiterMatch();
        if (caret == null || caret.getComponent() != null
                && caret.getComponent().getRenameState() != null) return;

        DecompiledClass decompiledClass = this.getDecompiledClass();
        if (decompiledClass == null) return;
        DecompilerDelimiterMatcher.Match match = DecompilerDelimiterMatcher.findMatchAtCaret(
                decompiledClass.getLines(), caret);
        if (match == null) return;

        this.delimiterMatch = match;
        this.delimiterMatchClass = decompiledClass;
    }

    void clearDelimiterMatch() {
        this.delimiterMatch = null;
        this.delimiterMatchClass = null;
    }

    private void validateDelimiterMatch(DecompiledClass decompiledClass) {
        if (this.delimiterMatch == null) return;
        List<DecompilerLine> lines = decompiledClass.getLines();
        DecompilerLine selectedLine = this.delimiterMatch.selected().getLine();
        DecompilerLine matchingLine = this.delimiterMatch.matching().getLine();
        int selectedIndex = selectedLine.getLineNumber() - 1;
        int matchingIndex = matchingLine.getLineNumber() - 1;
        if (this.delimiterMatchClass != decompiledClass
                || selectedIndex < 0 || selectedIndex >= lines.size()
                || matchingIndex < 0 || matchingIndex >= lines.size()
                || lines.get(selectedIndex) != selectedLine
                || lines.get(matchingIndex) != matchingLine) {
            this.clearDelimiterMatch();
        }
    }

    private void drawDelimiterHighlights() {
        if (this.delimiterMatch == null) return;
        this.drawDelimiterHighlight(this.delimiterMatch.selected());
        this.drawDelimiterHighlight(this.delimiterMatch.matching());
    }

    private void drawDelimiterHighlight(DecompilerCoordinates coordinates) {
        if (!this.isDecompilerLineRendered(coordinates.getLine())) return;
        int character = coordinates.getCharacter();
        DecompilerLine.TextRangeBounds bounds = coordinates.getLine()
                .getRenderedRange(character, character + 1);
        if (bounds == null) return;

        ImDrawList drawList = ImGui.getWindowDrawList();
        drawList.addRectFilled(bounds.minX() - 1.5F, bounds.minY() - 1.F,
                bounds.maxX() + 1.5F, bounds.maxY() + 1.F,
                CodeColorScheme.CURSOR_SELECTION);
        drawList.addRect(bounds.minX() - 1.5F, bounds.minY() - 1.F,
                bounds.maxX() + 1.5F, bounds.maxY() + 1.F,
                CodeColorScheme.setAlpha(CodeColorScheme.CURSOR, 220));
    }

    private void drawCollapsedImportEllipsis(boolean importLineHovered) {
        ImGui.textColored(importLineHovered ? CodeColorScheme.TEXT : CodeColorScheme.DISABLED,
                " ...");
        ImGui.sameLine(0.F, 0.F);
    }

    private boolean isCollapsedImportHovered(DecompilerLine line, float gutterScreenX,
                                             float lineNumberSpacing, ImVec2 textSize) {
        float lineNumberWidth = ImGui.calcTextSize(String.valueOf(line.getLineNumber())).x;
        float textScreenX = gutterScreenX + lineNumberSpacing;
        float right = textScreenX + ImGui.calcTextSize(line.getText() + " ...").x;
        float top = ImGui.getCursorScreenPosY();
        return ImGui.isWindowHovered()
                && ImGui.isMouseHoveringRect(gutterScreenX + lineNumberWidth + 2.F, top,
                right, top + textSize.y + ImGui.getStyle().getItemSpacingY());
    }

    private boolean isCollapsedImportTextHovered(DecompilerLine line, float gutterScreenX,
                                                 float lineNumberSpacing, ImVec2 textSize) {
        float textScreenX = gutterScreenX + lineNumberSpacing;
        float top = ImGui.getCursorScreenPosY();
        return ImGui.isWindowHovered()
                && ImGui.isMouseHoveringRect(textScreenX, top,
                textScreenX + ImGui.calcTextSize(line.getText() + " ...").x,
                top + textSize.y + ImGui.getStyle().getItemSpacingY());
    }

    private boolean drawImportDisclosure(float gutterScreenX, float gutterScreenY,
                                         float lineNumberWidth, float lineNumberSpacing,
                                         float lineHeight, DecompilerImportSection section,
                                         List<DecompilerLine> lines) {
        float centerX = gutterScreenX + lineNumberWidth + 7.F;
        float centerY = gutterScreenY + lineHeight * 0.5F;
        boolean hovered = ImGui.isWindowHovered()
                && ImGui.isMouseHoveringRect(centerX - 5.F, gutterScreenY,
                Math.min(centerX + 5.F, gutterScreenX + lineNumberSpacing),
                gutterScreenY + lineHeight);
        if (hovered) ImGui.setMouseCursor(ImGuiMouseCursor.Hand);

        int accent = Main.getPreferences().getAccentColor().getColor();
        int color = hovered ? CodeColorScheme.setAlpha(accent, 215)
                : CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 175);
        ImDrawList drawList = ImGui.getWindowDrawList();
        if (this.importsExpanded) {
            drawList.addTriangleFilled(centerX - 3.5F, centerY - 2.F,
                    centerX + 3.5F, centerY - 2.F, centerX, centerY + 3.F, color);
        } else {
            drawList.addTriangleFilled(centerX - 2.F, centerY - 3.5F,
                    centerX - 2.F, centerY + 3.5F, centerX + 3.F, centerY, color);
        }

        if (hovered && ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            this.toggleImportSection(section, lines);
        }
        return hovered;
    }

    private void toggleImportSection(DecompilerImportSection section, List<DecompilerLine> lines) {
        this.importsExpanded = !this.importsExpanded;
        if (!this.importsExpanded) {
            section.clearCollapsedRenderedBounds(lines);
            this.moveCursorOutOfCollapsedImports(section, lines);
        }
    }

    private void moveCursorOutOfCollapsedImports(DecompilerImportSection section,
                                                  List<DecompilerLine> lines) {
        DecompilerCoordinates coordinates = this.cursor.coordinates;
        int cursorLine = coordinates == null ? -1
                : coordinates.getLine().getLineNumber() - 1;
        int selectionLine = this.cursor.selectionEnd == null ? -1
                : this.cursor.selectionEnd.getLine().getLineNumber() - 1;
        boolean cursorHidden = section.isHiddenWhenCollapsed(cursorLine);
        boolean selectionHidden = section.isHiddenWhenCollapsed(selectionLine);
        if (!cursorHidden && !selectionHidden) return;

        this.cursor.selectionEnd = null;
        if (cursorHidden) {
            DecompilerLine firstImport = lines.get(section.firstLineIndex());
            this.cursor.navigateTo(new DecompilerCoordinates(
                    firstImport, firstImport.getText().length()));
        }
    }

    boolean isDecompilerLineHidden(DecompilerLine line) {
        if (this.importsExpanded) return false;
        DecompiledClass decompiledClass = this.getDecompiledClass();
        if (decompiledClass == null) return false;
        DecompilerImportSection section = decompiledClass.getImportSection();
        return section != null && section.isFoldable()
                && section.isHiddenWhenCollapsed(line.getLineNumber() - 1);
    }

    private int sourceIndexForVisibleRow(int visibleRow, DecompilerImportSection section,
                                         boolean importsExpanded) {
        return section == null ? visibleRow
                : section.sourceIndexForVisibleRow(visibleRow, importsExpanded);
    }

    private void updateVisibleMethodPriorities(DecompiledClass decompiledClass,
                                               DecompilerImportSection importSection,
                                               boolean importsExpanded,
                                               int visibleLineCount) {
        int firstSourceLine = -1;
        int lastSourceLine = -1;
        if (visibleLineCount > 0 && this.viewportFirstVisibleRow < visibleLineCount) {
            int firstVisibleRow = Math.min(
                    this.viewportFirstVisibleRow, visibleLineCount - 1);
            int lastVisibleRow = Math.min(visibleLineCount - 1,
                    Math.max(firstVisibleRow, this.viewportEndVisibleRow - 1));
            firstSourceLine = this.sourceIndexForVisibleRow(
                    firstVisibleRow, importSection, importsExpanded);
            lastSourceLine = this.sourceIndexForVisibleRow(
                    lastVisibleRow, importSection, importsExpanded);
        }

        long layoutVersion = decompiledClass.getLayoutVersion();
        if (this.prioritizedViewportClass != decompiledClass
                || this.prioritizedViewportLayoutVersion != layoutVersion
                || this.prioritizedViewportFirstLine != firstSourceLine
                || this.prioritizedViewportLastLine != lastSourceLine) {
            this.prioritizedViewportClass = decompiledClass;
            this.prioritizedViewportLayoutVersion = layoutVersion;
            this.prioritizedViewportFirstLine = firstSourceLine;
            this.prioritizedViewportLastLine = lastSourceLine;
            List<MethodInput> visibleMethods = firstSourceLine < 0
                    ? List.of()
                    : decompiledClass.getMethodsInLineRange(firstSourceLine, lastSourceLine);
            if (!visibleMethods.equals(this.prioritizedViewportMethods)) {
                // The old viewport must stop affecting the scheduler immediately. The new one is
                // published only after its method set remains stable for the dwell interval.
                trinity.getDecompiler().clearVisibleMethodPriorities(this.selectedClass);
                this.prioritizedViewportMethods = visibleMethods;
                this.prioritizedViewportDwellStartNanos = System.nanoTime();
                this.viewportPriorityPublished = false;
            }
        }

        if (!this.prioritizedViewportMethods.isEmpty()
                && (this.viewportPriorityPublished
                || System.nanoTime() - this.prioritizedViewportDwellStartNanos
                >= METHOD_PRIORITY_DWELL_NANOS)) {
            this.viewportPriorityPublished = true;
            // This also acts as a cheap heartbeat. The scheduler drops stale priorities when this
            // dock tab is no longer being rendered.
            trinity.getDecompiler().prioritizeVisibleMethods(
                    this.selectedClass, this.prioritizedViewportMethods);
        }
    }

    private void clearVisibleMethodPriorities() {
        if (this.selectedClass != null) {
            trinity.getDecompiler().clearVisibleMethodPriorities(this.selectedClass);
        }
        this.prioritizedViewportClass = null;
        this.prioritizedViewportLayoutVersion = -1L;
        this.prioritizedViewportFirstLine = -1;
        this.prioritizedViewportLastLine = -1;
        this.prioritizedViewportMethods = List.of();
        this.prioritizedViewportDwellStartNanos = 0L;
        this.viewportPriorityPublished = false;
    }

    private int visibleRowForSourceIndex(int sourceIndex, DecompilerImportSection section,
                                         boolean importsExpanded) {
        return section == null ? sourceIndex
                : section.visibleRowForSourceIndex(sourceIndex, importsExpanded);
    }

    boolean isDecompilerLineRendered(DecompilerLine line) {
        if (line == null || this.renderedSourceLines.isEmpty()) return false;
        int sourceIndex = line.getLineNumber() - 1;
        if (sourceIndex < 0 || sourceIndex >= this.renderedSourceLines.size()
                || this.renderedSourceLines.get(sourceIndex) != line
                || this.renderedImportSection != null && !this.renderedImportsExpanded
                && this.renderedImportSection.isHiddenWhenCollapsed(sourceIndex)) {
            return false;
        }
        int visibleRow = this.visibleRowForSourceIndex(sourceIndex,
                this.renderedImportSection, this.renderedImportsExpanded);
        return this.renderedVisibleRows.get(visibleRow);
    }

    int getFirstRenderedVisibleRow() {
        return this.renderedVisibleRows.nextSetBit(0);
    }

    int getNextRenderedVisibleRow(int fromIndex) {
        return this.renderedVisibleRows.nextSetBit(fromIndex);
    }

    int getSourceIndexForRenderedVisibleRow(int visibleRow) {
        return this.sourceIndexForVisibleRow(visibleRow,
                this.renderedImportSection, this.renderedImportsExpanded);
    }

    private float measureSourceWidth(DecompiledClass decompiledClass) {
        float fontSize = ImGui.getFontSize();
        long layoutVersion = decompiledClass.getLayoutVersion();
        if (this.measuredWidthClass != decompiledClass
                || this.measuredWidthLayoutVersion != layoutVersion
                || this.measuredWidthFontSize != fontSize) {
            this.measuredWidthClass = decompiledClass;
            this.measuredWidthLayoutVersion = layoutVersion;
            this.measuredWidthFontSize = fontSize;
            this.measuredSourceWidth = ImGui.calcTextSize(
                    decompiledClass.getLongestLineText()).x;
        }
        return this.measuredSourceWidth;
    }

    private void completeAutoScroll(DecompilerAutoScroll autoScroll, DecompiledClass decompiledClass,
                                    DecompilerLine line, int textOffset) {
        this.cursor.navigateTo(new DecompilerCoordinates(line, textOffset));
        this.navigationHighlight = new DecompilerHighlight(line);
        boolean fallbackToClass = autoScroll.isFallbackToClass();
        Input<?> requestedInput = autoScroll.getInput();
        autoScroll.markNavigated();
        if (fallbackToClass) this.showUnavailableMemberNotification(requestedInput);
        if (!decompiledClass.isProgressive() && this.autoscrollTo == autoScroll) {
            this.autoscrollTo = null;
        }
    }

    private void showUnavailableMemberNotification(Input<?> requestedInput) {
        if (!(requestedInput instanceof MemberInput<?> member)) return;

        String kind = member instanceof MethodInput ? "Method" : "Field";
        Notification notification = new Notification(NotificationType.WARNING,
                new SimpleCaption("Member Unavailable"), ColoredStringBuilder.create()
                .fmt(kind + " {} isn't available in the decompiler output.%nShowing class {} instead.",
                        member.getDisplayName().getName(),
                        member.getOwningClass().getDisplaySimpleName()).get());
        notification.setExpireTime(5_000L);
        Main.getDisplayManager().addNotification(notification);
    }

    private float getLineNumberSpacing(DecompilerLine line, float defaultSpacing,
                                       boolean importDisclosure) {
        float lineNumberWidth = ImGui.calcTextSize(String.valueOf(line.getLineNumber())).x;
        float spacing = importDisclosure
                ? Math.max(defaultSpacing, lineNumberWidth + 14.F)
                : defaultSpacing;
        if (line.getRecursiveInvocation() == null) return spacing;

        FontSettings decompilerFont = Main.getPreferences().getDecompilerFont();
        float iconSize = Math.max(8.F, decompilerFont.getSize() * 0.65F);
        ImGui.pushFont(decompilerFont.getIconFont(), iconSize);
        float iconWidth = ImGui.calcTextSize(FontAwesomeIcons.RedoAlt).x;
        ImGui.popFont();
        return Math.max(spacing, lineNumberWidth + iconWidth + 7.F);
    }

    private boolean drawLineGutter(DecompilerLine line, float cursorPosX,
                                   float lineNumberSpacing, boolean importDisclosure,
                                   DecompilerImportSection section, List<DecompilerLine> lines) {
        ImGui.setCursorPosX(cursorPosX);
        float gutterScreenX = ImGui.getCursorScreenPosX();
        float gutterScreenY = ImGui.getCursorScreenPosY();
        String lineNumber = String.valueOf(line.getLineNumber());
        float lineNumberWidth = ImGui.calcTextSize(lineNumber).x;
        ImGui.textColored(CodeColorScheme.LINE_NUMBER, lineNumber);
        boolean disclosureHovered = importDisclosure && this.drawImportDisclosure(
                gutterScreenX, gutterScreenY, lineNumberWidth, lineNumberSpacing,
                ImGui.getItemRectSize().y, section, lines);
        DecompilerComponent recursiveInvocation = line.getRecursiveInvocation();
        if (recursiveInvocation == null) return disclosureHovered;

        FontSettings decompilerFont = Main.getPreferences().getDecompilerFont();
        float iconSize = Math.max(8.F, decompilerFont.getSize() * 0.65F);
        ImGui.pushFont(decompilerFont.getIconFont(), iconSize);
        ImVec2 iconBounds = ImGui.calcTextSize(FontAwesomeIcons.RedoAlt);
        ImGui.popFont();
        float iconScreenX = gutterScreenX + lineNumberWidth + 4.F;
        float iconScreenY = gutterScreenY + 3.F;
        ImGui.getWindowDrawList().addText(decompilerFont.getIconFont(), Math.round(iconSize),
                iconScreenX, iconScreenY, Main.getPreferences().getAccentColor().getColor(),
                FontAwesomeIcons.RedoAlt);
        boolean iconHovered = ImGui.isWindowHovered()
                && ImGui.isMouseHoveringRect(iconScreenX, iconScreenY,
                iconScreenX + iconBounds.x, iconScreenY + iconBounds.y);
        if (iconHovered) this.drawRecursiveInvocationTooltip(recursiveInvocation);
        return disclosureHovered || iconHovered;
    }

    private void drawRecursiveInvocationTooltip(DecompilerComponent component) {
        ImGui.beginTooltip();
        ImGui.textColored(Main.getPreferences().getAccentColor().getColor(),
                FontAwesomeIcons.RedoAlt + " Recursive method invocation");
        ImGui.endTooltip();
    }

    private void handleMemberKeyMappings() {
        if (!ImGui.isWindowFocused(ImGuiFocusedFlags.RootAndChildWindows) || ImGui.isAnyItemActive()) return;

        DecompilerComponent target = this.hoveredComponent != null
                ? this.hoveredComponent : this.cursor.getComponent();
        if (target == null) return;

        HoveredInputKeyBindings.offerFocused(() -> this.dispatchMemberKeyMapping(target));
    }

    private void dispatchMemberKeyMapping(DecompilerComponent target) {
        KeyBindManager bindings = Main.getKeyBindManager();
        Input<?> input = target.getActionInput();
        if (bindings.DECOMPILER_ASSEMBLE.isPressed()) {
            if (input instanceof MethodInput method) method.openAssembler();
        } else if (bindings.DECOMPILER_RENAME.isPressed()) {
            if (target.getRenameHandler() != null) target.beginRenaming();
        } else if (bindings.DECOMPILER_EDIT.isPressed()) {
            if (input instanceof ClassInput || input instanceof MethodInput || input instanceof FieldInput) {
                BytecodeEditorLauncher.edit(input);
            }
        } else if (bindings.DECOMPILER_VIEW_XREFS.isPressed()) {
            if (target.getViewXrefs() != null) {
                target.getViewXrefs().run();
            } else if (target.getXrefBuilderProvider() != null) {
                target.getXrefBuilderProvider().viewXrefs(trinity);
            } else if (target.getSearchAllOccurrences() != null) {
                target.getSearchAllOccurrences().run();
            }
        } else if (bindings.DECOMPILER_VIEW_MEMBER.isPressed()) {
            if (input != null) {
                Main.getDisplayManager().followDecompilerView(input, NavigationAction.FOLLOW_MEMBER);
            }
        }
    }

    private void handleNavigationKeyMappings() {
        if (!ImGui.isWindowFocused(ImGuiFocusedFlags.RootAndChildWindows) || ImGui.isAnyItemActive()) return;
        KeyBindManager bindings = Main.getKeyBindManager();
        if (bindings.DECOMPILER_NAVIGATE_BACK.isPressed()) {
            Main.getDisplayManager().navigateBack();
        } else if (bindings.DECOMPILER_NAVIGATE_FORWARD.isPressed()) {
            Main.getDisplayManager().navigateForward();
        }
    }

    private boolean blockStickyPreviewInput() {
        if (this.stickyHeaderHeight <= 0.F && this.stickyFooterHeight <= 0.F) return false;
        float left = ImGui.getWindowPosX();
        float top = ImGui.getWindowPosY();
        float right = left + ImGui.getWindowWidth();
        float bottom = top + ImGui.getWindowHeight();
        return ImGui.isWindowHovered()
                && (ImGui.isMouseHoveringRect(left, top, right, top + this.stickyHeaderHeight)
                || ImGui.isMouseHoveringRect(left, bottom - this.stickyFooterHeight,
                right, bottom));
    }

    private boolean drawStickyHeaders(DecompiledClass decompiledClass, float lineNumberSpacing,
                                      ImVec2 textSize, boolean enumCardHovered) {
        DecompiledClass.StickyHeaders stickyHeaders = decompiledClass.getStickyHeaders();
        float visibleTop = ImGui.getWindowPosY();
        float visibleBottom = visibleTop + ImGui.getWindowHeight();
        float lineHeight = ImGui.getTextLineHeightWithSpacing();
        List<DecompilerLine> visibleHeaders = new ArrayList<>(3);

        DecompilerLine classLine = stickyHeaders.classLine();
        int classVisibleRow = stickyHeaders.classLineIndex() < 0 ? -1
                : this.visibleRowForSourceIndex(stickyHeaders.classLineIndex(),
                this.renderedImportSection, this.renderedImportsExpanded);
        boolean showClass = classLine != null && classVisibleRow < this.viewportFirstVisibleRow;
        int methodAnchorRow = Math.min(this.viewportEndVisibleRow,
                this.viewportFirstVisibleRow + (showClass ? 1 : 0));
        int methodAnchorSource = this.sourceIndexForVisibleRow(methodAnchorRow,
                this.renderedImportSection, this.renderedImportsExpanded);
        DecompiledClass.StickyMethod currentMethod = this.findStickyMethod(
                stickyHeaders.methods(), methodAnchorSource);
        if (showClass) visibleHeaders.add(classLine);
        if (currentMethod != null) visibleHeaders.add(currentMethod.signatureLine());
        DecompilerLine methodLine = currentMethod == null ? null : currentMethod.signatureLine();

        float sourceVisibleTop = visibleTop + visibleHeaders.size() * lineHeight;
        OffscreenDelimiterPreview delimiterPreview = this.getOffscreenDelimiterPreview(
                sourceVisibleTop, visibleBottom);
        DecompilerCoordinates topDelimiter = delimiterPreview != null && !delimiterPreview.below()
                ? delimiterPreview.coordinates() : null;
        DecompilerCoordinates bottomDelimiter = delimiterPreview != null && delimiterPreview.below()
                ? delimiterPreview.coordinates() : null;
        if (topDelimiter != null && !visibleHeaders.contains(topDelimiter.getLine())) {
            visibleHeaders.add(topDelimiter.getLine());
        }

        float left = ImGui.getWindowPosX();
        float right = left + ImGui.getWindowWidth();
        float bottom = visibleTop + lineHeight * visibleHeaders.size();
        this.stickyHeaderHeight = bottom - visibleTop;
        ImDrawList drawList = ImGui.getWindowDrawList();
        if (!visibleHeaders.isEmpty()) {
            drawList.addRectFilled(left, visibleTop, right, bottom,
                    ImGui.getColorU32(ImGuiCol.WindowBg));
        }

        float rowTop = visibleTop;
        boolean classHovered = false;
        boolean methodHovered = false;
        boolean delimiterHovered = false;
        boolean dedicatedDelimiterRow = false;
        boolean windowHovered = ImGui.isWindowHovered() && !enumCardHovered;
        for (DecompilerLine line : visibleHeaders) {
            boolean delimiterTarget = topDelimiter != null
                    && line == topDelimiter.getLine();
            boolean dedicatedDelimiter = delimiterTarget
                    && line != classLine && line != methodLine;
            dedicatedDelimiterRow |= dedicatedDelimiter;
            boolean rowHovered = windowHovered
                    && ImGui.isMouseHoveringRect(left, rowTop, right, rowTop + lineHeight);
            Animation hoverAnimation = line == classLine ? this.stickyClassHover
                    : line == methodLine ? this.stickyMethodHover : this.stickyDelimiterHover;
            hoverAnimation.run(rowHovered ? STICKY_HOVER_ALPHA : 0.F);
            if (hoverAnimation.getValue() > 0.F) {
                drawList.addRectFilled(left, rowTop, right, rowTop + lineHeight,
                        CodeColorScheme.setAlpha(CodeColorScheme.TEXT,
                                Math.round(hoverAnimation.getValue())));
            }
            if (rowHovered) {
                ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
                if (ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
                    this.cursor.navigateTo(delimiterTarget ? topDelimiter
                            : new DecompilerCoordinates(line, line.getText().length()));
                }
            }
            if (line == classLine) classHovered = rowHovered;
            else if (line == methodLine) methodHovered = rowHovered;
            else delimiterHovered = rowHovered;
            rowTop += lineHeight;
        }
        if (!showClass) this.stickyClassHover.run(0.F);
        if (currentMethod == null) this.stickyMethodHover.run(0.F);
        if (!dedicatedDelimiterRow && bottomDelimiter == null) {
            this.stickyDelimiterHover.run(0.F);
        }

        if (!visibleHeaders.isEmpty()) {
            drawList.addLine(left, bottom, right, bottom, ImGui.getColorU32(ImGuiCol.Border));
        }

        float lineNumberX = left + ImGui.getStyle().getWindowPaddingX() - ImGui.getScrollX();
        float textX = lineNumberX + lineNumberSpacing;
        float textY = visibleTop + Math.max(0.F, (lineHeight - textSize.y) * 0.5F);
        if (!visibleHeaders.isEmpty()) {
            drawList.pushClipRect(left, visibleTop, right, bottom, true);
            for (DecompilerLine line : visibleHeaders) {
                drawList.addText(lineNumberX, textY, CodeColorScheme.LINE_NUMBER,
                        String.valueOf(line.getLineNumber()));
                int highlightedCharacter = topDelimiter != null
                        && line == topDelimiter.getLine()
                        ? topDelimiter.getCharacter() : -1;
                drawStickyLine(drawList, line, textX, textY, highlightedCharacter, textSize.y);
                textY += lineHeight;
            }
            drawList.popClipRect();
        }

        boolean bottomHovered = this.drawBottomDelimiterPreview(bottomDelimiter, drawList,
                left, right, visibleBottom, lineHeight, lineNumberSpacing, textSize,
                windowHovered);
        boolean hovered = classHovered || methodHovered || delimiterHovered || bottomHovered;
        if (hovered) {
            this.hoveredComponent = null;
        }
        return hovered;
    }

    private boolean drawBottomDelimiterPreview(DecompilerCoordinates preview,
                                               ImDrawList drawList, float left, float right,
                                               float visibleBottom, float lineHeight,
                                               float lineNumberSpacing, ImVec2 textSize,
                                               boolean windowHovered) {
        if (preview == null) {
            this.stickyFooterHeight = 0.F;
            return false;
        }

        this.stickyFooterHeight = lineHeight;
        float top = visibleBottom - lineHeight;
        boolean hovered = windowHovered
                && ImGui.isMouseHoveringRect(left, top, right, visibleBottom);
        this.stickyDelimiterHover.run(hovered ? STICKY_HOVER_ALPHA : 0.F);
        drawList.addRectFilled(left, top, right, visibleBottom,
                ImGui.getColorU32(ImGuiCol.WindowBg));
        if (this.stickyDelimiterHover.getValue() > 0.F) {
            drawList.addRectFilled(left, top, right, visibleBottom,
                    CodeColorScheme.setAlpha(CodeColorScheme.TEXT,
                            Math.round(this.stickyDelimiterHover.getValue())));
        }
        drawList.addLine(left, top, right, top, ImGui.getColorU32(ImGuiCol.Border));

        float lineNumberX = left + ImGui.getStyle().getWindowPaddingX() - ImGui.getScrollX();
        float textX = lineNumberX + lineNumberSpacing;
        float textY = top + Math.max(0.F, (lineHeight - textSize.y) * 0.5F);
        drawList.pushClipRect(left, top, right, visibleBottom, true);
        drawList.addText(lineNumberX, textY, CodeColorScheme.LINE_NUMBER,
                String.valueOf(preview.getLine().getLineNumber()));
        drawStickyLine(drawList, preview.getLine(), textX, textY,
                preview.getCharacter(), textSize.y);
        drawList.popClipRect();

        if (hovered) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
            if (ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
                this.cursor.navigateTo(preview);
            }
        }
        return hovered;
    }

    private OffscreenDelimiterPreview getOffscreenDelimiterPreview(float visibleTop,
                                                                   float visibleBottom) {
        if (this.delimiterMatch == null) return null;
        DecompilerCoordinates matching = this.delimiterMatch.matching();
        int sourceIndex = matching.getLine().getLineNumber() - 1;
        int visibleRow = this.visibleRowForSourceIndex(sourceIndex,
                this.renderedImportSection, this.renderedImportsExpanded);
        if (visibleRow >= this.viewportFirstVisibleRow
                && visibleRow < this.viewportEndVisibleRow) return null;
        return new OffscreenDelimiterPreview(matching,
                visibleRow >= this.viewportEndVisibleRow);
    }

    private record OffscreenDelimiterPreview(DecompilerCoordinates coordinates, boolean below) {
    }

    private DecompiledClass.StickyMethod findStickyMethod(
            List<DecompiledClass.StickyMethod> methods, int sourceLineIndex) {
        int low = 0;
        int high = methods.size() - 1;
        DecompiledClass.StickyMethod candidate = null;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            DecompiledClass.StickyMethod method = methods.get(middle);
            if (method.signatureLineIndex() < sourceLineIndex) {
                candidate = method;
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return candidate != null && candidate.endLineIndex() >= sourceLineIndex
                ? candidate : null;
    }

    private static void drawStickyLine(ImDrawList drawList, DecompilerLine line, float startX,
                                       float y, int highlightedCharacter, float textHeight) {
        String lineText = line.getText();
        if (highlightedCharacter >= 0 && highlightedCharacter < lineText.length()) {
            float characterX = startX + ImGui.calcTextSize(
                    lineText.substring(0, highlightedCharacter)).x;
            float characterWidth = ImGui.calcTextSize(
                    lineText.substring(highlightedCharacter, highlightedCharacter + 1)).x;
            drawList.addRectFilled(characterX - 1.5F, y - 1.F,
                    characterX + characterWidth + 1.5F, y + textHeight + 1.F,
                    CodeColorScheme.CURSOR_SELECTION);
            drawList.addRect(characterX - 1.5F, y - 1.F,
                    characterX + characterWidth + 1.5F, y + textHeight + 1.F,
                    CodeColorScheme.setAlpha(CodeColorScheme.CURSOR, 220));
        }

        float x = startX;
        for (DecompilerLineText text : line.getComponents()) {
            String value = text.getText();
            if (value.isEmpty()) {
                continue;
            }
            drawList.addText(x, y, text.getComponent().getColor(), value);
            x += ImGui.calcTextSize(value).x;
        }
    }

    private void drawNavigationHighlight(DecompilerLine line, float startX, ImVec2 textSize) {
        DecompilerHighlight highlight = this.navigationHighlight;
        if (highlight == null || highlight.getLine() != line) {
            return;
        }
        if (highlight.isFinished()) {
            this.navigationHighlight = null;
            return;
        }
        if (!line.positioned) {
            return;
        }

        float startY = line.posY - 2.F;
        float endX = ImGui.getWindowPosX() + ImGui.getWindowContentRegionMax().x;
        float endY = startY + textSize.y + 4.F;
        ImGui.getWindowDrawList().addRectFilled(startX, startY, endX, endY, highlight.getFillColor());
        ImGui.getWindowDrawList().addRect(startX, startY, endX, endY, highlight.getBorderColor());
    }

    private void drawSearchResults() {
        if (!this.searchVisible) {
            return;
        }

        for (int row = this.getFirstRenderedVisibleRow(); row >= 0;
             row = this.getNextRenderedVisibleRow(row + 1)) {
            DecompilerLine line = this.renderedSourceLines.get(
                    this.getSourceIndexForRenderedVisibleRow(row));
            for (DecompilerSearchResult result : this.searchResultsByLine
                    .getOrDefault(line, List.of())) {
                DecompilerLine.TextRangeBounds bounds = line.getRenderedRange(
                        result.start(), result.end());
                if (bounds != null) {
                    ImGui.getWindowDrawList().addRectFilled(bounds.minX() - 1.F,
                            bounds.minY() - 1.F, bounds.maxX() + 1.F, bounds.maxY() + 1.F,
                            CodeColorScheme.SEARCH_RESULT);
                }
            }
        }
    }

    private void drawSelectionMatches(DecompiledClass decompiledClass) {
        this.refreshSelectionMatches(decompiledClass);
        for (int row = this.getFirstRenderedVisibleRow(); row >= 0;
             row = this.getNextRenderedVisibleRow(row + 1)) {
            DecompilerLine line = this.renderedSourceLines.get(
                    this.getSourceIndexForRenderedVisibleRow(row));
            for (DecompilerSearchResult result : this.selectionMatchesByLine
                    .getOrDefault(line, List.of())) {
                DecompilerLine.TextRangeBounds bounds = line.getRenderedRange(
                        result.start(), result.end());
                if (bounds != null) {
                    ImGui.getWindowDrawList().addRect(bounds.minX() - 1.F,
                            bounds.minY() - 1.F, bounds.maxX() + 1.F, bounds.maxY() + 1.F,
                            SELECTION_MATCH_BORDER, 0.F, 0, 1.F);
                }
            }
        }
    }

    private void refreshSelectionMatches(DecompiledClass decompiledClass) {
        String selectedText = this.cursor.getSingleLineSelectionTextForHighlight();
        if (!this.selectionMatchesDirty && this.selectionMatchesClass == decompiledClass
                && this.selectionMatchText.equals(selectedText)) {
            return;
        }

        this.selectionMatchesDirty = false;
        this.selectionMatchesClass = decompiledClass;
        this.selectionMatchText = selectedText;
        this.selectionMatches.clear();
        this.selectionMatchesByLine.clear();
        if (selectedText.isBlank() || selectedText.indexOf('\n') >= 0 || selectedText.indexOf('\r') >= 0) {
            return;
        }

        for (DecompilerLine line : decompiledClass.getLines()) {
            String text = line.getText();
            int start = 0;
            while ((start = text.indexOf(selectedText, start)) != -1) {
                int end = start + selectedText.length();
                DecompilerSearchResult result = new DecompilerSearchResult(line, start, end);
                this.selectionMatches.add(result);
                this.selectionMatchesByLine.computeIfAbsent(line, ignored -> new ArrayList<>())
                        .add(result);
                start = end;
            }
        }
    }

    public void setDecompileTarget(Input<?> input) {
        this.setDecompileTarget(input, null);
    }

    public void setDecompileTarget(Input<?> input, AbstractInsnNode instruction) {
        if (input instanceof ClassInput classInput && instruction == null) {
            this.setDecompileTarget(classInput);
            return;
        }
        this.setDecompileTarget(input.getOwningClass());
        this.navigationTarget = input;
        this.navigationInstruction = instruction;
        this.autoscrollTo = new DecompilerAutoScroll(input, instruction);
    }

    public void setDecompileVariableTarget(MethodInput methodInput, int variableIndex,
                                           int componentOccurrence) {
        this.setDecompileTarget(methodInput.getOwningClass());
        this.navigationTarget = methodInput;
        this.navigationInstruction = null;
        this.autoscrollTo = DecompilerAutoScroll.forVariable(
                methodInput, variableIndex, componentOccurrence);
    }

    public void setDecompileVariableDeclarationTarget(MethodInput methodInput, int variableIndex) {
        this.setDecompileTarget(methodInput.getOwningClass());
        this.navigationTarget = methodInput;
        this.navigationInstruction = null;
        this.autoscrollTo = DecompilerAutoScroll.forVariableDeclaration(methodInput, variableIndex);
    }

    @Override
    public DatabaseDecompiler createDatabaseObject() {
        return new DatabaseDecompiler(this.selectedClass.getRealName(),
                DatabaseDecompiler.createFlags(this.treatEnumAsClass));
    }

    private record EnumCardBounds(float left, float top, float width, float height) {
        float right() {
            return this.left + this.width;
        }

        float bottom() {
            return this.top + this.height;
        }

        boolean isHovered() {
            return ImGui.isWindowHovered(ImGuiHoveredFlags.RootAndChildWindows)
                    && ImGui.isMouseHoveringRect(this.left, this.top, this.right(), this.bottom());
        }
    }

    private record DecompilerSearchResult(DecompilerLine line, int start, int end) {
    }
}
