package me.f1nal.trinity.gui.windows.impl.cp;

import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import me.f1nal.trinity.execution.packages.Package;

public class ProjectBrowserTreeNodePackage extends ProjectBrowserTreeNode<Package> {
    public ProjectBrowserTreeNodePackage(Package pkg) {
        super(pkg);
    }

    @Override
    public void draw(ProjectBrowserFrame projectBrowserFrame) {
        boolean searching = !projectBrowserFrame.getSearch().isEmpty();
        ImGui.setNextItemOpen((node.isOpen() && (node.getParent() == null || node.getParent().isOpen())) || searching);
        boolean open = ImGui.treeNodeEx("###" + node.getInternalPath(), ImGuiTreeNodeFlags.SpanFullWidth);
        ImGui.sameLine(0.F, 0.F);

        node.getBrowserViewerNode().draw();

        if (!searching && node.isOpen() != open) {
            node.setOpen(open);
            node.save();
        }

        if (open && projectBrowserFrame.getTrinity().getExecution().isClassesLoaded()) {
            for (ProjectBrowserTreeNode<?> child : this.getChildren()) {
                child.draw(projectBrowserFrame);
            }
        }

        if (open) ImGui.treePop();
    }
}
