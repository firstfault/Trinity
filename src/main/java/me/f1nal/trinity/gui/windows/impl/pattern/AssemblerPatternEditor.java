package me.f1nal.trinity.gui.windows.impl.pattern;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.texteditor.TextEditor;
import imgui.extension.texteditor.TextEditorCursorPosition;
import imgui.extension.texteditor.TextEditorCursorSelection;
import imgui.extension.texteditor.TextEditorLanguage;
import imgui.extension.texteditor.flag.TextEditorColor;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.pattern.InstructionPatternCompiler;
import me.f1nal.trinity.gui.components.FontSettings;
import me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.AssemblerValueCodec;
import me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.OpcodeClasses;
import me.f1nal.trinity.theme.CodeColorScheme;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Compact assembler-format editor with validation-friendly opcode completion. */
final class AssemblerPatternEditor {
    private static final int AUTOCOMPLETE_VISIBLE_ROWS = 5;
    private static final int AUTOCOMPLETE_ALPHA = 102;
    private static final float AUTOCOMPLETE_MIN_WIDTH = 180.F;
    private static final float AUTOCOMPLETE_MAX_WIDTH = 360.F;
    private static final float EDITOR_LEFT_OFFSET = 20.F;
    private static final float GHOST_TEXT_OFFSET_X = 24.F;
    private static final float GHOST_TEXT_OFFSET_Y = 0.F;
    private final Trinity trinity;
    private final TextEditor editor = new TextEditor();
    private List<String> suggestions = List.of();   
    private boolean autocompleteActivated;
    private boolean autocompleteDismissed;
    private boolean editorFocused;
    private boolean autocompleteFocused;
    private boolean focusEditorNextFrame;
    private boolean autocompleteVisible;
    private boolean suggestionSelectionActive;
    private int selectedSuggestionIndex = -1;
    private float autocompleteMinX;
    private float autocompleteMinY;
    private float autocompleteMaxX;
    private float autocompleteMaxY;
    private String previousText = "";

    AssemblerPatternEditor(Trinity trinity) {
        this.trinity = trinity;
        editor.setLanguage(TextEditorLanguage.Python());
        editor.setShowLineNumbersEnabled(false);
        editor.setShowScrollbarMiniMapEnabled(false);
        editor.setShowWhitespacesEnabled(false);
        editor.setShowMatchingBrackets(true);
        editor.setAutoIndentEnabled(false);
        editor.setTabSize(4);
        editor.setText("");
    }

    EditorState draw(float height) {
        updatePalette();
        if (focusEditorNextFrame) {
            editor.setFocus();
            editorFocused = true;
            autocompleteDismissed = false;
            focusEditorNextFrame = false;
        }
        boolean completionKeyConsumed = handleCompletionKeyboard();
        TextEditorCursorSelection selectionAfterCompletion = completionKeyConsumed
                ? new TextEditorCursorSelection(editor.getMainCursorSelection()) : null;

        FontSettings font = me.f1nal.trinity.Main.getPreferences().getDecompilerFont();
        font.pushFont();
        if (completionKeyConsumed) editor.setReadOnlyEnabled(true);
        float editorWidth = Math.max(1.F, ImGui.getContentRegionAvailX() + EDITOR_LEFT_OFFSET);
        ImGui.setCursorPosX(ImGui.getCursorPosX() - EDITOR_LEFT_OFFSET);
        editor.render("###PatternAssemblerEditor", editorWidth, height);
        if (completionKeyConsumed) {
            editor.setReadOnlyEnabled(false);
            restoreSelection(selectionAfterCompletion);
        }
        ImVec2 editorMin = ImGui.getItemRectMin();
        ImVec2 editorMax = ImGui.getItemRectMax();
        updateEditorFocus(editorMin, editorMax);
        String text = editor.getText();
        boolean changed = !text.equals(previousText);
        if (changed) {
            clearSuggestionSelection();
            if (!text.isEmpty()) {
                autocompleteActivated = true;
                autocompleteDismissed = false;
                editorFocused = true;
            }
        }
        previousText = text;
        suggestions = createSuggestions();
        drawGhostText(editorMin);
        drawSuggestions(editorMin, editorMax);
        font.popFont();
        boolean searchShortcut = editorFocused && ImGui.getIO().getKeyCtrl()
                && ImGui.isKeyPressed(ImGuiKey.Enter, false);
        return new EditorState(text, changed, searchShortcut);
    }

    private void updateEditorFocus(ImVec2 editorMin, ImVec2 editorMax) {
        if (!ImGui.isMouseClicked(ImGuiMouseButton.Left)) return;
        ImVec2 mouse = ImGui.getMousePos();
        boolean overAutocomplete = autocompleteVisible
                && mouse.x >= autocompleteMinX && mouse.x <= autocompleteMaxX
                && mouse.y >= autocompleteMinY && mouse.y <= autocompleteMaxY;
        if (overAutocomplete) return;

        boolean overEditor = mouse.x >= editorMin.x && mouse.x <= editorMax.x
                && mouse.y >= editorMin.y && mouse.y <= editorMax.y
                && ImGui.isWindowHovered(ImGuiHoveredFlags.RootAndChildWindows);
        editorFocused = overEditor;
        autocompleteDismissed = !overEditor;
        autocompleteFocused = false;
        clearSuggestionSelection();
    }

    private boolean handleCompletionKeyboard() {
        if (!(editorFocused || autocompleteFocused) || !autocompleteActivated
                || autocompleteDismissed || suggestions.isEmpty()) {
            clearSuggestionSelection();
            return false;
        }

        if (ImGui.isKeyPressed(ImGuiKey.Escape, false)) {
            autocompleteDismissed = true;
            clearSuggestionSelection();
            return true;
        }

        if (!hasOnlyWhitespaceAfterCursor()) {
            clearSuggestionSelection();
            return false;
        }

        boolean control = ImGui.getIO().getKeyCtrl();
        boolean tab = !control && ImGui.isKeyPressed(ImGuiKey.Tab, false);
        boolean down = !control && ImGui.isKeyPressed(ImGuiKey.DownArrow, true);
        boolean up = !control && ImGui.isKeyPressed(ImGuiKey.UpArrow, true);
        boolean right = !control && ImGui.isKeyPressed(ImGuiKey.RightArrow, false);
        boolean enter = !control && (ImGui.isKeyPressed(ImGuiKey.Enter, false)
                || ImGui.isKeyPressed(ImGuiKey.KeypadEnter, false));

        if (!suggestionSelectionActive) {
            if (tab || down) {
                selectSuggestion(0);
                focusEditorNextFrame = true;
                return true;
            }
            if (up) {
                selectSuggestion(suggestions.size() - 1);
                focusEditorNextFrame = true;
                return true;
            }
            if (right) {
                acceptSuggestion(suggestions.get(0));
                return true;
            }
            return false;
        }

        if (enter || right) {
            acceptSuggestion(suggestions.get(selectedSuggestionIndex));
            return true;
        }
        if (tab || down) {
            int direction = tab && ImGui.getIO().getKeyShift() ? -1 : 1;
            selectSuggestion(Math.floorMod(selectedSuggestionIndex + direction, suggestions.size()));
            focusEditorNextFrame = true;
            return true;
        }
        if (up) {
            selectSuggestion(Math.floorMod(selectedSuggestionIndex - 1, suggestions.size()));
            focusEditorNextFrame = true;
            return true;
        }
        if (ImGui.isKeyPressed(ImGuiKey.Home, false)
                || ImGui.isKeyPressed(ImGuiKey.PageUp, false)) {
            selectSuggestion(0);
            focusEditorNextFrame = true;
            return true;
        }
        if (ImGui.isKeyPressed(ImGuiKey.End, false)
                || ImGui.isKeyPressed(ImGuiKey.PageDown, false)) {
            selectSuggestion(suggestions.size() - 1);
            focusEditorNextFrame = true;
            return true;
        }
        return false;
    }

    private boolean hasOnlyWhitespaceAfterCursor() {
        CompletionContext context = completionContext();
        int cursorColumn = context.tokenStart() + context.prefix().length();
        return context.line().substring(cursorColumn).isBlank();
    }

    private void selectSuggestion(int index) {
        suggestionSelectionActive = true;
        selectedSuggestionIndex = Math.max(0, Math.min(index, suggestions.size() - 1));
    }

    private void clearSuggestionSelection() {
        suggestionSelectionActive = false;
        selectedSuggestionIndex = -1;
    }

    private void restoreSelection(TextEditorCursorSelection selection) {
        if (selection == null) return;
        if (selection.start.equals(selection.end)) {
            editor.setCursor(selection.end.line, selection.end.column);
        } else {
            editor.selectRegion(selection.start.line, selection.start.column,
                    selection.end.line, selection.end.column);
        }
    }

    private void updatePalette() {
        editor.setPaletteColor(TextEditorColor.text, CodeColorScheme.TEXT);
        editor.setPaletteColor(TextEditorColor.keyword, CodeColorScheme.KEYWORD_DATA);
        editor.setPaletteColor(TextEditorColor.declaration, CodeColorScheme.KEYWORD_CALL);
        editor.setPaletteColor(TextEditorColor.number, CodeColorScheme.NUMBER);
        editor.setPaletteColor(TextEditorColor.string, CodeColorScheme.STRING);
        editor.setPaletteColor(TextEditorColor.comment, CodeColorScheme.DISABLED);
        editor.setPaletteColor(TextEditorColor.preprocessor, CodeColorScheme.DISABLED);
        editor.setPaletteColor(TextEditorColor.identifier, CodeColorScheme.TEXT);
        editor.setPaletteColor(TextEditorColor.knownIdentifier, CodeColorScheme.CLASS_REF);
        editor.setPaletteColor(TextEditorColor.background, CodeColorScheme.BACKGROUND);
        editor.setPaletteColor(TextEditorColor.cursor, CodeColorScheme.CURSOR);
        editor.setPaletteColor(TextEditorColor.selection, CodeColorScheme.CURSOR_SELECTION);
        editor.setPaletteColor(TextEditorColor.lineNumber, CodeColorScheme.LINE_NUMBER);
    }

    private List<String> createSuggestions() {
        CompletionContext context = completionContext();
        Set<String> candidates = new LinkedHashSet<>();
        if (context.tokenIndex() == 0) {
            candidates.addAll(OpcodeClasses.getNamesToClasses().keySet());
            candidates.add("*");
            candidates.add("...");
        } else {
            candidates.add("*");
            candidates.addAll(contextualOperands(context));
        }
        String prefix = context.prefix();
        return candidates.stream()
                .filter(candidate -> prefix.isEmpty()
                        || fuzzyScore(candidate, prefix) < Integer.MAX_VALUE)
                .sorted(Comparator.comparingInt(candidate -> fuzzyScore(candidate, prefix)))
                .limit(AUTOCOMPLETE_VISIBLE_ROWS)
                .toList();
    }

    private List<String> contextualOperands(CompletionContext context) {
        List<String> candidates = new ArrayList<>();
        Class<?> type = OpcodeClasses.getOpcodeClass(context.opcode().toLowerCase(Locale.ROOT));
        if (type == null) return candidates;
        if ((type == TypeInsnNode.class && context.tokenIndex() == 1)
                || ((type == FieldInsnNode.class || type == MethodInsnNode.class)
                && context.tokenIndex() == 1)) {
            trinity.getExecution().getClassList().stream().map(ClassInput::getRealName)
                    .map(AssemblerValueCodec::quote).forEach(candidates::add);
        }
        ClassInput owner = owner(context.lineTokens());
        if (owner != null && context.tokenIndex() == 2) {
            if (type == FieldInsnNode.class) owner.getFieldMap().values().stream()
                    .map(field -> AssemblerValueCodec.quote(field.getDetails().getName())).distinct().forEach(candidates::add);
            if (type == MethodInsnNode.class) owner.getMethodMap().values().stream()
                    .map(method -> AssemblerValueCodec.quote(method.getName())).distinct().forEach(candidates::add);
        }
        if (owner != null && context.tokenIndex() == 3) {
            if (type == FieldInsnNode.class) owner.getFieldMap().values().stream()
                    .map(field -> AssemblerValueCodec.quote(field.getDescriptor())).distinct().forEach(candidates::add);
            if (type == MethodInsnNode.class) owner.getMethodMap().values().stream()
                    .map(method -> AssemblerValueCodec.quote(method.getDescriptor())).distinct().forEach(candidates::add);
        }
        if (type == MethodInsnNode.class && context.tokenIndex() == 4) {
            candidates.add("false");
            candidates.add("true");
        }
        candidates.add("true");
        candidates.add("false");
        candidates.add("int(0)");
        candidates.add("string(\"\")");
        return candidates;
    }

    private ClassInput owner(List<String> tokens) {
        if (tokens.size() < 2 || tokens.get(1).contains("*") || tokens.get(1).contains("?")) return null;
        try {
            return trinity.getExecution().getClassInput(AssemblerValueCodec.parseQuotedString(tokens.get(1)));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void drawSuggestions(ImVec2 editorMin, ImVec2 editorMax) {
        if (!autocompleteActivated || autocompleteDismissed || suggestions.isEmpty()) {
            autocompleteVisible = false;
            autocompleteFocused = false;
            clearSuggestionSelection();
            return;
        }
        if (selectedSuggestionIndex >= suggestions.size()) clearSuggestionSelection();

        ImVec2 cursor = cursorScreenPosition(editorMin);
        float maximumTextWidth = 0.F;
        for (String suggestion : suggestions) {
            maximumTextWidth = Math.max(maximumTextWidth, ImGui.calcTextSize(suggestion).x);
        }
        float editorWidth = Math.max(1.F, editorMax.x - editorMin.x);
        float width = Math.min(editorWidth, Math.max(AUTOCOMPLETE_MIN_WIDTH,
                Math.min(AUTOCOMPLETE_MAX_WIDTH, maximumTextWidth + 24.F)));
        float rowHeight = ImGui.getFrameHeight();
        float height = Math.max(rowHeight, suggestions.size() * rowHeight + 6.F);
        float x = Math.max(editorMin.x, Math.min(cursor.x, editorMax.x - width));
        float y = cursor.y + editor.getLineHeight();
        autocompleteVisible = true;
        autocompleteMinX = x;
        autocompleteMinY = y;
        autocompleteMaxX = x + width;
        autocompleteMaxY = y + height;

        ImGui.setNextWindowPos(x, y, ImGuiCond.Always);
        ImGui.setNextWindowSize(width, height, ImGuiCond.Always);
        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoSavedSettings
                | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoFocusOnAppearing
                | ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoScrollbar
                | ImGuiWindowFlags.NoScrollWithMouse;
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.F);
        if (ImGui.begin("###PatternAutocomplete", flags)) {
            // Focusing the editor raises its host window above this non-focusable overlay.
            // Keep the overlay at the front of the display order without stealing focus back.
            imgui.internal.ImGui.bringWindowToDisplayFront(
                    imgui.internal.ImGui.getCurrentWindow());
            autocompleteFocused = ImGui.isWindowFocused();
            ImGui.getWindowDrawList().addRectFilled(x, y, x + width, y + height,
                    CodeColorScheme.setAlpha(CodeColorScheme.POPUP_BACKGROUND, AUTOCOMPLETE_ALPHA), 3.F);
            for (int i = 0; i < suggestions.size(); i++) {
                String suggestion = suggestions.get(i);
                float rowY = y + 3.F + i * rowHeight;
                ImGui.setCursorScreenPos(x + 3.F, rowY);
                boolean clicked = ImGui.invisibleButton(
                        "###PatternCompletion" + i, width - 6.F, rowHeight);
                boolean hovered = ImGui.isItemHovered();
                if (hovered && (clicked || Math.abs(ImGui.getIO().getMouseDeltaX()) > 0.F
                        || Math.abs(ImGui.getIO().getMouseDeltaY()) > 0.F)) {
                    selectSuggestion(i);
                }
                boolean selected = suggestionSelectionActive && selectedSuggestionIndex == i;
                if (selected) {
                    ImGui.getWindowDrawList().addRectFilled(x + 3.F, rowY,
                            x + width - 3.F, rowY + rowHeight,
                            CodeColorScheme.setAlpha(CodeColorScheme.KEYWORD_DATA, 48), 2.F);
                    ImGui.getWindowDrawList().addRect(x + 3.F, rowY,
                            x + width - 3.F, rowY + rowHeight,
                            CodeColorScheme.setAlpha(CodeColorScheme.KEYWORD_DATA, 82), 2.F);
                } else if (hovered) {
                    ImGui.getWindowDrawList().addRectFilled(x + 3.F, rowY,
                            x + width - 3.F, rowY + rowHeight,
                            CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 42), 2.F);
                }
                if (selected || (!suggestionSelectionActive && i == 0)) {
                    ImGui.getWindowDrawList().addRectFilled(x + 3.F, rowY + 3.F,
                            x + 5.F, rowY + rowHeight - 3.F,
                            CodeColorScheme.setAlpha(CodeColorScheme.KEYWORD_DATA, AUTOCOMPLETE_ALPHA), 1.F);
                }
                float textY = rowY + Math.max(0.F, (rowHeight - ImGui.getTextLineHeight()) * 0.5F);
                ImGui.getWindowDrawList().addText(x + 9.F, textY,
                        CodeColorScheme.setAlpha(CodeColorScheme.TEXT,
                                selected ? 180 : AUTOCOMPLETE_ALPHA), suggestion);
                if (clicked) {
                    acceptSuggestion(suggestion);
                    focusEditorNextFrame = true;
                }
            }
        }
        ImGui.end();
        ImGui.popStyleVar();
    }

    private void drawGhostText(ImVec2 editorMin) {
        if (suggestions.isEmpty()) return;
        CompletionContext context = completionContext();
        int cursorColumn = context.tokenStart() + context.prefix().length();
        if (cursorColumn != context.tokenEnd()
                || !context.line().substring(cursorColumn).isBlank()) {
            return;
        }
        String suggestion = suggestions.get(0);
        if (!suggestion.regionMatches(true, 0, context.prefix(), 0, context.prefix().length())) return;
        String suffix = suggestion.substring(context.prefix().length());
        if (suffix.isEmpty()) return;
        ImVec2 cursor = cursorScreenPosition(editorMin);
        ImGui.getWindowDrawList().addText(cursor.x, cursor.y,
                CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 115), suffix);
    }

    private ImVec2 cursorScreenPosition(ImVec2 editorMin) {
        TextEditorCursorPosition cursor = editor.getMainCursorPosition();
        float x = editorMin.x + GHOST_TEXT_OFFSET_X
                + (cursor.column - editor.getFirstVisibleColumn()) * editor.getGlyphWidth();
        float y = editorMin.y + GHOST_TEXT_OFFSET_Y
                + (cursor.line - editor.getFirstVisibleLine()) * editor.getLineHeight();
        return new ImVec2(x, y);
    }

    private void acceptSuggestion(String suggestion) {
        CompletionContext context = completionContext();
        String replacement = suggestion;
        if (context.tokenIndex() == 0 && context.line().trim().equals(context.prefix())) {
            replacement = InstructionPatternCompiler.completionTemplate(suggestion);
        }
        editor.replaceSectionText(context.lineNumber(), context.tokenStart(),
                context.lineNumber(), context.tokenEnd(), replacement);
        int wildcard = replacement.indexOf('*');
        if (wildcard >= 0) {
            editor.selectRegion(context.lineNumber(), context.tokenStart() + wildcard,
                    context.lineNumber(), context.tokenStart() + wildcard + 1);
        } else {
            editor.setCursor(context.lineNumber(), context.tokenStart() + replacement.length());
        }
        clearSuggestionSelection();
        editor.setFocus();
        focusEditorNextFrame = true;
    }

    private CompletionContext completionContext() {
        TextEditorCursorPosition cursor = editor.getMainCursorPosition();
        int lineNumber = Math.max(0, Math.min(cursor.line, Math.max(0, editor.getLineCount() - 1)));
        String line = editor.getLineCount() == 0 ? "" : editor.getLineText(lineNumber);
        int column = Math.max(0, Math.min(cursor.column, line.length()));
        int start = column;
        while (start > 0 && !Character.isWhitespace(line.charAt(start - 1))) start--;
        int end = column;
        while (end < line.length() && !Character.isWhitespace(line.charAt(end))) end++;
        String prefix = line.substring(start, column);
        List<String> tokens;
        try {
            tokens = me.f1nal.trinity.gui.windows.impl.assembler.AssemblerClipboardCodec.tokenize(line.trim());
        } catch (IllegalArgumentException exception) {
            tokens = List.of();
        }
        int tokenIndex = 0;
        for (int i = 0; i < start; i++) {
            if (!Character.isWhitespace(line.charAt(i))
                    && (i == 0 || Character.isWhitespace(line.charAt(i - 1)))) tokenIndex++;
        }
        String opcode = tokens.isEmpty() ? "" : tokens.get(0);
        return new CompletionContext(lineNumber, line, start, end, prefix, tokenIndex, opcode, tokens);
    }

    private static int fuzzyScore(String candidate, String prefix) {
        String left = candidate.toLowerCase(Locale.ROOT);
        String right = prefix.toLowerCase(Locale.ROOT);
        if (right.isEmpty()) return candidate.equals("aconst_null") ? 0 : 10 + candidate.length();
        if (left.equals(right)) return 0;
        if (left.startsWith(right)) return 10 + left.length() - right.length();
        int contains = left.indexOf(right);
        if (contains >= 0) return 100 + contains * 4 + left.length() - right.length();
        int distance = levenshtein(left, right);
        return distance <= Math.max(2, right.length() / 2) ? 200 + distance * 10 + left.length() : Integer.MAX_VALUE;
    }

    private static int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) previous[j] = j;
        for (int i = 1; i <= left.length(); i++) {
            int[] current = new int[right.length() + 1];
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            previous = current;
        }
        return previous[right.length()];
    }

    record EditorState(String text, boolean changed, boolean searchShortcut) {
    }

    private record CompletionContext(int lineNumber, String line, int tokenStart, int tokenEnd,
                                     String prefix, int tokenIndex, String opcode,
                                     List<String> lineTokens) {
    }
}
