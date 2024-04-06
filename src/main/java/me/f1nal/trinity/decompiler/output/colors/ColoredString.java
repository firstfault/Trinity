package me.f1nal.trinity.decompiler.output.colors;


import me.f1nal.trinity.util.NameUtil;
import imgui.ImGui;

import java.util.List;

/**
 * The {@link ColoredString} class represents a string of text with an associated color.
 * Instances of this class are immutable, meaning their values cannot be changed after creation.
 * Each {@link ColoredString} object encapsulates a textual content and a corresponding color value.
 */
public class ColoredString {
    private final String text;
    private final int color;

    /**
     * Constructs a new {@link ColoredString} object with the specified text and color.
     *
     * @param text  The textual content of the colored string.
     * @param color An integer representing the color associated with the text.
     */
    public ColoredString(String text, int color) {
        this.text = text;
        this.color = color;
    }

    public static void drawText(List<ColoredString> text) {
        for (int i = 0; i < text.size(); i++) {
            ColoredString detail = text.get(i);
            if (detail.getText().equals("\n")) {
                ImGui.newLine();
                continue;
            }
            if (i != 0) {
                ImGui.sameLine(0.F, 0.F);
            }
            ImGui.textColored(detail.getColor(), NameUtil.cleanNewlines(detail.getText()));
        }
    }

    /**
     * Retrieves the text content of the {@link ColoredString}.
     *
     * @return The text content.
     */
    public String getText() {
        return text;
    }

    /**
     * Retrieves the color value associated with the {@link ColoredString}.
     *
     * @return An integer representing the RGBA color.
     */
    public int getColor() {
        return color;
    }
}
