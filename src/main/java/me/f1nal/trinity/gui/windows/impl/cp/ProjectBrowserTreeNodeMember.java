package me.f1nal.trinity.gui.windows.impl.cp;

import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;

final class ProjectBrowserTreeNodeMember extends ProjectBrowserTreeNode<ProjectBrowserMemberNode> {
    ProjectBrowserTreeNodeMember(ProjectBrowserMemberNode node) {
        super(node);
    }

    @Override
    public void draw(ProjectBrowserFrame projectBrowserFrame) {
        ImGui.treeNodeEx("###Member" + node.getInput(), ImGuiTreeNodeFlags.Leaf
                | ImGuiTreeNodeFlags.NoTreePushOnOpen | ImGuiTreeNodeFlags.SpanFullWidth);
        ImGui.sameLine(0.F, 0.F);
        node.getBrowserViewerNode().draw();
    }
}
