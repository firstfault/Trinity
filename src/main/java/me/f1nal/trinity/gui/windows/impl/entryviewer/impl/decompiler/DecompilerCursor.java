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
    public DecompilerSelection selection;
    /**
     * Cursor blink animation
     */
    private final Stopwatch blink = new Stopwatch();
    /**
     * If set to {@code true}, triggers a forced scroll to the cursor in the case it is not visible.
     */
    private boolean scroll;
    private DecompilerSelection draggingSelection;

    public DecompilerCursor(DecompilerWindow window) {
        this.window = window;
    }

    public void handleHoveredLineInputs(float cursorScreenPosX, float lineNumberSpacing, float mousePosX, DecompilerLine line) {
        final float startX = cursorScreenPosX + lineNumberSpacing;

        if (mousePosX <= startX) {
            return;
        }

        if (this.draggingSelection != null) {
            this.selection = new DecompilerSelection(this.draggingSelection.getStart(), this.getCoordinates(line, mousePosX, startX));
        }

        ImGui.setMouseCursor(ImGuiMouseCursor.TextInput);

        if (ImGui.isMouseClicked(0)) {
            this.setCoordinates(this.getCoordinates(line, mousePosX, startX));
            this.draggingSelection = new DecompilerSelection(this.coordinates, this.coordinates);
            this.blink.reset();
        }
    }

    private void setCoordinates(DecompilerCoordinates coordinates) {
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
        this.coordinates = new DecompilerCoordinates(this.coordinates.getLine(), Math.min(Math.max(character, 0), this.coordinates.getLine().getText().length()));
    }

    public void moveVertically(int delta) {
        List<DecompilerLine> lines = window.getDecompiledClass().getLines();
        int indexOf = lines.indexOf(this.coordinates.getLine()) + delta;

        if (indexOf < 0 || indexOf >= lines.size()) {
            return;
        }

        this.coordinates = new DecompilerCoordinates(lines.get(indexOf), this.coordinates.getCharacter());
    }

    public void handleLineCursorDrawing(float cursorScreenPosX, float lineNumberSpacing, float mousePosX, float cursorPosY, ImVec2 textSize) {
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

        ImGui.getWindowDrawList().addLine(cursorPositionX, cursorPositionY, cursorPositionX, cursorPositionY + textSize.y, CodeColorScheme.CURSOR, 1.F);

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

    public void setScrollToCursor() {
        this.scroll = true;
    }

    public void handleInputs(float mousePosX, float mousePosY) {
        if (!ImGui.isMouseDown(0)) {
            this.draggingSelection = null;
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
        this.blink.reset();
    }

    public void handleLineDrawing(DecompilerLine line, float cursorScreenPosX, float lineNumberSpacing, float mousePosX, float cursorPosY, ImVec2 textSize) {
        if (this.coordinates != null && this.coordinates.getLine() == line)
            this.handleLineCursorDrawing(cursorScreenPosX, lineNumberSpacing, mousePosX, cursorPosY, textSize);
    }
}
