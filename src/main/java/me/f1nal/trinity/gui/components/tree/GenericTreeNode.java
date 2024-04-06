package me.f1nal.trinity.gui.components.tree;

import java.util.ArrayList;
import java.util.List;

public class GenericTreeNode<T extends GenericTreeNode<?>> {
    private final List<T> children;

    public GenericTreeNode() {
        this.children = new ArrayList<>();
    }

    public boolean isLeaf() {
        return this.children.isEmpty();
    }

    public void addChild(T treeNode) {
        this.children.add(treeNode);
    }

    public List<T> getChildren() {
        return children;
    }

    private void addChildrenRecursively(GenericTreeNode<?> child, List<GenericTreeNode<?>> list) {
        list.add(child);
        for (GenericTreeNode<?> c : child.getChildren()) {
            this.addChildrenRecursively(c, list);
        }
    }

    public List<T> getAllChildren() {
        List<T> list = new ArrayList<>();
        //noinspection unchecked
        this.addChildrenRecursively(this, (List<GenericTreeNode<?>>) list);
        return list;
    }
}
