package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import com.google.common.eventbus.Subscribe;
import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
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
import java.util.List;
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
    private DecompiledClass searchedClass;
    private String searchError;
    private int searchResultIndex = -1;
    private boolean searchVisible;
    private boolean focusSearch;
    private boolean selectSearchText;
    private boolean searchDirty = true;
    private boolean searchBarFocused;
    private float stickyHeaderHeight;
    private final Animation stickyClassHover = new Animation(Easing.EASE_OUT_QUAD, 110L);
    private final Animation stickyMethodHover = new Animation(Easing.EASE_OUT_QUAD, 110L);
    private final Animation stickyDelimiterHover = new Animation(Easing.EASE_OUT_QUAD, 110L);
    private final Animation importAlpha = new Animation(
            Easing.EASE_OUT_QUAD, IMPORT_ALPHA_ANIMATION_TIME, COLLAPSED_IMPORT_ALPHA);
    private final List<DecompilerSearchResult> selectionMatches = new ArrayList<>();
    private DecompiledClass selectionMatchesClass;
    private String selectionMatchText = "";
    private boolean selectionMatchesDirty = true;
    /**
     * Selection cursor.
     */
    public final DecompilerCursor cursor = new DecompilerCursor(this);
    private DecompilerAutoScroll autoscrollTo;
    private DecompilerHighlight navigationHighlight;
    private DecompilerDelimiterMatcher.Match delimiterMatch;
    private DecompiledClass delimiterMatchClass;
    private final Stopwatch focusTime = new Stopwatch();
    private static Stopwatch viewMember = new Stopwatch();
    private int currentDockId;

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
        this.selectedClass = null;
        this.navigationTarget = null;
        this.navigationInstruction = null;
        this.searchedClass = null;
        this.selectionMatchesClass = null;
        this.clearDelimiterMatch();
        this.searchResults.clear();
        this.selectionMatches.clear();
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
        this.navigationTarget = classInput;
        this.navigationInstruction = null;
        this.autoscrollTo = null;
        this.navigationHighlight = null;
        if (classInput == selectedClass) {
            return;
        }
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
        Main.getDisplayManager().trackCurrentDecompilerView(this.navigationTarget, this.navigationInstruction);
        this.updateClassStructure();
    }

    private void updateClassStructure() {
        if (this.selectedClass != null) {
            Main.getWindowManager().addStaticWindow(ClassStructureWindow.class).setClassStructure(new ClassStructure(this.selectedClass));
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
    protected boolean beginWindow() {
        boolean visible = super.beginWindow();
        // Begin() establishes the dock node even for an inactive tab. Recording it here makes
        // the assembler anchor independent of which decompiler tab currently renders content.
        this.currentDockId = ImGui.getWindowDockID();
        return visible;
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
        getMenuBar().setProgress(decompiling || progressive
                ? new MenuBarProgress("Decompiler", decompiling ? "Decompiling Methods" : "Rendering Methods", -1)
                : null);
    }

    public int getCurrentDockId() {
        return currentDockId;
    }

    @Subscribe
    public void onClassModified(EventClassModified event) {
        if (event.getClassInput() == this.selectedClass) {
            this.forceRefreshDecompiler();
            this.updateClassStructure();
        }
    }

    @Subscribe
    public void onMemberModified(EventMemberModified event) {
        if (event.getClassInput() == this.selectedClass) {
            this.updateClassStructure();
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
                this.searchResults.add(new DecompilerSearchResult(line, start, end));
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

    boolean hasActiveRename() {
        DecompiledClass decompiledClass = this.getDecompiledClass();
        if (decompiledClass == null) return false;

        for (DecompilerLine line : decompiledClass.getLines()) {
            for (DecompilerLineText text : line.getComponents()) {
                if (text.getComponent().getRenameState() != null) return true;
            }
        }
        return false;
    }

    private void drawDecompiledOutput(DecompiledClass decompiledClass, boolean enumCardHovered) {
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
        boolean decompilerInputBlocked = enumCardHovered || this.blockStickyHeaderInput();
        DecompilerGhostTextRenderer.setInteractionBlocked(decompilerInputBlocked);

        if (!this.searchBarFocused && !decompilerInputBlocked) cursor.handleInputs(mousePosX, mousePosY);

        DecompilerAutoScroll pendingAutoScroll = this.autoscrollTo != null
                && this.autoscrollTo.isNavigationPending() ? this.autoscrollTo : null;
        DecompilerComponent autoScrollComponent = pendingAutoScroll == null
                ? null : pendingAutoScroll.findComponent(decompiledClass);

        List<DecompilerLine> lines = decompiledClass.getLines();
        DecompilerImportSection importSection = DecompilerImportSection.find(lines);
        boolean importsFoldable = importSection != null && importSection.isFoldable();
        if (importsFoldable && !this.importsExpanded) {
            importSection.clearCollapsedRenderedBounds(lines);
        }

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            DecompilerLine line = lines.get(lineIndex);
            if (importsFoldable && !this.importsExpanded
                    && importSection.isHiddenWhenCollapsed(lineIndex)) {
                lineIndex = importSection.lastLineIndex();
                continue;
            }
            boolean firstImportLine = importsFoldable
                    && lineIndex == importSection.firstLineIndex();
            final float cursorScreenPosX = ImGui.getCursorScreenPosX();
            float currentLineNumberSpacing = this.getLineNumberSpacing(
                    line, lineNumberSpacing, firstImportLine);
            boolean collapsedImportHovered = firstImportLine && !this.importsExpanded
                    && !decompilerInputBlocked && this.isCollapsedImportHovered(
                    line, cursorScreenPosX, currentLineNumberSpacing, textSize);
            boolean collapsedImportTextHovered = firstImportLine && !this.importsExpanded
                    && !decompilerInputBlocked && this.isCollapsedImportTextHovered(
                    line, cursorScreenPosX, currentLineNumberSpacing, textSize);
            if (collapsedImportHovered) ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
            if (firstImportLine) {
                this.importAlpha.run(this.importsExpanded || collapsedImportHovered
                        ? 1.F : COLLAPSED_IMPORT_ALPHA);
                ImGui.pushStyleVar(ImGuiStyleVar.Alpha, this.importAlpha.getValue());
            }

            this.drawNavigationHighlight(line, cursorScreenPosX, textSize);

            int textOffset = 0, sameLines = 0;
            ImGui.setCursorPosX(cursorPosX + currentLineNumberSpacing);
            line.pos = ImGui.getCursorScreenPos().minus(2.5F, 0.F);
            boolean textPositioned = false;
            for (DecompilerLineText text : line.getComponents()) {
                boolean customRendered = text.getComponent().render();
                if (!customRendered) {
                    if (!textPositioned) {
                        line.pos = new ImVec2(line.pos.x, ImGui.getCursorScreenPosY());
                        textPositioned = true;
                    }
                    text.render(decompiledClass.isComponentHighlighted(text.getComponent()));
                    ImGui.sameLine(0.F, 0.F);
                } else {
                    text.captureRenderedBounds();
                    textPositioned = false;
                }

                if (pendingAutoScroll != null && pendingAutoScroll.isNavigationPending()
                        && text.getComponent() == autoScrollComponent) {
                    this.completeAutoScroll(pendingAutoScroll, decompiledClass, line, textOffset);
                }

                if (!decompilerInputBlocked && !collapsedImportTextHovered
                        && this.hoveredComponent == null && ImGui.isItemHovered()) {
                    this.hoveredComponent = text.getComponent();
                }

                textOffset += text.getText().length();

            }

            if (firstImportLine && !this.importsExpanded) {
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
                    && mousePosY >= cursorPosY
                    && mousePosY < cursorPosY + textSize.y + ImGui.getStyle().getItemSpacingY();

            if (hovered)
                this.cursor.handleHoveredLineInputs(
                        cursorScreenPosX, currentLineNumberSpacing, mousePosX, line);

            ImGui.sameLine(cursorPosX + currentLineNumberSpacing, 0.F);

            this.cursor.handleLineDrawing(line, cursorScreenPosX, currentLineNumberSpacing,
                    mousePosX, cursorPosY, textSize);

            ImGui.newLine();
        }

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
    }

    void updateDelimiterMatch(DecompilerCoordinates clickedCharacter, float mouseX) {
        this.clearDelimiterMatch();
        if (clickedCharacter == null || this.hasActiveRename()) return;

        int character = clickedCharacter.getCharacter();
        DecompilerLine.TextRangeBounds bounds = clickedCharacter.getLine()
                .getRenderedRange(character, character + 1);
        if (bounds == null || mouseX < bounds.minX() || mouseX > bounds.maxX()) return;

        DecompiledClass decompiledClass = this.getDecompiledClass();
        if (decompiledClass == null) return;
        DecompilerDelimiterMatcher.Match match = DecompilerDelimiterMatcher.findMatch(
                decompiledClass.getLines(), clickedCharacter);
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
        if (this.delimiterMatchClass != decompiledClass
                || !lines.contains(this.delimiterMatch.selected().getLine())
                || !lines.contains(this.delimiterMatch.matching().getLine())) {
            this.clearDelimiterMatch();
        }
    }

    private void drawDelimiterHighlights() {
        if (this.delimiterMatch == null) return;
        this.drawDelimiterHighlight(this.delimiterMatch.selected());
        this.drawDelimiterHighlight(this.delimiterMatch.matching());
    }

    private void drawDelimiterHighlight(DecompilerCoordinates coordinates) {
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
        int cursorLine = coordinates == null ? -1 : lines.indexOf(coordinates.getLine());
        int selectionLine = this.cursor.selectionEnd == null ? -1
                : lines.indexOf(this.cursor.selectionEnd.getLine());
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
        List<DecompilerLine> lines = decompiledClass.getLines();
        DecompilerImportSection section = DecompilerImportSection.find(lines);
        return section != null && section.isFoldable()
                && section.isHiddenWhenCollapsed(lines.indexOf(line));
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

    private boolean blockStickyHeaderInput() {
        if (this.stickyHeaderHeight <= 0.F) return false;

        float left = ImGui.getWindowPosX();
        float top = ImGui.getWindowPosY();
        float right = left + ImGui.getWindowWidth();
        return ImGui.isWindowHovered()
                && ImGui.isMouseHoveringRect(left, top, right, top + this.stickyHeaderHeight);
    }

    private boolean drawStickyHeaders(DecompiledClass decompiledClass, float lineNumberSpacing,
                                      ImVec2 textSize, boolean enumCardHovered) {
        DecompiledClass.StickyHeaders stickyHeaders = decompiledClass.getStickyHeaders();
        float visibleTop = ImGui.getWindowPosY();
        float visibleBottom = visibleTop + ImGui.getWindowHeight();
        float lineHeight = textSize.y + ImGui.getStyle().getItemSpacingY();
        List<DecompilerLine> visibleHeaders = new ArrayList<>(3);

        DecompilerLine classLine = stickyHeaders.classLine();
        boolean showClass = isLineAboveViewport(classLine, visibleTop, textSize.y);
        float methodVisibleTop = visibleTop + (showClass ? lineHeight : 0.F);
        DecompiledClass.StickyMethod currentMethod = null;
        for (DecompiledClass.StickyMethod method : stickyHeaders.methods()) {
            if (isLineAboveViewport(method.signatureLine(), methodVisibleTop, textSize.y)
                    && isLineAtOrBelowViewport(method.endLine(), methodVisibleTop, lineHeight)) {
                currentMethod = method;
            }
        }
        if (showClass) visibleHeaders.add(classLine);
        if (currentMethod != null) visibleHeaders.add(currentMethod.signatureLine());
        DecompilerLine methodLine = currentMethod == null ? null : currentMethod.signatureLine();

        float sourceVisibleTop = visibleTop + visibleHeaders.size() * lineHeight;
        DecompilerCoordinates delimiterPreview = this.getOffscreenDelimiterPreview(
                sourceVisibleTop, visibleBottom);
        if (delimiterPreview != null
                && !visibleHeaders.contains(delimiterPreview.getLine())) {
            visibleHeaders.add(delimiterPreview.getLine());
        }
        if (visibleHeaders.isEmpty()) {
            this.stickyHeaderHeight = 0.F;
            this.stickyDelimiterHover.run(0.F);
            return false;
        }

        float left = ImGui.getWindowPosX();
        float right = left + ImGui.getWindowWidth();
        float bottom = visibleTop + lineHeight * visibleHeaders.size();
        this.stickyHeaderHeight = bottom - visibleTop;
        ImDrawList drawList = ImGui.getWindowDrawList();
        drawList.addRectFilled(left, visibleTop, right, bottom, ImGui.getColorU32(ImGuiCol.WindowBg));

        float rowTop = visibleTop;
        boolean classHovered = false;
        boolean methodHovered = false;
        boolean delimiterHovered = false;
        boolean dedicatedDelimiterRow = false;
        boolean windowHovered = ImGui.isWindowHovered() && !enumCardHovered;
        for (DecompilerLine line : visibleHeaders) {
            boolean delimiterTarget = delimiterPreview != null
                    && line == delimiterPreview.getLine();
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
                    this.cursor.navigateTo(delimiterTarget ? delimiterPreview
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
        if (!dedicatedDelimiterRow) this.stickyDelimiterHover.run(0.F);

        drawList.addLine(left, bottom, right, bottom, ImGui.getColorU32(ImGuiCol.Border));

        float lineNumberX = left + ImGui.getStyle().getWindowPaddingX() - ImGui.getScrollX();
        float textX = lineNumberX + lineNumberSpacing;
        float textY = visibleTop + Math.max(0.F, (lineHeight - textSize.y) * 0.5F);
        drawList.pushClipRect(left, visibleTop, right, bottom, true);
        for (DecompilerLine line : visibleHeaders) {
            drawList.addText(lineNumberX, textY, CodeColorScheme.LINE_NUMBER,
                    String.valueOf(line.getLineNumber()));
            int highlightedCharacter = delimiterPreview != null
                    && line == delimiterPreview.getLine()
                    ? delimiterPreview.getCharacter() : -1;
            drawStickyLine(drawList, line, textX, textY, highlightedCharacter, textSize.y);
            textY += lineHeight;
        }
        drawList.popClipRect();

        boolean hovered = classHovered || methodHovered || delimiterHovered;
        if (hovered) {
            this.hoveredComponent = null;
        }
        return hovered;
    }

    private DecompilerCoordinates getOffscreenDelimiterPreview(float visibleTop,
                                                                float visibleBottom) {
        if (this.delimiterMatch == null) return null;
        DecompilerCoordinates matching = this.delimiterMatch.matching();
        int character = matching.getCharacter();
        DecompilerLine.TextRangeBounds bounds = matching.getLine()
                .getRenderedRange(character, character + 1);
        if (bounds != null && bounds.maxY() > visibleTop && bounds.minY() < visibleBottom) {
            return null;
        }
        return matching;
    }

    private static boolean isLineAboveViewport(DecompilerLine line, float visibleTop, float textHeight) {
        return line != null && line.pos != null && line.pos.y + textHeight < visibleTop;
    }

    private static boolean isLineAtOrBelowViewport(DecompilerLine line, float visibleTop, float lineHeight) {
        return line != null && line.pos != null && line.pos.y + lineHeight >= visibleTop;
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
        if (line.pos == null) {
            return;
        }

        float startY = line.pos.y - 2.F;
        float endX = ImGui.getWindowPosX() + ImGui.getWindowContentRegionMax().x;
        float endY = startY + textSize.y + 4.F;
        ImGui.getWindowDrawList().addRectFilled(startX, startY, endX, endY, highlight.getFillColor());
        ImGui.getWindowDrawList().addRect(startX, startY, endX, endY, highlight.getBorderColor());
    }

    private void drawSearchResults() {
        if (!this.searchVisible) {
            return;
        }

        for (DecompilerSearchResult result : this.searchResults) {
            DecompilerLine line = result.line();
            if (line.pos == null) {
                continue;
            }

            DecompilerLine.TextRangeBounds bounds = line.getRenderedRange(result.start(), result.end());
            if (bounds != null) {
                ImGui.getWindowDrawList().addRectFilled(bounds.minX() - 1.F, bounds.minY() - 1.F,
                        bounds.maxX() + 1.F, bounds.maxY() + 1.F, CodeColorScheme.SEARCH_RESULT);
            }
        }
    }

    private void drawSelectionMatches(DecompiledClass decompiledClass) {
        this.refreshSelectionMatches(decompiledClass);
        for (DecompilerSearchResult result : this.selectionMatches) {
            DecompilerLine line = result.line();
            if (line.pos == null) {
                continue;
            }

            DecompilerLine.TextRangeBounds bounds = line.getRenderedRange(result.start(), result.end());
            if (bounds != null) {
                ImGui.getWindowDrawList().addRect(bounds.minX() - 1.F, bounds.minY() - 1.F,
                        bounds.maxX() + 1.F, bounds.maxY() + 1.F,
                        SELECTION_MATCH_BORDER, 0.F, 0, 1.F);
            }
        }
    }

    private void refreshSelectionMatches(DecompiledClass decompiledClass) {
        String selectedText = this.cursor.shouldHighlightSelectionMatches() ? this.cursor.getSelectionText() : "";
        if (!this.selectionMatchesDirty && this.selectionMatchesClass == decompiledClass
                && this.selectionMatchText.equals(selectedText)) {
            return;
        }

        this.selectionMatchesDirty = false;
        this.selectionMatchesClass = decompiledClass;
        this.selectionMatchText = selectedText;
        this.selectionMatches.clear();
        if (selectedText.isBlank() || selectedText.indexOf('\n') >= 0 || selectedText.indexOf('\r') >= 0) {
            return;
        }

        for (DecompilerLine line : decompiledClass.getLines()) {
            String text = line.getText();
            int start = 0;
            while ((start = text.indexOf(selectedText, start)) != -1) {
                int end = start + selectedText.length();
                this.selectionMatches.add(new DecompilerSearchResult(line, start, end));
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
