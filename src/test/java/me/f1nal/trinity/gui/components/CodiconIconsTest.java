package me.f1nal.trinity.gui.components;

import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodiconIconsTest {
    @Test
    void bundledFontContainsEverySemanticIcon() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/fonts/codicon.ttf")) {
            assertNotNull(stream);
            Font font = Font.createFont(Font.TRUETYPE_FONT, stream);

            for (String icon : List.of(
                    CodiconIcons.FILE,
                    CodiconIcons.FOLDER,
                    CodiconIcons.SYMBOL_NAMESPACE,
                    CodiconIcons.SYMBOL_METHOD,
                    CodiconIcons.SYMBOL_ENUM,
                    CodiconIcons.ARCHIVE,
                    CodiconIcons.CASE_SENSITIVE,
                    CodiconIcons.FILE_CODE,
                    CodiconIcons.FOLDER_OPENED,
                    CodiconIcons.MENTION,
                    CodiconIcons.PACKAGE,
                    CodiconIcons.SAVE,
                    CodiconIcons.SYMBOL_CLASS,
                    CodiconIcons.SYMBOL_FIELD,
                    CodiconIcons.SYMBOL_FILE,
                    CodiconIcons.SYMBOL_INTERFACE,
                    CodiconIcons.SYMBOL_KEYWORD)) {
                assertTrue(font.canDisplay(icon.codePointAt(0)), () -> "Missing Codicon glyph " + icon);
            }
        }
    }
}
