package me.f1nal.trinity.refactor.identity;

import java.util.Objects;

/** A condition that should be shown before a potentially unsafe refactor is applied. */
public record IdentityRefactorIssue(
        IdentityRefactorSeverity severity,
        String title,
        String detail) {

    public IdentityRefactorIssue {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(detail, "detail");
    }
}
