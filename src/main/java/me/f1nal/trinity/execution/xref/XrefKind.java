package me.f1nal.trinity.execution.xref;

import me.f1nal.trinity.gui.components.filter.kind.IKindType;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.function.Supplier;

public enum XrefKind implements IKindType {
    TYPE("Type", () -> CodeColorScheme.XREF_TYPE),
    INVOKE("Invoke", () -> CodeColorScheme.XREF_INVOKE),
    FIELD("Field", () -> CodeColorScheme.XREF_FIELD),
    INHERIT("Inherit", () -> CodeColorScheme.XREF_INHERIT),
    RETURN("Return", () -> CodeColorScheme.XREF_RETURN),
    PARAMETER("Parameter", () -> CodeColorScheme.XREF_PARAMETER),
    ANNOTATION("Annotation", () -> CodeColorScheme.XREF_ANNOTATION),
    EXCEPTION("Exception", () -> CodeColorScheme.XREF_EXCEPTION),
    LITERAL("Literal", () -> CodeColorScheme.XREF_LITERAL),
    ;

    private final String name;
    private final Supplier<Integer> color;

    XrefKind(String name, Supplier<Integer> color) {
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
