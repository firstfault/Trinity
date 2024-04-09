package me.f1nal.trinity.gui.frames.impl.entryviewer.impl.decompiler;

public class DecompilerCoordinates {
    private final DecompilerLine line;
    private final int character;

    public DecompilerCoordinates(DecompilerLine line, int character) {
        this.line = line;
        this.character = character;
    }

    public DecompilerLine getLine() {
        return line;
    }

    public int getCharacter() {
        return character;
    }
}
