package me.f1nal.trinity.gui.windows.impl.classstructure;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.MemorableCheckboxComponent;
import me.f1nal.trinity.gui.components.filter.ListFilterComponent;
import me.f1nal.trinity.gui.components.filter.SearchBarFilter;
import me.f1nal.trinity.gui.components.filter.kind.IKindType;
import me.f1nal.trinity.gui.components.filter.kind.KindFilter;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.popup.PopupMenuBar;
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.gui.windows.impl.classstructure.nodes.ClassStructureNode;

import java.awt.event.KeyEvent;

public class ClassStructureWindow extends StaticWindow {
    private static final MemorableCheckboxComponent showFilter = new MemorableCheckboxComponent("classStructureShowFilter", false);
    private ClassStructure classStructure;
    private ListFilterComponent<ClassStructureNode> filterComponent;
    private final KindFilter<ClassStructureNode> kindFilter = new KindFilter<>(StructureKind.values());
    private final SearchBarFilter<ClassStructureNode> searchBarFilter = new SearchBarFilter<>();
    private final PopupMenuBar popupMenuBar = new PopupMenuBar(PopupItemBuilder.create());

    public ClassStructureWindow(Trinity trinity) {
        super("Class Structure", 600, 400, trinity);
        this.kindFilter.setExclude(new IKindType[]{StructureKind.CLASSES});
        this.windowFlags |= ImGuiWindowFlags.MenuBar;
        this.windowFlags |= ImGuiWindowFlags.HorizontalScrollbar;
    }

    public void setClassStructure(ClassStructure classStructure) {
        this.classStructure = classStructure;
        this.filterComponent = new ListFilterComponent<>(classStructure.getRootNode().getAllChildren(), this.searchBarFilter, this.kindFilter);
    }

    public ClassStructure getClassStructure() {
        return classStructure;
    }

    @Override
    public void render() {
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.F, ImGui.getStyle().getWindowPaddingY());
        super.render();
        ImGui.popStyleVar();
    }

    @Override
    protected void renderFrame() {
        this.popupMenuBar.set(PopupItemBuilder.create().menu("Find", find -> find.menuItem("Show Filter", "Ctrl+F", showFilter.getState(), showFilter::toggleState)));
        this.popupMenuBar.draw();

        if (ImGui.isWindowFocused() && ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(KeyEvent.VK_F)) {
            showFilter.toggleState();
        }

        final ClassStructure structure = this.getClassStructure();

        if (structure == null) {
            return;
        }

        if (showFilter.getState()) {
            this.filterComponent.draw();

            ImGui.separator();
        }

        ImGui.pushStyleVar(ImGuiStyleVar.CellPadding, 0.F, 4.F);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, ImGui.getStyle().getItemSpacingX(), 8.F);
        ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0);
        ImGui.pushStyleColor(ImGuiCol.HeaderActive, 0);
        ImVec2 extraPadding = ImGui.getStyle().getTouchExtraPadding();
        ImGui.getStyle().setTouchExtraPadding(extraPadding.x, 4.F);

        this.drawNode(structure.getRootNode());

        ImGui.popStyleColor(2);
        ImGui.getStyle().setTouchExtraPadding(extraPadding.x, extraPadding.y);
        ImGui.popStyleVar(2);
    }

    private void drawNode(ClassStructureNode node) {
        int flags = ImGuiTreeNodeFlags.SpanFullWidth;

        if (node.isLeaf()) flags |= ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen;
        if (node.getBrowserViewerNode().isDefaultOpen()) flags |= ImGuiTreeNodeFlags.DefaultOpen;

        boolean tree = ImGui.treeNodeEx("###" + node.getStrId(), flags);
        ImGui.sameLine();
        node.getBrowserViewerNode().draw();

        if (tree) {
            for (ClassStructureNode child : node.getChildren()) {
                if (!this.filterComponent.getFilteredList().contains(child)) {
                    continue;
                }
                this.drawNode(child);
            }

            if (!node.isLeaf()) ImGui.treePop();
        }
    }
}
