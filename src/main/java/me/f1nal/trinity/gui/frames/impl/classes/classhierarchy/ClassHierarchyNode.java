package me.f1nal.trinity.gui.frames.impl.classes.classhierarchy;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.gui.components.tree.TreeNode;

public class ClassHierarchyNode {
    private final ClassHierarchyNodeType type;
    private final ClassInput classInput;
    private final long id = ++idCounter;

    public ClassHierarchyNode(ClassHierarchyNodeType type, ClassInput classInput) {
        this.type = type;
        this.classInput = classInput;
    }

    public ClassHierarchyNodeType getType() {
        return type;
    }

    public ClassInput getClassInput() {
        return classInput;
    }

    public long getId() {
        return id;
    }

    private static long idCounter;
}
