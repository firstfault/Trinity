package me.f1nal.trinity.gui.frames.impl.entryviewer.impl.decompiler;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiButtonFlags;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseCursor;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.Stopwatch;

import java.awt.event.KeyEvent;
import java.util.List;

public class DecompilerCursor {
    private final DecompilerWindow window;
    public DecompilerCoordinates coordinates;
    private final Stopwatch blink = new Stopwatch();
    private boolean scroll;

    public DecompilerCursor(DecompilerWindow window) {
        this.window = window;
    }

    public void handleHoveredLineInputs(float cursorScreenPosX, float lineNumberSpacing, float mousePosX, DecompilerLine line) {
        final float startX = cursorScreenPosX + lineNumberSpacing;

        if (mousePosX <= startX) {
            return;
        }

        ImGui.setMouseCursor(ImGuiMouseCursor.TextInput);

        if (ImGui.isMouseClicked(0)) {
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

            this.coordinates = new DecompilerCoordinates(line, Math.max(characterPosition, 0));
            this.blink.reset();
        }
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
        final long blinkTime = 550L;

        if (blink.hasPassed(blinkTime)) {
            if (blink.hasPassed(blinkTime * 2L)) {
                blink.reset();
            }
            return;
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

    public void handleKeyboardInputs() {
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
}
