package me.f1nal.trinity.execution.hierarchy;

import me.f1nal.trinity.execution.MethodInput;

import java.util.ArrayList;
import java.util.List;

public class MethodHierarchy {
    /**
     * Methods linked to this hierarchy.
     */
    private final List<MethodInput> linkedMethods = new ArrayList<>();

    public List<MethodInput> getLinkedMethods() {
        return linkedMethods;
    }

    public void linkMethod(MethodInput methodInput) {
        this.linkedMethods.add(methodInput);
        methodInput.setMethodHierarchy(this);
    }
}
