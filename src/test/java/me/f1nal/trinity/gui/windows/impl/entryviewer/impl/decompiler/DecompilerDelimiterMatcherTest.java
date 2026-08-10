package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DecompilerDelimiterMatcherTest {
    @Test
    void matchesNestedDelimitersAcrossLinesInBothDirections() {
        DecompilerLine first = line(1, "if (values[index]) {");
        DecompilerLine second = line(2, "    consume(values[index]);");
        DecompilerLine third = line(3, "}");
        List<DecompilerLine> lines = List.of(first, second, third);

        DecompilerDelimiterMatcher.Match opening = DecompilerDelimiterMatcher.findMatch(
                lines, new DecompilerCoordinates(first, first.getText().length() - 1));
        DecompilerDelimiterMatcher.Match closing = DecompilerDelimiterMatcher.findMatch(
                lines, new DecompilerCoordinates(third, 0));

        assertNotNull(opening);
        assertEquals(third, opening.matching().getLine());
        assertEquals(0, opening.matching().getCharacter());
        assertNotNull(closing);
        assertEquals(first, closing.matching().getLine());
        assertEquals(first.getText().length() - 1, closing.matching().getCharacter());
    }

    @Test
    void ignoresDelimitersInsideLiteralsAndComments() {
        DecompilerLine first = line(1, "call(\")\", '{'); // } ] )");
        DecompilerLine second = line(2, "/* {[( */ finish());");
        DecompilerDelimiterMatcher.Match match = DecompilerDelimiterMatcher.findMatch(
                List.of(first, second), new DecompilerCoordinates(first, 4));

        assertNotNull(match);
        assertEquals(first, match.matching().getLine());
        assertEquals(13, match.matching().getCharacter());
        assertNull(DecompilerDelimiterMatcher.findMatch(
                List.of(first), new DecompilerCoordinates(first, 6)));
    }

    @Test
    void ignoresTextBlockContents() {
        DecompilerLine first = line(1, "String source = \"\"\"");
        DecompilerLine second = line(2, "    if (ignored[0]) {");
        DecompilerLine third = line(3, "    \"\"\";");

        assertNull(DecompilerDelimiterMatcher.findMatch(
                List.of(first, second, third), new DecompilerCoordinates(second, 7)));
    }

    @Test
    void ignoresDelimiterCharactersInsideInjectedComponents() {
        DecompilerLine line = new DecompilerLine(1);
        line.addComponent(text("invoke(", true));
        line.addComponent(text("injected)name", false));
        line.addComponent(text(")", true));

        DecompilerDelimiterMatcher.Match match = DecompilerDelimiterMatcher.findMatch(
                List.of(line), new DecompilerCoordinates(line, 6));

        assertNotNull(match);
        assertEquals(line.getText().length() - 1, match.matching().getCharacter());
        assertNull(DecompilerDelimiterMatcher.findMatch(
                List.of(line), new DecompilerCoordinates(line, 15)));
    }

    private static DecompilerLine line(int number, String text) {
        DecompilerLine line = new DecompilerLine(number);
        line.addComponent(text(text, true));
        return line;
    }

    private static DecompilerLineText text(String text, boolean raw) {
        DecompilerComponent component = new DecompilerComponent(text);
        component.setRawDecompilerText(raw);
        return new DecompilerLineText(text, component);
    }
}
