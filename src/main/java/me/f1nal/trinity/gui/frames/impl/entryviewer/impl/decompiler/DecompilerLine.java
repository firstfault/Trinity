package me.f1nal.trinity.gui.frames.impl.entryviewer.impl.decompiler;

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
}
