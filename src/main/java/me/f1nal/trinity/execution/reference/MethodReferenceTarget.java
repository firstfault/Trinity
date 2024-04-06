package me.f1nal.trinity.execution.reference;

import me.f1nal.trinity.execution.MethodInput;
import org.jetbrains.annotations.Nullable;

public interface MethodReferenceTarget extends ReferenceTarget {
    /**
     * Method that owns this reference.
     * @return The method that calls the {@link MethodReferenceTarget#getReferencedMethod()} method.
     */
    MethodInput getMethod();

    /**
     * Method getting referenced.
     * @return Method that got called by the {@link MethodReferenceTarget#getMethod()} method.
     */
    @Nullable MethodInput getReferencedMethod();
}
