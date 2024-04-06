package me.f1nal.trinity.gui.components.filter;

import java.util.Collection;
import java.util.function.Predicate;

public abstract class Filter<T> {
    /**
     * Initializes this filter.
     */
    public abstract void initialize(Collection<T> collection);

    public abstract Predicate<T> filter();

    /**
     * Draws widgets related to this filter.
     * @return If the state was modified and a list refresh is needed, {@code true} needs to be returned.
     */
    public abstract boolean draw();
}
