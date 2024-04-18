package me.f1nal.trinity.gui.windows.impl.cp;

import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import me.f1nal.trinity.execution.packages.ArchiveEntry;

public class ProjectBrowserTreeNodeEntry extends ProjectBrowserTreeNode<ArchiveEntry> {
    public ProjectBrowserTreeNodeEntry(ArchiveEntry archiveEntry) {
        super(archiveEntry);
    }
    
    @Override
    public void draw(ProjectBrowserFrame projectBrowserFrame) {
        ImGui.tableNextRow();
        ImGui.tableNextColumn();

        ImGui.treeNodeEx("", ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen |
                ImGuiTreeNodeFlags.SpanFullWidth);
        ImGui.sameLine();
        node.getBrowserViewerNode().draw();

        ImGui.tableNextColumn();
        this.drawSize(node.getSize(), node.getSizeInBytes());
        ImGui.tableNextColumn();
        ImGui.textUnformatted(node.getArchiveEntryTypeName());
    }
}
