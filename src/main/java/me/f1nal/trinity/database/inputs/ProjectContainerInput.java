package me.f1nal.trinity.database.inputs;

import me.f1nal.trinity.database.ClassPath;
import me.f1nal.trinity.execution.packages.ProjectContainerKind;

import java.util.UUID;

/** Binary input assigned to one project container before ASM parsing. */
public final class ProjectContainerInput {
    private final UUID id;
    private final String name;
    private final ProjectContainerKind kind;
    private final ClassPath classPath;

    public ProjectContainerInput(UUID id, String name, ProjectContainerKind kind, ClassPath classPath) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.classPath = classPath;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ProjectContainerKind getKind() {
        return kind;
    }

    public ClassPath getClassPath() {
        return classPath;
    }
}
