package me.f1nal.trinity.gui.windows.impl.cp;

import imgui.ImGui;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.tree.GenericTreeNode;
import me.f1nal.trinity.util.GuiUtil;
import me.f1nal.trinity.util.SystemUtil;

public abstract class ProjectBrowserTreeNode<N extends IBrowserViewerNode> extends GenericTreeNode<ProjectBrowserTreeNode<?>> {
    protected final N node;

    protected ProjectBrowserTreeNode(N node) {
        this.node = node;
    }

    public N getNode() {
        return node;
    }

    public abstract void draw(ProjectBrowserFrame projectBrowserFrame);

    protected final void drawSize(String sizeText, long sizeInBytes) {
        ImGui.text(sizeText);
        GuiUtil.tooltip(sizeInBytes + "B");
        if (ImGui.isItemClicked(1)) {
            Main.getDisplayManager().getPopupMenu().show(PopupItemBuilder.create().menuItem("Copy Size", () -> SystemUtil.copyToClipboard(String.valueOf(sizeInBytes))));
        }
    }
}
