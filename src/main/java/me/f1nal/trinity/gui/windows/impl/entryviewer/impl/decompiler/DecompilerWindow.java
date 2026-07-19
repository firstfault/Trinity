package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import com.google.common.eventbus.Subscribe;
import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiFocusedFlags;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiMouseCursor;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.object.DatabaseDecompiler;
import me.f1nal.trinity.decompiler.DecompiledClass;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.events.EventRefreshDecompilerText;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.packages.other.ExtractArchiveEntryRunnable;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.FontSettings;
import me.f1nal.trinity.gui.components.popup.MenuBarProgress;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.popup.PopupMenuBar;
import me.f1nal.trinity.gui.windows.impl.classstructure.ClassStructure;
import me.f1nal.trinity.gui.windows.impl.classstructure.ClassStructureWindow;
import me.f1nal.trinity.gui.windows.impl.entryviewer.ArchiveEntryViewerWindow;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.GuiUtil;
import me.f1nal.trinity.util.Stopwatch;
import me.f1nal.trinity.util.SystemUtil;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class DecompilerWindow extends ArchiveEntryViewerWindow<ClassTarget> implements IEventListener, IDatabaseSavable<DatabaseDecompiler> {
    private static final int MIN_LINE_NUMBER_DIGITS = 4;
    private static final int METHOD_PREVIEW_LINES = 7;
    private ClassInput selectedClass;
    /**
     * Notifies the selected class must be refreshed.
     */
    private boolean forceRefresh = true;
    /**
     * Text component that is currently hovered.
     */
    private DecompilerComponent hoveredComponent;
    private boolean resetLines;
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
    /**
     * Selection cursor.
     */
    public final DecompilerCursor cursor = new DecompilerCursor(this);
    private DecompilerAutoScroll autoscrollTo;
    private final Stopwatch focusTime = new Stopwatch();
    private static Stopwatch viewMember = new Stopwatch();

    public DecompilerWindow(ClassTarget classTarget, Trinity trinity) {
        super(trinity, classTarget);
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
        return getArchiveEntry().getDisplayOrRealName() + ".java";
    }

    public void setDecompileTarget(ClassInput classInput) {
        if (classInput == selectedClass) {
            return;
        }
        selectedClass = classInput;
        this.searchDirty = true;
        if (classInput != null && !trinity.getDatabase().isLoading()) this.save();
        if (this.isFocusGained()) this.updateClassStructure();
    }

    @Override
    protected void onFocusGain() {
        this.focusTime.reset();
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
        getMenuBar().setProgress(decompiling || progressive
                ? new MenuBarProgress("Decompiler", decompiling ? "Decompiling Methods" : "Rendering Methods", -1)
                : null);
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

    @Subscribe
    public void onRefreshDecompilerText(EventRefreshDecompilerText event) {
        DecompiledClass decompiledClass = getDecompiledClass();
        if (getDecompiledClass() != null && event.getPredicate().test(decompiledClass)) {
            this.resetLines = true;
        }
    }

    private ClassInput decompilingInput;

    private void drawDecompileTab() {
        this.runControls();

        if (ImGui.isWindowFocused(ImGuiFocusedFlags.RootAndChildWindows) && ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(GLFW.GLFW_KEY_F)) {
            this.openSearch();
        }

        DecompiledClass decompiledClass = this.getDecompiledClass();
        if (decompiledClass != null && decompiledClass.applyPendingOutput()) {
            this.searchDirty = true;
        }
        if (decompiledClass != null && this.resetLines) {
            decompiledClass.resetLines();
            this.resetLines = false;
            this.searchDirty = true;
        }

        if (this.searchVisible) {
            this.drawSearchBar(decompiledClass);
        } else {
            this.searchBarFocused = false;
        }

        ImGui.beginChild("DecompilerWindowChild", 0.F, 0.F, false, ImGuiWindowFlags.HorizontalScrollbar);

        if (decompiledClass == null) {
            ImGui.textUnformatted("...");
        } else {
            FontSettings decompilerFont = Main.getPreferences().getDecompilerFont();
            decompilerFont.pushFont();
            this.drawDecompiledOutput(decompiledClass);
            decompilerFont.popFont();
        }

        ImGui.endChild();
    }

    private void runControls() {
        if (this.forceRefresh) {
            this.forceRefresh = false;

            try {
                trinity.getDecompiler().decompile(selectedClass, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (trinity.getDecompiler().isDecompileFailed(selectedClass)) {
            ImGui.textColored(ImColor.rgb(245, 80, 80), "Decompilation failed");
        }
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
            boolean enterPressed = ImGui.isKeyPressed(GLFW.GLFW_KEY_ENTER);
            if (this.searchBarFocused && (ImGui.isKeyPressed(GLFW.GLFW_KEY_UP) || enterPressed)) {
                this.moveSearchResult(-1);
                if (enterPressed) this.focusSearch = true;
            } else if (this.searchBarFocused && ImGui.isKeyPressed(GLFW.GLFW_KEY_DOWN)) {
                this.moveSearchResult(1);
            }
            if (this.searchBarFocused && ImGui.isKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
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
        this.cursor.setCoordinates(new DecompilerCoordinates(result.line(), result.start()));
        this.cursor.selectionEnd = new DecompilerCoordinates(result.line(), result.end() - 1);
        this.cursor.setScrollToCursor();
    }

    public DecompiledClass getDecompiledClass() {
        return trinity.getDecompiler().getFromCache(selectedClass);
    }

    private void drawDecompiledOutput(DecompiledClass decompiledClass) {
        this.hoveredComponent = null;

        float mousePosY = ImGui.getMousePosY() + ImGui.getScrollY() - ImGui.getWindowPosY();
        float mousePosX = ImGui.getMousePosX() + ImGui.getScrollX();

        int lineNumberDigits = Math.max(MIN_LINE_NUMBER_DIGITS,
                String.valueOf(decompiledClass.getLines().size() + 1).length());
        ImVec2 textSize = ImGui.calcTextSize("0".repeat(lineNumberDigits));
        float lineNumberSpacing = 3.F + textSize.x;
        float cursorPosX = ImGui.getCursorPosX();

        if (!this.searchBarFocused) cursor.handleInputs(mousePosX, mousePosY);

        for (DecompilerLine line : decompiledClass.getLines()) {
            final float cursorScreenPosX = ImGui.getCursorScreenPosX();

            int textOffset = 0, sameLines = 0;
            ImGui.setCursorPosX(cursorPosX + lineNumberSpacing);
            line.pos = ImGui.getCursorScreenPos().minus(2.5F, 0.F);
            boolean textPositioned = false;
            for (DecompilerLineText text : line.getComponents()) {
                if (!text.getComponent().render()) {
                    if (!textPositioned) {
                        line.pos = ImGui.getCursorScreenPos().minus(2.5F, 0.F);
                        textPositioned = true;
                    }
                    text.render(decompiledClass.isComponentHighlighted(text.getComponent()));
                    ImGui.sameLine(0.F, 0.F);
                }

                if (this.autoscrollTo != null && text.getComponent() == this.autoscrollTo.findComponent(decompiledClass)) {
                    cursor.setCoordinates(new DecompilerCoordinates(line, textOffset));
                    cursor.setScrollToCursor();
                    this.autoscrollTo = null;
                }

                if (this.hoveredComponent == null && ImGui.isItemHovered()) {
                    this.hoveredComponent = text.getComponent();
                }

                textOffset += text.getText().length();

            }

            float cursorPosY = ImGui.getCursorPosY();
            final boolean hovered = ImGui.isWindowHovered() && mousePosY >= cursorPosY && mousePosY < cursorPosY + textSize.y + ImGui.getStyle().getItemSpacingY();

            if (hovered)
                this.cursor.handleHoveredLineInputs(cursorScreenPosX, lineNumberSpacing, mousePosX, line);

            ImGui.setCursorPosX(cursorPosX);
            ImGui.textColored(CodeColorScheme.LINE_NUMBER, String.valueOf(line.getLineNumber()));
            ImGui.sameLine();

            this.cursor.handleLineDrawing(line, cursorScreenPosX, lineNumberSpacing, mousePosX, cursorPosY, textSize);

            ImGui.newLine();
        }

        this.drawSearchResults();
        this.cursor.drawSelectionBox();

        boolean rightClick = ImGui.isWindowHovered() && ImGui.isMouseClicked(ImGuiMouseButton.Right);
        boolean leftClick = !rightClick && ImGui.isWindowHovered() && ImGui.isMouseClicked(ImGuiMouseButton.Left);

        if (this.hoveredComponent != null) {
            List<ColoredString> tooltip = this.hoveredComponent.createTooltip();
            MethodInput previewMethod = this.hoveredComponent.getPreviewMethod();

            if (tooltip != null || previewMethod != null) {
                ImGui.beginTooltip();

                if (tooltip != null) {
                    ColoredString.drawText(tooltip);
                }
                if (previewMethod != null) {
                    this.drawMethodPreview(previewMethod, tooltip != null);
                }

                ImGui.endTooltip();
            }

            if (this.hoveredComponent.getViewMember() != null) {
                if (ImGui.getIO().getKeyCtrl()) {
                    ImGui.setMouseCursor(ImGuiMouseCursor.Hand);

                    if (focusTime.hasPassed(150L) && viewMember.hasPassed(250L) && (ImGui.isKeyPressed(GLFW.GLFW_KEY_B) || leftClick)) {
                        Main.getDisplayManager().openDecompilerView(this.hoveredComponent.getViewMember());
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

        if (cursor.hasTextSelection() && ImGui.isWindowFocused() && ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(GLFW.GLFW_KEY_C)) {
            this.copyToClipboard();
        }
    }

    private void drawMethodPreview(MethodInput methodInput, boolean hasDetails) {
        DecompiledClass previewClass = trinity.getDecompiler().getOrDecompile(methodInput.getOwningClass());
        if (previewClass == null) {
            return;
        }

        previewClass.applyPendingOutput();
        DecompiledClass.MethodPreview preview = previewClass.getMethodPreview(methodInput, METHOD_PREVIEW_LINES);
        if (preview.lines().isEmpty()) {
            return;
        }

        if (hasDetails) {
            ImGui.separator();
        }
        if (preview.skippedLeading()) {
            ImGui.textColored(CodeColorScheme.DISABLED, "...");
        }
        for (List<DecompilerLineText> line : preview.lines()) {
            for (int i = 0; i < line.size(); i++) {
                DecompilerLineText text = line.get(i);
                if (i > 0) {
                    ImGui.sameLine(0.F, 0.F);
                }
                ImGui.textColored(text.getComponent().getColor(), text.getText());
            }
        }
        if (preview.hasMoreLines()) {
            ImGui.textColored(CodeColorScheme.DISABLED, "...");
        }
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

            String text = line.getText();
            float startX = line.pos.x + ImGui.calcTextSize(text.substring(0, result.start())).x;
            float endX = line.pos.x + ImGui.calcTextSize(text.substring(0, result.end())).x;
            float textHeight = ImGui.calcTextSize(text).y;
            float heightAdjustment = Main.getPreferences().getDecompilerFont().getSize() % 0.5F == 0 ? 0.5F : 0.F;
            ImGui.getWindowDrawList().addRectFilled(startX, line.pos.y - 2.F, endX, line.pos.y + textHeight + 2.F - heightAdjustment, CodeColorScheme.SEARCH_RESULT);
        }
    }

    public void setDecompileTarget(Input<?> input) {
        this.autoscrollTo = new DecompilerAutoScroll(this, input);
        this.setDecompileTarget(input.getOwningClass());
    }

    @Override
    public DatabaseDecompiler createDatabaseObject() {
        return new DatabaseDecompiler(this.selectedClass.getRealName());
    }

    private record DecompilerSearchResult(DecompilerLine line, int start, int end) {
    }
}
