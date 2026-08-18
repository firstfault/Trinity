package me.f1nal.trinity.gui.windows.impl.classstructure;

import com.google.common.eventbus.Subscribe;
import imgui.ImGui;
import imgui.ImGuiListClipper;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.events.EventThemeChanged;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.gui.components.MemorableCheckboxComponent;
import me.f1nal.trinity.gui.components.filter.ListFilterComponent;
import me.f1nal.trinity.gui.components.filter.SearchBarFilter;
import me.f1nal.trinity.gui.components.filter.kind.IKindType;
import me.f1nal.trinity.gui.components.filter.kind.KindFilter;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.popup.PopupMenuBar;
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.gui.windows.impl.classstructure.nodes.ClassStructureNode;

import java.util.List;

public class ClassStructureWindow extends StaticWindow implements IEventListener {
    private static final MemorableCheckboxComponent showFilter = new MemorableCheckboxComponent("classStructureShowFilter", "Show Filter", false);
    private ClassStructure classStructure;
    private ListFilterComponent<ClassStructureNode> filterComponent;
    /** Flat member rows currently accepted by the filters. The class root is drawn separately. */
    private List<ClassStructureNode> filteredMembers = List.of();
    private final KindFilter<ClassStructureNode> kindFilter = new KindFilter<>(StructureKind.values());
    private final SearchBarFilter<ClassStructureNode> searchBarFilter = new SearchBarFilter<>(true);
    private final PopupMenuBar popupMenuBar = new PopupMenuBar(PopupItemBuilder.create());

    public ClassStructureWindow(Trinity trinity) {
        super("Class Structure", 600, 400, trinity);
        this.kindFilter.setExclude(new IKindType[]{StructureKind.CLASSES});
        this.windowFlags |= ImGuiWindowFlags.MenuBar;
        this.windowFlags |= ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse;
        trinity.getEventManager().registerListener(this);
    }

    @Subscribe
    public void onThemeChanged(EventThemeChanged event) {
        if (this.classStructure != null) {
            this.classStructure.getRootNode().refreshTheme();
        }
    }

    public void setClassStructure(ClassStructure classStructure) {
        this.classStructure = classStructure;
        this.filterComponent = new ListFilterComponent<>(classStructure.getRootNode().getAllChildren(), this.searchBarFilter, this.kindFilter);
        this.filterComponent.addFilterChangeListener(() -> this.filteredMembers =
                this.filterComponent.getFilteredList().stream()
                        .filter(node -> node != this.classStructure.getRootNode())
                        .toList());
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
        this.popupMenuBar.set(PopupItemBuilder.create().menu("Find", find -> find.menuItem("Show Filter", "Ctrl+F", showFilter.isChecked(), showFilter::toggleChecked)));
        this.popupMenuBar.draw();

        if (ImGui.isWindowFocused() && ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(ImGuiKey.F)) {
            showFilter.toggleChecked();
        }

        final ClassStructure structure = this.getClassStructure();

        if (structure == null) {
            return;
        }

        if (showFilter.isChecked()) {
            this.filterComponent.draw();

            ImGui.separator();
        }

        ImGui.pushStyleVar(ImGuiStyleVar.CellPadding, 0.F, 4.F);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, ImGui.getStyle().getItemSpacingX(), 8.F);
        ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0);
        ImGui.pushStyleColor(ImGuiCol.HeaderActive, 0);
        ImVec2 extraPadding = ImGui.getStyle().getTouchExtraPadding();
        ImGui.getStyle().setTouchExtraPadding(extraPadding.x, 4.F);

        if (ImGui.beginChild(getId("StructureTree"), 0.F, 0.F,
                false, ImGuiWindowFlags.HorizontalScrollbar)) {
            this.drawNode(structure.getRootNode());
        }
        ImGui.endChild();

        ImGui.popStyleColor(2);
        ImGui.getStyle().setTouchExtraPadding(extraPadding.x, extraPadding.y);
        ImGui.popStyleVar(2);
    }

    private void drawNode(ClassStructureNode node) {
        int flags = ImGuiTreeNodeFlags.SpanFullWidth;

        if (node.isLeaf()) flags |= ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen;
        if (node.getBrowserViewerNode().isDefaultOpen()) flags |= ImGuiTreeNodeFlags.DefaultOpen;

        boolean tree = ImGui.treeNodeEx("###" + node.getStrId(), flags);
        ImGui.sameLine(0.F, 0.F);
        node.getBrowserViewerNode().draw();

        if (tree) {
            if (node == this.classStructure.getRootNode()) this.drawVisibleMembers();
            else for (ClassStructureNode child : node.getChildren()) this.drawNode(child);

            if (!node.isLeaf()) ImGui.treePop();
        }
    }

    private void drawVisibleMembers() {
        if (this.filteredMembers.isEmpty()) return;

        ImGuiListClipper clipper = new ImGuiListClipper();
        try {
            // Class members are a flat collection of leaf rows with a stable height. Only submit
            // the rows intersecting the child viewport instead of thousands of ImGui tree items.
            clipper.begin(this.filteredMembers.size(), ImGui.getTextLineHeightWithSpacing());
            while (clipper.step()) {
                int start = Math.max(0, clipper.getDisplayStart());
                int end = Math.min(this.filteredMembers.size(), clipper.getDisplayEnd());
                for (int index = start; index < end; index++) {
                    this.drawNode(this.filteredMembers.get(index));
                }
            }
        } finally {
            clipper.destroy();
        }
    }
}
