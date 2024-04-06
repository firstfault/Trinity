package me.f1nal.trinity.gui.frames.impl.cp;

import me.f1nal.trinity.gui.components.filter.kind.IKindType;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.function.Supplier;

public enum FileKind implements IKindType {
    CLASSES("Classes", "Class", () -> CodeColorScheme.CLASS_REF),
    ABSTRACT("Abstract", "Abstract Class", () -> CodeColorScheme.FILE_ABSTRACT),
    INTERFACES("Interface", "Interface", () -> CodeColorScheme.FILE_INTERFACE),
    ENUM("Enum", "Enum Class", () -> CodeColorScheme.FILE_ENUM),
    ANNOTATION("Annotation", "Annotation Class", () -> CodeColorScheme.XREF_ANNOTATION),
    RESOURCE("Resource", "Resource File", () -> CodeColorScheme.FILE_RESOURCE),
    ;

    private final String name;
    private final String fileType;
    private final Supplier<Integer> color;

    FileKind(String name, String fileType, Supplier<Integer> color) {
        this.name = name;
        this.fileType = fileType;
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

    public String getFileType() {
        return fileType;
    }
}
