package me.f1nal.trinity.refactor.identity;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable analyzed refactor, including the project state against which it was built. */
public final class IdentityRefactorPlan {
    private final IdentityRefactorRequest request;
    private final List<IdentityRefactorChange> changes;
    private final List<IdentityRefactorIssue> issues;
    private final IdentityMapping mapping;
    private final IdentityProjectState expectedState;
    private final Set<ClassInput> affectedClasses;

    IdentityRefactorPlan(IdentityRefactorRequest request,
                         List<IdentityRefactorChange> changes,
                         List<IdentityRefactorIssue> issues,
                         IdentityMapping mapping,
                         IdentityProjectState expectedState,
                         Set<ClassInput> affectedClasses) {
        this.request = Objects.requireNonNull(request, "request");
        this.changes = List.copyOf(changes);
        this.issues = List.copyOf(issues);
        this.mapping = Objects.requireNonNull(mapping, "mapping");
        this.expectedState = Objects.requireNonNull(expectedState, "expectedState");
        this.affectedClasses = Set.copyOf(affectedClasses);
    }

    public IdentityRefactorRequest getRequest() {
        return request;
    }

    public List<IdentityRefactorChange> getChanges() {
        return changes;
    }

    public List<IdentityRefactorIssue> getIssues() {
        return issues;
    }

    public Set<ClassInput> getAffectedClasses() {
        return affectedClasses;
    }

    public long getConflictCount() {
        return issues.stream().filter(issue ->
                issue.severity() == IdentityRefactorSeverity.CONFLICT).count();
    }

    public long getWarningCount() {
        return issues.stream().filter(issue ->
                issue.severity() == IdentityRefactorSeverity.WARNING).count();
    }

    public boolean hasConflicts() {
        return getConflictCount() != 0;
    }

    /** A clean plan can be applied without interrupting the user with a review dialog. */
    public boolean requiresReview() {
        return hasConflicts() || getWarningCount() != 0;
    }

    public boolean isProjectStateCurrent() {
        return expectedState.isCurrent();
    }

    boolean belongsTo(Trinity trinity) {
        return expectedState.belongsTo(trinity);
    }

    IdentityMapping mapping() {
        return mapping;
    }
}
