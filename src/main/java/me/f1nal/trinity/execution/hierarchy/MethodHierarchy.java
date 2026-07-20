package me.f1nal.trinity.execution.hierarchy;

import me.f1nal.trinity.execution.MethodInput;

import java.util.LinkedHashSet;
import java.util.Set;

public final class MethodHierarchy {
    /**
     * Methods linked to this hierarchy.
     */
    private final Set<MethodInput> linkedMethods = new LinkedHashSet<>();

    public Set<MethodInput> getLinkedMethods() {
        return linkedMethods;
    }

    public void linkMethod(MethodInput methodInput) {
        MethodHierarchy existing = methodInput.getMethodHierarchy();
        if (existing == this) {
            return;
        }
        if (existing != null) {
            for (MethodInput linkedMethod : Set.copyOf(existing.linkedMethods)) {
                this.updateMethodHierarchy(linkedMethod);
            }
            return;
        }
        this.updateMethodHierarchy(methodInput);
    }

    private void updateMethodHierarchy(MethodInput methodInput) {
        this.linkedMethods.add(methodInput);
        methodInput.setMethodHierarchy(this);
    }

}
