package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import java.util.List;

/** The source range occupied by decompiler-generated import declarations. */
public record DecompilerImportSection(int firstLineIndex, int lastLineIndex, int importLineCount) {
    public static DecompilerImportSection find(List<DecompilerLine> lines) {
        int first = -1;
        int last = -1;
        int count = 0;
        for (int index = 0; index < lines.size(); index++) {
            boolean importLine = lines.get(index).getComponents().stream()
                    .anyMatch(text -> text.getComponent().isImportDeclaration());
            if (!importLine) continue;
            if (first == -1) first = index;
            last = index;
            count++;
        }
        return first == -1 ? null : new DecompilerImportSection(first, last, count);
    }

    boolean contains(int lineIndex) {
        return lineIndex >= firstLineIndex && lineIndex <= lastLineIndex;
    }

    boolean isFoldable() {
        return importLineCount > 1;
    }

    boolean isHiddenWhenCollapsed(int lineIndex) {
        return isFoldable() && lineIndex > firstLineIndex && lineIndex <= lastLineIndex;
    }

    int hiddenLineCount() {
        return isFoldable() ? lastLineIndex - firstLineIndex : 0;
    }

    int visibleLineCount(int sourceLineCount, boolean expanded) {
        return expanded ? sourceLineCount : sourceLineCount - this.hiddenLineCount();
    }

    int sourceIndexForVisibleRow(int visibleRow, boolean expanded) {
        if (expanded || visibleRow <= firstLineIndex) return visibleRow;
        return visibleRow + this.hiddenLineCount();
    }

    int visibleRowForSourceIndex(int sourceIndex, boolean expanded) {
        if (expanded || sourceIndex <= firstLineIndex) return sourceIndex;
        if (sourceIndex <= lastLineIndex) return firstLineIndex;
        return sourceIndex - this.hiddenLineCount();
    }

    void clearCollapsedRenderedBounds(List<DecompilerLine> lines) {
        for (int index = firstLineIndex + 1; index <= lastLineIndex; index++) {
            lines.get(index).clearRenderedBounds();
        }
    }
}
