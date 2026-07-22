package me.f1nal.trinity.execution.pattern;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.MethodInput;

import java.util.ArrayList;
import java.util.List;

/** A render-thread search job that yields between methods to keep ImGui responsive. */
public final class PatternSearchSession {
    private final InstructionPattern pattern;
    private final List<MethodInput> methods;
    private final int retainedResultLimit;
    private final List<InstructionPatternMatch> results = new ArrayList<>();
    private int methodIndex;
    private long matchCount;
    private boolean cancelled;

    public PatternSearchSession(Trinity trinity, InstructionPattern pattern, int retainedResultLimit) {
        this.pattern = pattern;
        this.retainedResultLimit = Math.max(1, retainedResultLimit);
        this.methods = trinity.getExecution().getClassList().stream()
                .flatMap(input -> input.getMethodMap().values().stream())
                .filter(method -> method.getOwningClass().getDeclaredMethod(
                        method.getName(), method.getDescriptor()) == method)
                .toList();
    }

    public void advance(long budgetNanos) {
        if (isFinished()) return;
        long deadline = System.nanoTime() + Math.max(100_000L, budgetNanos);
        do {
            MethodInput method = methods.get(methodIndex++);
            List<InstructionPatternMatch> found = InstructionPatternMatcher.findAll(method, pattern);
            matchCount += found.size();
            int remaining = retainedResultLimit - results.size();
            if (remaining > 0) results.addAll(found.subList(0, Math.min(remaining, found.size())));
        } while (methodIndex < methods.size() && System.nanoTime() < deadline && !cancelled);
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isFinished() {
        return cancelled || methodIndex >= methods.size();
    }

    public float progress() {
        return methods.isEmpty() ? 1.F : (float) methodIndex / methods.size();
    }

    public int methodsSearched() {
        return methodIndex;
    }

    public int methodCount() {
        return methods.size();
    }

    public long matchCount() {
        return matchCount;
    }

    public List<InstructionPatternMatch> results() {
        return List.copyOf(results);
    }

    public InstructionPattern pattern() {
        return pattern;
    }
}
