package me.f1nal.trinity.decompiler;

import me.f1nal.trinity.gui.components.filter.kind.IKindType;

import java.awt.Color;

import static me.f1nal.trinity.theme.CodeColorScheme.getRgb;

public enum DecompilerVariableAccess implements IKindType {
    READ("Read", getRgb(new Color(158, 158, 158))),
    WRITE("Write", getRgb(new Color(195, 195, 195))),
    READ_WRITE("Read / Write", getRgb(new Color(225, 225, 225)));

    private final String name;
    private final int color;

    DecompilerVariableAccess(String name, int color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getColor() {
        return color;
    }
}
