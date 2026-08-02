package me.f1nal.trinity.execution.xref;

import me.f1nal.trinity.gui.components.filter.kind.IDetailedKindType;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.List;
import java.util.function.Supplier;

public enum XrefKind implements IDetailedKindType {
    TYPE("Type", "General class references from declarations and bytecode.",
            () -> CodeColorScheme.XREF_TYPE,
            "Field type", "New", "New (Array)", "New (Multi Array)", "Cast",
            "Instance Of", "Invoke (Dynamic)", "ConstantDynamic type", "Handle descriptor"),
    INVOKE("Invoke", "Method calls and method handles.",
            () -> CodeColorScheme.XREF_INVOKE,
            "Invoke (Virtual)", "Invoke (Special)", "Invoke (Static)",
            "Invoke (Interface)", "Invoke (Constructor)"),
    FIELD("Field", "Field reads, writes, and field handles.",
            () -> CodeColorScheme.XREF_FIELD,
            "Field (Get)", "Field (Put)", "Static (Get)", "Static (Put)"),
    DESCRIPTOR("Descriptor", "Types used by call and field access descriptors.",
            () -> CodeColorScheme.XREF_DESCRIPTOR,
            "Invocation descriptor", "Field descriptor"),
    METADATA("Metadata", "Types referenced by signatures and class metadata.",
            () -> CodeColorScheme.XREF_METADATA,
            "Class signature", "Field signature", "Method signature", "Enclosing class",
            "Enclosing method", "Enclosing method descriptor", "Nest host", "Nest member",
            "Permitted subclass", "Inner class metadata", "Inner class owner",
            "Module main class", "Module service use", "Module service",
            "Module service provider", "Record component type", "Record component signature"),
    INHERIT("Inherit", "Classes and interfaces extended or implemented.",
            () -> CodeColorScheme.XREF_INHERIT,
            "Extends", "Implements"),
    RETURN("Return", "Types returned by methods.",
            () -> CodeColorScheme.XREF_RETURN,
            "Returns"),
    PARAMETER("Parameter", "Types used as method parameters.",
            () -> CodeColorScheme.XREF_PARAMETER,
            "Parameter"),
    VARIABLE("Variable", "Types used by local variables.",
            () -> CodeColorScheme.XREF_VARIABLE,
            "Variable"),
    STACK_FRAME("Stack Frame", "Types recorded in stack map frames.",
            () -> CodeColorScheme.XREF_STACK_FRAME,
            "Stack frame"),
    ANNOTATION("Annotation", "Annotation types and enum values.",
            () -> CodeColorScheme.XREF_ANNOTATION,
            "Annotation", "Annotation enum", "Enum constant"),
    EXCEPTION("Exception", "Exception types that are thrown or caught.",
            () -> CodeColorScheme.XREF_EXCEPTION,
            "Throws", "Catch"),
    LITERAL("Literal", "Constants used by fields, instructions, and bootstrap data.",
            () -> CodeColorScheme.XREF_LITERAL,
            ".class"),
    ;

    private final String name;
    private final String description;
    private final Supplier<Integer> color;
    private final List<String> typeNames;

    XrefKind(String name, String description, Supplier<Integer> color, String... typeNames) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.typeNames = List.of(typeNames);
    }

    @Override
    public int getColor() {
        return color.get();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public List<String> getTypeNames() {
        return this.typeNames;
    }

    @Override
    public boolean matchesTypeName(String listedType, String resultType) {
        if (this == ANNOTATION && listedType.equals("Enum constant") && resultType != null) {
            return !resultType.equals("Annotation") && !resultType.equals("Annotation enum");
        }
        return IDetailedKindType.super.matchesTypeName(listedType, resultType);
    }
}
