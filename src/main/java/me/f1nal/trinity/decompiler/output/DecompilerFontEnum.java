package me.f1nal.trinity.decompiler.output;

import imgui.ImFont;
import me.f1nal.trinity.util.INameable;

public enum DecompilerFontEnum implements INameable {
    JETBRAINS_MONO("JetBrains Mono"),
    ZED_MONO("Zed Mono"),
    INTER("Inter"),
    ;

    private final String name;
    private ImFont font;

    DecompilerFontEnum(String name) {
        this.name = name;
    }

    public void setFont(ImFont font) {
        this.font = font;
    }

    public ImFont getFont() {
        return font;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
