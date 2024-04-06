package me.f1nal.trinity.events;

import me.f1nal.trinity.decompiler.DecompiledClass;

import java.util.function.Predicate;

public class EventRefreshDecompilerText {
    private final Predicate<DecompiledClass> predicate;

    public EventRefreshDecompilerText(Predicate<DecompiledClass> predicate) {
        this.predicate = predicate;
    }

    public Predicate<DecompiledClass> getPredicate() {
        return predicate;
    }
}
