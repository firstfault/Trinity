package me.f1nal.trinity.gui.windows.impl.cp;

import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import me.f1nal.trinity.execution.packages.Package;

import java.util.List;

public class ProjectBrowserTreeNodePackage extends ProjectBrowserTreeNode<Package> {
    private final List<Package> packageChain;
    private final String displayName;

    public ProjectBrowserTreeNodePackage(Package pkg) {
        this(List.of(pkg));
    }

    public ProjectBrowserTreeNodePackage(List<Package> packageChain) {
        super(lastPackage(packageChain));
        this.packageChain = List.copyOf(packageChain);
        Package last = lastPackage(this.packageChain);
        this.displayName = last.isArchive() && last.getContainer() != null
                ? last.getContainer().getName()
                : this.packageChain.stream()
                    .map(Package::getName)
                    .reduce((left, right) -> left + "/" + right)
                    .orElseThrow();
    }

    private static Package lastPackage(List<Package> packageChain) {
        if (packageChain.isEmpty()) throw new IllegalArgumentException("Package chain cannot be empty");
        return packageChain.get(packageChain.size() - 1);
    }

    @Override
    public void draw(ProjectBrowserFrame projectBrowserFrame) {
        boolean searching = !projectBrowserFrame.getSearch().isEmpty();
        ImGui.setNextItemOpen((this.isOpen() && (node.getParent() == null || node.getParent().isOpen())) || searching);
        String containerId = node.getContainer() == null ? "legacy" : node.getContainer().getId().toString();
        boolean open = ImGui.treeNodeEx("###" + containerId + ":" + node.getInternalPath(), ImGuiTreeNodeFlags.SpanFullWidth);
        projectBrowserFrame.drawPackageDropTarget(this.node);
        ImGui.sameLine(0.F, 0.F);

        node.getBrowserViewerNode().draw(node.isArchive() && node.getContainer() != null
                ? node.getContainer().getName() : this.displayName);

        if (!searching && this.isOpen() != open) {
            this.setOpen(open);
        }

        if (open && projectBrowserFrame.getTrinity().getExecution().isClassesLoaded()) {
            for (ProjectBrowserTreeNode<?> child : this.getChildren()) {
                child.draw(projectBrowserFrame);
            }
        }

        if (open) ImGui.treePop();
    }

    private boolean isOpen() {
        return this.packageChain.stream().allMatch(Package::isOpen);
    }

    private void setOpen(boolean open) {
        if (open) {
            for (Package pkg : this.packageChain) {
                pkg.setOpen(true);
                pkg.save();
            }
        } else {
            for (int i = this.packageChain.size() - 1; i >= 0; i--) {
                Package pkg = this.packageChain.get(i);
                pkg.setOpen(false);
                pkg.save();
            }
        }
    }
}
