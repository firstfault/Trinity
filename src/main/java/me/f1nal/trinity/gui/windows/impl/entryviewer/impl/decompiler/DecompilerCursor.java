package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiMouseCursor;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.Stopwatch;
import me.f1nal.trinity.util.animation.Animation;
import me.f1nal.trinity.util.animation.Easing;

import java.util.List;

public class DecompilerCursor {
    /**
     * Cursor blink interval.
     */
    private static final long BLINK_TIME = 555L;
    private static final long SCROLL_ANIMATION_TIME = 320L;

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
    private Animation scrollAnimation;
    private float scrollDestination;
    /**
     * If we are currently dragging the mouse across the screen, causing a selection box.
     * @see DecompilerCursor#selectionEnd
     */
    private boolean draggingSelection;
    /**
     * Whether the current selection was created by dragging and should highlight matching text.
     */
    private boolean highlightSelectionMatches;
    /** Whether a dragged selection stores caret boundaries instead of inclusive characters. */
    private boolean selectionUsesBoundaries;

    public DecompilerCursor(DecompilerWindow window) {
        this.window = window;
    }

    public void handleHoveredLineInputs(float cursorScreenPosX, float lineNumberSpacing, float mousePosX, DecompilerLine line) {
        final float startX = cursorScreenPosX + lineNumberSpacing;

        if (mousePosX <= startX) {
            return;
        }

        if (this.draggingSelection) {
            DecompilerCoordinates newCoordinates = this.getCoordinates(line, mousePosX, true);
            if (!this.coordinates.equals(newCoordinates)) {
                if (!newCoordinates.equals(this.selectionEnd)) {
                    this.selectionEnd = newCoordinates;
                    this.selectionUsesBoundaries = true;
                    this.highlightSelectionMatches = true;
                    this.blink.reset();
                }
            }
        }

        ImGui.setMouseCursor(ImGuiMouseCursor.TextInput);

        int clickCount = ImGui.getMouseClickedCount(ImGuiMouseButton.Left);
        if (clickCount >= 3) {
            this.selectLine(line);
        } else if (clickCount == 2) {
            this.selectWord(this.getCoordinates(line, mousePosX, false));
        } else if (ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            this.setCoordinates(this.getCoordinates(line, mousePosX, true));
            this.draggingSelection = true;
            this.selectionEnd = null;
            this.selectionUsesBoundaries = false;
            this.highlightSelectionMatches = false;
            this.blink.reset();
        }
    }

    private void selectLine(DecompilerLine line) {
        String text = line.getText();
        if (text.isEmpty()) {
            this.draggingSelection = false;
            this.highlightSelectionMatches = false;
            this.selectionEnd = null;
            this.setCoordinates(new DecompilerCoordinates(line, 0));
        } else {
            this.selectRange(new DecompilerCoordinates(line, 0),
                    new DecompilerCoordinates(line, text.length() - 1), true);
        }
        this.blink.reset();
    }

    private void selectWord(DecompilerCoordinates coordinates) {
        String text = coordinates.getLine().getText();
        if (text.isEmpty()) {
            this.setCoordinates(coordinates);
            return;
        }

        int character = Math.min(coordinates.getCharacter(), text.length() - 1);
        int start = character;
        int end = character;
        if (Character.isJavaIdentifierPart(text.charAt(character))) {
            while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) {
                start--;
            }
            while (end + 1 < text.length() && Character.isJavaIdentifierPart(text.charAt(end + 1))) {
                end++;
            }
        }

        this.selectRange(new DecompilerCoordinates(coordinates.getLine(), start),
                new DecompilerCoordinates(coordinates.getLine(), end), true);
        this.blink.reset();
    }

    public void setCoordinates(DecompilerCoordinates coordinates) {
        this.coordinates = coordinates;
        this.window.getDecompiledClass().setComponentHighlighted(coordinates.getComponent());
    }

    public DecompilerComponent getComponent() {
        return this.coordinates == null ? null : this.coordinates.getComponent();
    }

    public void navigateTo(DecompilerCoordinates coordinates) {
        this.selectionEnd = null;
        this.draggingSelection = false;
        this.highlightSelectionMatches = false;
        this.selectionUsesBoundaries = false;
        this.setCoordinates(coordinates);
        this.setScrollToCursor();
    }

    public void selectRange(DecompilerCoordinates from, DecompilerCoordinates to, boolean highlightMatches) {
        this.draggingSelection = false;
        this.highlightSelectionMatches = highlightMatches;
        this.selectionUsesBoundaries = false;
        this.setCoordinates(from);
        this.selectionEnd = to;
    }

    private DecompilerCoordinates getCoordinates(DecompilerLine line, float mousePosX,
                                                 boolean nearestBoundary) {
        int character = nearestBoundary
                ? line.getCharacterAtRenderedX(mousePosX)
                : line.getCharacterUnderRenderedX(mousePosX);
        return new DecompilerCoordinates(line, character);
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

        Float renderedCharacterX = coordinates.getLine().getRenderedCharacterX(coordinates.getCharacter());
        float cursorPositionX = renderedCharacterX == null
                ? cursorScreenPosX + lineNumberSpacing
                + ImGui.calcTextSize(lineText.substring(0, coordinates.getCharacter())).x
                : renderedCharacterX;
        float cursorPositionY = cursorPosY + ImGui.getWindowPosY() - ImGui.getScrollY();

        ImGui.getWindowDrawList().addLine(cursorPositionX, cursorPositionY, cursorPositionX, cursorPositionY + textSize.y, this.selectionEnd == null ? CodeColorScheme.CURSOR : CodeColorScheme.CURSOR_SELECTION, 1.F);
    }

    public void setScrollToCursor() {
        this.scroll = true;
        this.blink.reset();
    }

    public void updateScrollAnimation() {
        if (this.scrollAnimation == null) return;

        boolean manualScroll = ImGui.isWindowHovered()
                && (ImGui.getIO().getMouseWheel() != 0.F
                || ImGui.isMouseClicked(ImGuiMouseButton.Left)
                || ImGui.isMouseDragging(ImGuiMouseButton.Left));
        if (manualScroll) {
            this.scrollAnimation = null;
            return;
        }

        this.scrollAnimation.run(this.scrollDestination);
        ImGui.setScrollY(this.scrollAnimation.getValue());
        if (this.scrollAnimation.isFinished()) {
            ImGui.setScrollY(this.scrollDestination);
            this.scrollAnimation = null;
        }
    }

    private void animateScrollToCurrentItem() {
        float itemCenterY = (ImGui.getItemRectMinY() + ImGui.getItemRectMaxY()) * 0.5F;
        float viewportCenterY = ImGui.getWindowPosY() + ImGui.getWindowHeight() * 0.5F;
        float target = ImGui.getScrollY() + itemCenterY - viewportCenterY;
        this.scrollDestination = Math.max(0.F, Math.min(target, ImGui.getScrollMaxY()));

        this.scrollAnimation = new Animation(Easing.EASE_OUT_CUBIC,
                SCROLL_ANIMATION_TIME, ImGui.getScrollY());
        this.scrollAnimation.run(this.scrollDestination);
    }

    public void handleInputs(float mousePosX, float mousePosY) {
        if (!ImGui.isMouseDown(0)) {
            this.draggingSelection = false;
        }

        if (this.coordinates == null) {
            return;
        }

        if (ImGui.isKeyPressed(ImGuiKey.DownArrow)) this.moveVertically(1);
        else if (ImGui.isKeyPressed(ImGuiKey.UpArrow)) this.moveVertically(-1);
        else if (ImGui.isKeyPressed(ImGuiKey.LeftArrow)) this.moveHorizontally(-1);
        else if (ImGui.isKeyPressed(ImGuiKey.RightArrow)) this.moveHorizontally(1);
        else return;

        this.setScrollToCursor();
    }

    public void handleLineDrawing(DecompilerLine line, float cursorScreenPosX, float lineNumberSpacing, float mousePosX, float cursorPosY, ImVec2 textSize) {
        if (this.coordinates != null && this.coordinates.getLine() == line) {
            if (this.selectionEnd == null) {
                this.handleLineCursorDrawing(this.coordinates, cursorScreenPosX, lineNumberSpacing, mousePosX, cursorPosY, textSize);
            }

            if (this.scroll) {
                ImVec2 oldCursorPos = ImGui.getCursorPos();
                ImGui.text("");
                ImGui.setCursorPos(oldCursorPos.x, oldCursorPos.y);
                if (!ImGui.isItemVisible()) {
                    ImGui.setScrollHereX();
                    this.animateScrollToCurrentItem();
                }
                this.scroll = false;
            }
        }
    }

    public void drawSelectionBox() {
        DecompilerCoordinates from = this.coordinates, to = this.selectionEnd;

        if (from == null || to == null) {
            return;
        }

        if (from.getLine().getLineNumber() > to.getLine().getLineNumber() ||
                (from.getLine().getLineNumber() == to.getLine().getLineNumber() && from.getCharacter() > to.getCharacter())) {
            DecompilerCoordinates temp = from;
            from = to;
            to = temp;
        }

        List<DecompilerLine> lines = window.getDecompiledClass().getLines();
        int startLine = lines.indexOf(from.getLine());
        int endLine = lines.indexOf(to.getLine());

        if (startLine == -1 || endLine == -1) {
            return;
        }

        for (int i = startLine; i <= endLine; i++) {
            DecompilerLine line = lines.get(i);
            String text = line.getText();
            int rangeStart = i == startLine ? from.getCharacter() : 0;
            int rangeEnd = i == endLine
                    ? Math.min(text.length(), to.getCharacter() + (this.selectionUsesBoundaries ? 0 : 1))
                    : text.length();
            DecompilerLine.TextRangeBounds bounds = line.getRenderedRange(rangeStart, rangeEnd);
            if (bounds != null) {
                ImGui.getWindowDrawList().addRectFilled(bounds.minX() - 1.F, bounds.minY() - 1.F,
                        bounds.maxX() + 1.F, bounds.maxY() + 1.F, CodeColorScheme.CURSOR_SELECTION);
            }
        }
    }

    public boolean hasTextSelection() {
        return selectionEnd != null && coordinates != null;
    }

    public boolean shouldHighlightSelectionMatches() {
        return this.highlightSelectionMatches && this.hasTextSelection();
    }

    public String getSelectionText() {
        if (!hasTextSelection()) {
            return "";
        }

        DecompilerCoordinates from = this.coordinates, to = this.selectionEnd;

        if (from.getLine().getLineNumber() > to.getLine().getLineNumber() ||
                (from.getLine().getLineNumber() == to.getLine().getLineNumber() && from.getCharacter() > to.getCharacter())) {
            DecompilerCoordinates temp = from;
            from = to;
            to = temp;
        }

        StringBuilder result = new StringBuilder();
        List<DecompilerLine> lines = window.getDecompiledClass().getLines();
        int startLine = lines.indexOf(from.getLine());
        int endLine = lines.indexOf(to.getLine());
        if (startLine == -1 || endLine == -1) {
            return "";
        }

        for (int i = startLine; i <= endLine; i++) {
            DecompilerLine line = lines.get(i);
            String text = line.getText();

            if (i == startLine && i == endLine) {
                int startCharacter = Math.min(from.getCharacter(), text.length());
                int endCharacter = Math.min(to.getCharacter()
                        + (this.selectionUsesBoundaries ? 0 : 1), text.length());
                result.append(text, startCharacter, endCharacter);
            } else if (i == startLine) {
                result.append(text.substring(Math.min(from.getCharacter(), text.length())));
            } else if (i == endLine) {
                result.append(text.substring(0, Math.min(to.getCharacter()
                        + (this.selectionUsesBoundaries ? 0 : 1), text.length())));
            } else {
                result.append(text);
            }

            if (i != endLine) {
                result.append(System.lineSeparator());
            }
        }

        return result.toString();
    }
}
