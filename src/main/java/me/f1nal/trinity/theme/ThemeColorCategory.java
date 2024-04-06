package me.f1nal.trinity.theme;

import me.f1nal.trinity.util.INameable;

public enum ThemeColorCategory implements INameable {
    CODE_EDITOR("Code Editor"),
    ASSEMBLER("Assembler"),
    XREF_KIND("Xref Kind"),
    FILE_KIND("File Kind"),
    FRAME("Frame"),
    ;

    private final String name;

    ThemeColorCategory(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
