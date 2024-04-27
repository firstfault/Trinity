package me.f1nal.trinity.gui.windows.impl.cp;

import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.util.ByteUtil;

public class ProjectBrowserTreeNodePackage extends ProjectBrowserTreeNode<Package> {
    public ProjectBrowserTreeNodePackage(Package pkg) {
        super(pkg);
    }

    @Override
    public void draw(ProjectBrowserFrame projectBrowserFrame) {
        final Trinity trinity = projectBrowserFrame.getTrinity();

        ImGui.tableNextRow();
        ImGui.tableNextColumn();
        boolean searching = !projectBrowserFrame.getSearch().isEmpty();
        ImGui.setNextItemOpen((node.isOpen() && (node.getParent() == null || node.getParent().isOpen())) || searching);
        boolean open = ImGui.treeNodeEx("###" + node.getInternalPath(), ImGuiTreeNodeFlags.SpanFullWidth);
        ImGui.sameLine();

        node.getBrowserViewerNode().draw();

        ImGui.tableNextColumn();
        if (node.isArchive() && trinity.getDatabase().getDatabaseSize() != 0L) {
            this.drawSize(ByteUtil.getHumanReadableByteCountSI(trinity.getDatabase().getDatabaseSize()), trinity.getDatabase().getDatabaseSize());
        } else {
            ImGui.textDisabled("--");
        }
        ImGui.tableNextColumn();
        ImGui.textUnformatted(node.isArchive() ? "Project" : "Folder");

        if (!searching && node.isOpen() != open) {
            node.setOpen(open);
            node.save();
        }

        if (open && trinity.getExecution().isClassesLoaded()) {
            for (ProjectBrowserTreeNode<?> child : this.getChildren()) {
                child.draw(projectBrowserFrame);
            }
        }

        if (open) ImGui.treePop();
    }
}
