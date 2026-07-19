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
        int flags = ImGuiTreeNodeFlags.SpanFullWidth;
        if (this.isLeaf()) {
            flags |= ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen;
        }
        boolean open = ImGui.treeNodeEx("###Entry" + node.getRealName(), flags);
        ImGui.sameLine(0.F, 0.F);
        node.getBrowserViewerNode().draw();

        if (open && !this.isLeaf()) {
            for (ProjectBrowserTreeNode<?> child : this.getChildren()) {
                child.draw(projectBrowserFrame);
            }
            ImGui.treePop();
        }
    }
}
