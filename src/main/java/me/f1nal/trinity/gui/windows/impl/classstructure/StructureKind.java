package me.f1nal.trinity.gui.windows.impl.classstructure;

import me.f1nal.trinity.gui.components.filter.kind.IKindType;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.function.Supplier;

public enum StructureKind implements IKindType {
    CLASSES("Class", () -> CodeColorScheme.CLASS_REF),
    FIELD("Field", () -> CodeColorScheme.FIELD_REF),
    METHOD("Method", () -> CodeColorScheme.METHOD_REF),
    ;

    private final String name;
    private final Supplier<Integer> color;

    StructureKind(String name, Supplier<Integer> color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public int getColor() {
        return color.get();
    }

    @Override
    public String getName() {
        return this.name;
    }
}
