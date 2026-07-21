package me.f1nal.trinity.gui.windows.impl.cp;

import imgui.ImFont;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import me.f1nal.trinity.execution.packages.ArchiveEntry;
import me.f1nal.trinity.theme.CodeColorScheme;

public class ProjectBrowserTreeNodeEntry extends ProjectBrowserTreeNode<ArchiveEntry> {
    private static final float SIZE_RIGHT_PADDING = 8.F;
    private static final float SIZE_LABEL_GAP = 8.F;
    private static final float SIZE_FONT_REDUCTION = 2.F;

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
        projectBrowserFrame.drawEntryDragSource(this.node);
        projectBrowserFrame.drawPackageDropTarget(this.node.getPackage());
        float rowMinY = ImGui.getItemRectMinY();
        float rowMaxY = ImGui.getItemRectMaxY();
        SizeLayout sizeLayout = this.getSizeLayout();
        ImGui.sameLine(0.F, 0.F);
        node.getBrowserViewerNode().draw(null, sizeLayout.x() - SIZE_LABEL_GAP);
        this.drawSize(sizeLayout, rowMinY, rowMaxY);

        if (open && !this.isLeaf()) {
            for (ProjectBrowserTreeNode<?> child : this.getChildren()) {
                child.draw(projectBrowserFrame);
            }
            ImGui.treePop();
        }
    }

    private SizeLayout getSizeLayout() {
        String size = node.getSize();
        ImFont font = ImGui.getFont();
        int fontSize = Math.max(10, Math.round(ImGui.getFontSize() - SIZE_FONT_REDUCTION));
        float textWidth = font.calcTextSizeA(fontSize, Float.MAX_VALUE, -1.F, size).x;
        float right = ImGui.getWindowPosX() + ImGui.getWindowContentRegionMax().x - SIZE_RIGHT_PADDING;
        return new SizeLayout(size, font, fontSize, right - textWidth);
    }

    private void drawSize(SizeLayout layout, float rowMinY, float rowMaxY) {
        float y = rowMinY + (rowMaxY - rowMinY - layout.fontSize()) * 0.5F;
        ImGui.getWindowDrawList().addText(layout.font(), layout.fontSize(), layout.x(), y,
                CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 190), layout.text());
    }

    private record SizeLayout(String text, ImFont font, int fontSize, float x) {
    }
}
