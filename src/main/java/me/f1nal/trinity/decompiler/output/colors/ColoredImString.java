package me.f1nal.trinity.decompiler.output.colors;

import imgui.type.ImString;

public class ColoredImString extends SupplierColoredString {
    /**
     * Constructs a new {@link ColoredString} object with the specified text and color.
     *
     * @param text  The textual content of the colored string.
     * @param color An integer representing the color associated with the text.
     */
    public ColoredImString(ImString text, int color) {
        super(() -> text.get(), color);
    }
}
