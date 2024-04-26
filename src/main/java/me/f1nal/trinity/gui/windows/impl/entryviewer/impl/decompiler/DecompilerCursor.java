package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseCursor;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.Stopwatch;

import java.util.List;

public class DecompilerCursor {
    /**
     * Cursor blink interval.
     */
    private static final long BLINK_TIME = 555L;

    private final DecompilerWindow window;
    /**
     * Coordinates of the cursor.
     */
    public DecompilerCoordinates coordinates;
    /**
     * Text selection (when dragging over text)
     */
    public DecompilerCoordinates selectionEnd;
    /**
     * Cursor blink animation
     */
    private final Stopwatch blink = new Stopwatch();
    /**
     * If set to {@code true}, triggers a forced scroll to the cursor in the case it is not visible.
     */
    private boolean scroll;
    /**
     * If we are currently dragging the mouse across the screen, causing a selection box.
     * @see DecompilerCursor#selectionEnd
     */
    private boolean draggingSelection;

    public DecompilerCursor(DecompilerWindow window) {
        this.window = window;
    }

    public void handleHoveredLineInputs(float cursorScreenPosX, float lineNumberSpacing, float mousePosX, DecompilerLine line) {
        final float startX = cursorScreenPosX + lineNumberSpacing;

        if (mousePosX <= startX) {
            return;
        }

        if (this.draggingSelection) {
            DecompilerCoordinates newCoordinates = this.getCoordinates(line, mousePosX, startX);
            if (!this.coordinates.equals(newCoordinates)) {
                if (!newCoordinates.equals(this.selectionEnd)) {
                    this.selectionEnd = newCoordinates;
                    this.blink.reset();
                }
            }
        }

        ImGui.setMouseCursor(ImGuiMouseCursor.TextInput);

        if (ImGui.isMouseClicked(0)) {
            this.setCoordinates(this.getCoordinates(line, mousePosX, startX));
            this.draggingSelection = true;
            this.selectionEnd = null;
            this.blink.reset();
        }
    }

    public void setCoordinates(DecompilerCoordinates coordinates) {
        this.coordinates = coordinates;
        this.window.getDecompiledClass().setComponentHighlighted(coordinates.getComponent());
    }

    private DecompilerCoordinates getCoordinates(DecompilerLine line, float mousePosX, float startX) {
        final String text = line.getText();

        int characterPosition = -1;

        for (int i = 0; i < text.length(); i++) {
            final String substring = text.substring(0, i);
            final float substringWidth = ImGui.calcTextSize(substring).x;

            if (mousePosX <= startX + substringWidth) {
                break;
            }

            characterPosition = i;
        }

        return new DecompilerCoordinates(line, Math.max(characterPosition, 0));
    }

    public void moveHorizontally(int delta) {
        final int nextCharacter = this.coordinates.getCharacter() + delta;
        if (nextCharacter < 0) {
            this.moveVertically(-1);
            this.setCharacter(Integer.MAX_VALUE);
        } else if (nextCharacter >= this.coordinates.getLine().getText().length()) {
            this.moveVertically(1);
            this.setCharacter(0);
        } else {
            this.setCharacter(nextCharacter);
        }
    }

    public void setCharacter(int character) {
        this.setCoordinates(new DecompilerCoordinates(this.coordinates.getLine(), Math.min(Math.max(character, 0), this.coordinates.getLine().getText().length())));
    }

    public void moveVertically(int delta) {
        List<DecompilerLine> lines = window.getDecompiledClass().getLines();
        int indexOf = lines.indexOf(this.coordinates.getLine()) + delta;

        if (indexOf < 0 || indexOf >= lines.size()) {
            return;
        }

        this.setCoordinates(new DecompilerCoordinates(lines.get(indexOf), this.coordinates.getCharacter()));
    }

    public void handleLineCursorDrawing(DecompilerCoordinates coordinates, float cursorScreenPosX, float lineNumberSpacing, float mousePosX, float cursorPosY, ImVec2 textSize) {
        if (blink.hasPassed(BLINK_TIME)) {
            if (blink.hasPassed(BLINK_TIME * 2L)) {
                blink.reset();
            } else {
                return;
            }
        }

        String lineText = coordinates.getLine().getText();

        if (coordinates.getCharacter() >= lineText.length()) {
            coordinates = new DecompilerCoordinates(coordinates.getLine(), lineText.length());
        }

        String substring = lineText.substring(0, coordinates.getCharacter());
        float substringSizeX = ImGui.calcTextSize(substring).x + 3.F;

        float cursorPositionX = cursorScreenPosX + lineNumberSpacing + substringSizeX - 0.5F;
        float cursorPositionY = cursorPosY + ImGui.getWindowPosY() - ImGui.getScrollY();

        ImGui.getWindowDrawList().addLine(cursorPositionX, cursorPositionY, cursorPositionX, cursorPositionY + textSize.y, this.selectionEnd == null ? CodeColorScheme.CURSOR : CodeColorScheme.CURSOR_SELECTION, 1.F);
    }

    public void setScrollToCursor() {
        this.scroll = true;
        this.blink.reset();
    }

    public void handleInputs(float mousePosX, float mousePosY) {
        if (!ImGui.isMouseDown(0)) {
            this.draggingSelection = false;
        }

        if (this.coordinates == null) {
            return;
        }

        if (ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.DownArrow))) this.moveVertically(1);
        else if (ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.UpArrow))) this.moveVertically(-1);
        else if (ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.LeftArrow))) this.moveHorizontally(-1);
        else if (ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.RightArrow))) this.moveHorizontally(1);
        else return;

        this.setScrollToCursor();
    }

    public void handleLineDrawing(DecompilerLine line, float cursorScreenPosX, float lineNumberSpacing, float mousePosX, float cursorPosY, ImVec2 textSize) {
        if (this.selectionEnd != null && this.selectionEnd.getLine() == line) {
            // TODO: Selection box
            this.handleLineCursorDrawing(this.selectionEnd, cursorScreenPosX, lineNumberSpacing, mousePosX, cursorPosY, textSize);

        }

        if (this.coordinates != null && this.coordinates.getLine() == line) {
            this.handleLineCursorDrawing(this.coordinates, cursorScreenPosX, lineNumberSpacing, mousePosX, cursorPosY, textSize);

            if (this.scroll) {
                ImVec2 oldCursorPos = ImGui.getCursorPos();
                ImGui.text("");
                ImGui.setCursorPos(oldCursorPos.x, oldCursorPos.y);
                if (!ImGui.isItemVisible()) {
                    ImGui.setScrollHereX();
                    ImGui.setScrollHereY();
                }
                this.scroll = false;
            }
        }
    }

    public String getSelectionText() {
        if (true) {
            // TODO :(
            return "";
        }
        if (this.coordinates == null || this.selectionEnd == null) {
            return "";
        }

        DecompilerCoordinates from = this.coordinates, to = this.selectionEnd;

        if (from.getLine().getLineNumber() > to.getLine().getLineNumber()) {
            DecompilerCoordinates temp;
            temp = from;
            from = to;
            to = temp;
        }

        String text = from.getLine().getText();
        StringBuilder result = new StringBuilder();
        // Start line
        result.append(text.substring(Math.min(from.getCharacter(), text.length())));
        if (!from.getLine().equals(to.getLine())) {

        }
        // End line
        return result.toString();
    }
}
