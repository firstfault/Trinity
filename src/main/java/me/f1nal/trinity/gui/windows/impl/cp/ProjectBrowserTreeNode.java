package me.f1nal.trinity.gui.windows.impl.cp;

import me.f1nal.trinity.gui.components.tree.GenericTreeNode;

public abstract class ProjectBrowserTreeNode<N extends IBrowserViewerNode> extends GenericTreeNode<ProjectBrowserTreeNode<?>> {
    protected final N node;

    protected ProjectBrowserTreeNode(N node) {
        this.node = node;
    }

    public N getNode() {
        return node;
    }

    public abstract void draw(ProjectBrowserFrame projectBrowserFrame);
}
