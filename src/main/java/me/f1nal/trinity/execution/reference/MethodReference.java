package me.f1nal.trinity.execution.reference;

import me.f1nal.trinity.execution.MethodInput;
import org.jetbrains.annotations.Nullable;

public final class MethodReference implements MethodReferenceTarget {
    /**
     * Method that calls.
     */
    private final MethodInput method;
    /**
     * Method that is getting called.
     */
    private final MethodInput referenced;

    public MethodReference(MethodInput method, MethodInput referenced) {
        this.method = method;
        this.referenced = referenced;
    }

    @Override
    public MethodInput getMethod() {
        return this.method;
    }

    @Override
    public @Nullable MethodInput getReferencedMethod() {
        return this.referenced;
    }
}
