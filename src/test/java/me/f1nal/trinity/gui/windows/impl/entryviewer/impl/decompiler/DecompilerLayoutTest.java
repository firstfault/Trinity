package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecompilerLayoutTest {
    @Test
    void collapsedImportsMapVisibleRowsWithoutBuildingAnotherLineList() {
        List<DecompilerLine> lines = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            DecompilerComponent component = new DecompilerComponent("line" + index);
            component.setImportDeclaration(index >= 1 && index <= 3);
            DecompilerLine line = new DecompilerLine(index + 1);
            line.addComponent(new DecompilerLineText(component.getText(), component));
            lines.add(line);
        }

        DecompilerImportSection section = DecompilerImportSection.find(lines);
        assertEquals(1, section.firstLineIndex());
        assertEquals(3, section.lastLineIndex());
        assertEquals(4, section.visibleLineCount(lines.size(), false));
        assertEquals(0, section.sourceIndexForVisibleRow(0, false));
        assertEquals(1, section.sourceIndexForVisibleRow(1, false));
        assertEquals(4, section.sourceIndexForVisibleRow(2, false));
        assertEquals(5, section.sourceIndexForVisibleRow(3, false));
        assertEquals(1, section.visibleRowForSourceIndex(2, false));
        assertEquals(1, section.visibleRowForSourceIndex(3, false));
        assertEquals(6, section.visibleLineCount(lines.size(), true));
        assertEquals(3, section.sourceIndexForVisibleRow(3, true));
    }

    @Test
    void lineCachesTextAndRecursiveMarkerAsComponentsAreAdded() {
        DecompilerLine line = new DecompilerLine(1);
        DecompilerComponent first = new DecompilerComponent("alpha");
        DecompilerComponent recursive = new DecompilerComponent("beta");
        recursive.setRecursiveInvocation(true);

        line.addComponent(new DecompilerLineText(first.getText(), first));
        assertEquals("alpha", line.getText());
        assertFalse(line.getText().isEmpty());

        line.addComponent(new DecompilerLineText(recursive.getText(), recursive));
        assertEquals("alphabeta", line.getText());
        assertTrue(line.getRecursiveInvocation().isRecursiveInvocation());
        assertSame(recursive, line.getRecursiveInvocation());
    }
}
