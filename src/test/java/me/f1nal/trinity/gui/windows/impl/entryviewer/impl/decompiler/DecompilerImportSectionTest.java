package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecompilerImportSectionTest {
    @Test
    void findsTheCompleteImportBlockAndIncludesLinesBetweenImports() {
        DecompilerLine packageLine = line(component("package sample;", false));
        DecompilerLine firstImport = line(component("java.util.List", true));
        DecompilerLine blankLine = new DecompilerLine(3);
        DecompilerLine secondImport = line(component("java.util.Map", true));
        DecompilerLine classLine = line(component("public class Example {}", false));

        DecompilerImportSection section = DecompilerImportSection.find(List.of(
                packageLine, firstImport, blankLine, secondImport, classLine));

        assertEquals(1, section.firstLineIndex());
        assertEquals(3, section.lastLineIndex());
        assertEquals(2, section.importLineCount());
        assertTrue(section.contains(2));
        assertFalse(section.contains(4));
        assertFalse(section.isHiddenWhenCollapsed(1));
        assertTrue(section.isHiddenWhenCollapsed(2));
        assertTrue(section.isHiddenWhenCollapsed(3));
        assertFalse(section.isHiddenWhenCollapsed(4));
    }

    @Test
    void returnsNoSectionWhenTheDecompilerEmitsNoImports() {
        assertNull(DecompilerImportSection.find(List.of(
                line(component("public class Example {}", false)))));
    }

    private static DecompilerComponent component(String text, boolean importDeclaration) {
        DecompilerComponent component = new DecompilerComponent(text);
        component.setImportDeclaration(importDeclaration);
        return component;
    }

    private static DecompilerLine line(DecompilerComponent component) {
        DecompilerLine line = new DecompilerLine(1);
        line.addComponent(new DecompilerLineText(component.getText(), component));
        return line;
    }
}
