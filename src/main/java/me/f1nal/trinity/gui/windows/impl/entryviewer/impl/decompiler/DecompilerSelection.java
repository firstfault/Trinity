package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

public class DecompilerSelection {
    private final DecompilerCoordinates start;
    private DecompilerCoordinates end;

    public DecompilerSelection(DecompilerCoordinates start, DecompilerCoordinates end) {
        this.start = start;
        this.end = end;
    }

    public DecompilerCoordinates getStart() {
        return start;
    }

    public DecompilerCoordinates getEnd() {
        return end;
    }

    public void setEnd(DecompilerCoordinates end) {
        this.end = end;
    }
}
