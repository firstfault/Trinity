package me.f1nal.trinity.refactor.identity;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MethodInput;

import java.util.Objects;

/** A stable description of one requested JVM identity rename. */
public record IdentityRefactorRequest(
        IdentityRefactorKind kind,
        String owner,
        String name,
        String descriptor,
        String newName,
        boolean applyingDisplayName) {

    public IdentityRefactorRequest {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(newName, "newName");
        if (kind != IdentityRefactorKind.CLASS) {
            Objects.requireNonNull(descriptor, "descriptor");
        }
    }

    public static IdentityRefactorRequest forInput(
            Input<?> input, String newName, boolean applyingDisplayName) {
        Objects.requireNonNull(input, "input");
        if (input instanceof ClassInput classInput) {
            String currentName = classInput.getNode().name;
            return new IdentityRefactorRequest(IdentityRefactorKind.CLASS,
                    currentName, currentName, null,
                    newName, applyingDisplayName);
        }
        if (input instanceof MethodInput methodInput) {
            return new IdentityRefactorRequest(IdentityRefactorKind.METHOD,
                    methodInput.getOwningClass().getNode().name,
                    methodInput.getNode().name, methodInput.getNode().desc,
                    newName, applyingDisplayName);
        }
        if (input instanceof FieldInput fieldInput) {
            return new IdentityRefactorRequest(IdentityRefactorKind.FIELD,
                    fieldInput.getOwningClass().getNode().name,
                    fieldInput.getNode().name, fieldInput.getNode().desc,
                    newName, applyingDisplayName);
        }
        throw new IllegalArgumentException("Only JVM classes, methods, and fields can be refactored");
    }

    public static String currentName(Input<?> input) {
        Objects.requireNonNull(input, "input");
        if (input instanceof ClassInput classInput) return classInput.getNode().name;
        if (input instanceof MethodInput methodInput) return methodInput.getNode().name;
        if (input instanceof FieldInput fieldInput) return fieldInput.getNode().name;
        throw new IllegalArgumentException("Only JVM classes, methods, and fields can be refactored");
    }

    public String oldIdentity() {
        return kind == IdentityRefactorKind.CLASS
                ? owner
                : owner + '.' + name + descriptor;
    }

    public String proposedIdentity() {
        return switch (kind) {
            case CLASS -> newName;
            case METHOD, FIELD -> owner + '.' + newName + descriptor;
        };
    }
}
