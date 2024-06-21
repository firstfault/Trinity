package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

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
}
