package me.f1nal.trinity.execution.hierarchy;

import me.f1nal.trinity.execution.MethodInput;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MethodHierarchy {
    /**
     * Methods linked to this hierarchy.
     */
    private final Set<MethodInput> linkedMethods = new HashSet<>();

    public Set<MethodInput> getLinkedMethods() {
        return linkedMethods;
    }

    public void linkMethod(MethodInput methodInput) {
        if (methodInput.getMethodHierarchy() != null) {
            methodInput.getMethodHierarchy().mergeInto(this);
            return;
        }
        this.updateMethodHierarchy(methodInput);
    }

    private void updateMethodHierarchy(MethodInput methodInput) {
        this.linkedMethods.add(methodInput);
        methodInput.setMethodHierarchy(this);
    }

    private void mergeInto(MethodHierarchy methodHierarchy) {
        for (MethodInput linkedMethod : methodHierarchy.getLinkedMethods()) {
            this.updateMethodHierarchy(linkedMethod);
        }
    }
}
