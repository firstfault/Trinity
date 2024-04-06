package me.f1nal.trinity.execution.reference;

import me.f1nal.trinity.execution.MethodInput;
import org.jetbrains.annotations.Nullable;

public final class LibraryMethodReference implements MethodReferenceTarget {
    private final MethodInput method;
    private final String owner, name, desc;

    public LibraryMethodReference(MethodInput method, String owner, String name, String desc) {
        this.method = method;
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    @Override
    public MethodInput getMethod() {
        return this.method;
    }

    @Override
    public @Nullable MethodInput getReferencedMethod() {
        return null;
    }
}
