package me.f1nal.trinity.decompiler.output.colors;

import java.util.function.Supplier;

public class SupplierColoredString extends ColoredString {
    private final Supplier<String> text;

    /**
     * Constructs a new {@link ColoredString} object with the specified text and color.
     *
     * @param text  The textual content of the colored string.
     * @param color An integer representing the color associated with the text.
     */
    public SupplierColoredString(Supplier<String> text, int color) {
        super(null, color);
        this.text = text;
    }

    @Override
    public String getText() {
        return text.get();
    }
}
