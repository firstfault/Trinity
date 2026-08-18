package me.f1nal.trinity.gui.navigation;

/**
 * Restorable state for the decompiler view represented by a navigation entry.
 * Line indexes are zero-based; a negative cursor line means no caret was set.
 */
public record NavigationViewState(int cursorLine, int cursorCharacter,
                                  int selectionLine, int selectionCharacter,
                                  boolean selectionUsesBoundaries,
                                  float scrollX, float scrollY,
                                  boolean importsExpanded) {
    public boolean hasCursor() {
        return cursorLine >= 0;
    }

    public boolean hasSelection() {
        return selectionLine >= 0;
    }
}
