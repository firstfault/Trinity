package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import java.util.Objects;

public class DecompilerCoordinates {
    private final DecompilerLine line;
    private final int character;

    public DecompilerCoordinates(DecompilerLine line, int character) {
        this.line = line;
        this.character = character;
    }

    public DecompilerComponent getComponent() {
        return line.getComponentAtCharacter(this.getCharacter());
    }

    public DecompilerLine getLine() {
        return line;
    }

    public int getCharacter() {
        return character;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DecompilerCoordinates that = (DecompilerCoordinates) o;
        return character == that.character && Objects.equals(line, that.line);
    }

    @Override
    public int hashCode() {
        return Objects.hash(line, character);
    }
}
