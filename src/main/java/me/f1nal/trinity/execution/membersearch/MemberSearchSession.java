package me.f1nal.trinity.execution.membersearch;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.Input;

import java.util.ArrayList;
import java.util.List;

/** A render-thread search job that yields between candidates to keep ImGui responsive. */
public final class MemberSearchSession {
    private final MemberSearchQuery query;
    private final MemberSearchEngine engine;
    private final List<Input<?>> candidates;
    private final List<MemberSearchResult> results = new ArrayList<>();
    private int candidateIndex;
    private boolean cancelled;

    public MemberSearchSession(Trinity trinity, MemberSearchQuery query) {
        this.query = query;
        this.engine = new MemberSearchEngine(trinity);
        List<String> errors = engine.validate(query);
        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join("; ", errors));
        this.candidates = engine.candidates(query.target());
    }

    public void advance(long budgetNanos) {
        if (isFinished()) return;
        long deadline = System.nanoTime() + Math.max(100_000L, budgetNanos);
        do {
            MemberSearchResult result = engine.evaluate(query, candidates.get(candidateIndex++));
            if (result != null) results.add(result);
        } while (candidateIndex < candidates.size() && System.nanoTime() < deadline && !cancelled);
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isFinished() {
        return cancelled || candidateIndex >= candidates.size();
    }

    public float progress() {
        return candidates.isEmpty() ? 1.F : (float) candidateIndex / candidates.size();
    }

    public int searchedCount() {
        return candidateIndex;
    }

    public int candidateCount() {
        return candidates.size();
    }

    public int unresolvedHierarchyComparisons() {
        return engine.unresolvedHierarchyComparisons();
    }

    public List<MemberSearchResult> results() {
        return List.copyOf(results);
    }

    public MemberSearchQuery query() {
        return query;
    }
}
