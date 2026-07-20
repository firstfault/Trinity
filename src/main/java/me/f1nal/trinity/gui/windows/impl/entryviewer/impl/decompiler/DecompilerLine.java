package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import imgui.ImGui;
import imgui.ImVec2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DecompilerLine {
    /**
     * The text components to be drawn on this specific line.
     */
    private final List<DecompilerLineText> components = new ArrayList<>();
    /**
     * Index of this line.
     */
    private final int lineNumber;
    public ImVec2 pos;

    public DecompilerLine(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void addComponent(DecompilerLineText component) {
        this.components.add(component);
    }

    public List<DecompilerLineText> getComponents() {
        return components;
    }

    public String getText() {
        return components.stream().map(DecompilerLineText::getText).collect(Collectors.joining());
    }

    public DecompilerComponent getComponentAtCharacter(int character) {
        int totalLength = 0;
        for (DecompilerLineText component : components) {
            int length = component.getText().length();

            if (character >= totalLength && character < totalLength + length) {
                return component.getComponent();
            }

            totalLength += length;
        }
        return null;
    }

    public DecompilerComponent getRecursiveInvocation() {
        for (DecompilerLineText component : components) {
            if (component.getComponent().isRecursiveInvocation()) return component.getComponent();
        }
        return null;
    }

    /**
     * Resolves a character range against the rectangles ImGui actually produced for each colored
     * component. Measuring the concatenated line drifts because it does not match separate
     * {@code text()/sameLine()} item layout and kerning.
     */
    public TextRangeBounds getRenderedRange(int start, int end) {
        String lineText = this.getText();
        int rangeStart = Math.max(0, Math.min(start, lineText.length()));
        int rangeEnd = Math.max(rangeStart, Math.min(end, lineText.length()));
        if (rangeStart == rangeEnd) {
            return null;
        }

        DecompilerLineText startComponent = null;
        DecompilerLineText endComponent = null;
        int startComponentOffset = 0;
        int endComponentOffset = 0;
        int offset = 0;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (DecompilerLineText component : this.components) {
            int componentEnd = offset + component.getText().length();
            if (component.hasRenderedBounds() && componentEnd > rangeStart && offset < rangeEnd) {
                if (startComponent == null) {
                    startComponent = component;
                    startComponentOffset = offset;
                }
                endComponent = component;
                endComponentOffset = offset;
                minY = Math.min(minY, component.getRenderedMin().y);
                maxY = Math.max(maxY, component.getRenderedMax().y);
            }
            offset = componentEnd;
        }

        if (startComponent == null || endComponent == null) {
            return null;
        }

        int localStart = rangeStart - startComponentOffset;
        int localEnd = rangeEnd - endComponentOffset;
        float minX = startComponent.getRenderedMin().x
                + ImGui.calcTextSize(startComponent.getText().substring(0, localStart)).x;
        float maxX = endComponent.getRenderedMin().x
                + ImGui.calcTextSize(endComponent.getText().substring(0, localEnd)).x;
        return new TextRangeBounds(minX, minY, maxX, maxY);
    }

    /** Returns the nearest caret boundary for a screen-space mouse X coordinate. */
    public int getCharacterAtRenderedX(float mouseX) {
        return this.getCharacterAtRenderedX(mouseX, true);
    }

    /** Returns the source character directly beneath a screen-space mouse X coordinate. */
    public int getCharacterUnderRenderedX(float mouseX) {
        return this.getCharacterAtRenderedX(mouseX, false);
    }

    private int getCharacterAtRenderedX(float mouseX, boolean nearestBoundary) {
        int offset = 0;
        for (DecompilerLineText component : this.components) {
            String text = component.getText();
            int length = text.length();
            if (length == 0) {
                continue;
            }

            if (component.hasRenderedBounds()) {
                float componentX = component.getRenderedMin().x;
                if (mouseX <= componentX) {
                    return offset;
                }
                if (mouseX <= component.getRenderedMax().x) {
                    float left = componentX;
                    for (int index = 0; index < length; index++) {
                        float right = componentX
                                + ImGui.calcTextSize(text.substring(0, index + 1)).x;
                        if (mouseX <= right) {
                            if (nearestBoundary && mouseX > (left + right) * 0.5F) {
                                return offset + index + 1;
                            }
                            return offset + index;
                        }
                        left = right;
                    }
                }
            }

            offset += length;
        }
        return nearestBoundary ? offset : Math.max(offset - 1, 0);
    }

    /** Returns the real screen-space X boundary before a source character. */
    public Float getRenderedCharacterX(int character) {
        int target = Math.max(0, Math.min(character, this.getText().length()));
        int offset = 0;
        for (DecompilerLineText component : this.components) {
            String text = component.getText();
            int componentEnd = offset + text.length();
            if (!text.isEmpty() && component.hasRenderedBounds() && target <= componentEnd) {
                int localCharacter = Math.max(0, target - offset);
                return component.getRenderedMin().x
                        + ImGui.calcTextSize(text.substring(0, localCharacter)).x;
            }
            offset = componentEnd;
        }
        return null;
    }

    public record TextRangeBounds(float minX, float minY, float maxX, float maxY) {
    }
}
