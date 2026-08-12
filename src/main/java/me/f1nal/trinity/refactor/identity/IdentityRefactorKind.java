package me.f1nal.trinity.refactor.identity;

/** The kind of JVM symbol whose binary identity is being changed. */
public enum IdentityRefactorKind {
    CLASS("Class"),
    METHOD("Method"),
    FIELD("Field");

    private final String displayName;

    IdentityRefactorKind(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
