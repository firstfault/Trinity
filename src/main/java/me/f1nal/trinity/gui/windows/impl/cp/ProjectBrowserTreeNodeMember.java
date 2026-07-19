package me.f1nal.trinity.gui.windows.impl.cp;

import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import me.f1nal.trinity.execution.MethodInput;

final class ProjectBrowserTreeNodeMember extends ProjectBrowserTreeNode<ProjectBrowserMemberNode> {
    ProjectBrowserTreeNodeMember(ProjectBrowserMemberNode node) {
        super(node);
    }

    @Override
    public void draw(ProjectBrowserFrame projectBrowserFrame) {
        ImGui.tableNextRow();
        ImGui.tableNextColumn();
        ImGui.treeNodeEx("###Member" + node.getInput(), ImGuiTreeNodeFlags.Leaf
                | ImGuiTreeNodeFlags.NoTreePushOnOpen | ImGuiTreeNodeFlags.SpanFullWidth);
        ImGui.sameLine();
        node.getBrowserViewerNode().draw();
        ImGui.tableNextColumn();
        if (node.getInput() instanceof MethodInput) {
            ImGui.textUnformatted(String.valueOf(((MethodInput) node.getInput()).getInstructions().size()));
        } else {
            ImGui.textDisabled("--");
        }
        ImGui.tableNextColumn();
        ImGui.textUnformatted(node.getInput() instanceof MethodInput ? "Method" : "Field");
    }
}
