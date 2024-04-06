package me.f1nal.trinity.gui.frames.impl.cp;

import com.google.common.eventbus.Subscribe;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiTreeNodeFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.packages.ArchiveEntry;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.gui.components.filter.ListFilterComponent;
import me.f1nal.trinity.gui.components.filter.SearchBarFilter;
import me.f1nal.trinity.gui.components.filter.kind.KindFilter;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.frames.StaticWindow;
import me.f1nal.trinity.util.ByteUtil;
import me.f1nal.trinity.util.GuiUtil;
import me.f1nal.trinity.util.SystemUtil;

import java.util.ArrayList;
import java.util.List;

public class ProjectBrowserFrame extends StaticWindow implements IEventListener {
    private final SearchBarFilter<IBrowserViewerNode> searchBarFilter = new SearchBarFilter<>();
    private final KindFilter<IBrowserViewerNode> kindFilter = new KindFilter<>(FileKind.values());
    private ListFilterComponent<IBrowserViewerNode> filterComponent;
    private String search;

    public ProjectBrowserFrame(Trinity trinity) {
        super("Project Browser", 600, 460, trinity);
        this.refreshFilterComponent();
        trinity.getEventManager().registerListener(this);
    }

    @Subscribe
    public void onClassesLoaded(EventClassesLoaded event) {
        this.refreshFilterComponent();
    }

    private void refreshFilterComponent() {
        this.filterComponent = null;
    }

    private List<IBrowserViewerNode> createViewerList() {
        List<IBrowserViewerNode> collection = new ArrayList<>();
        this.addTreeToCollection(trinity.getExecution().getRootPackage(), collection);
        return collection;
    }

    private void addTreeToCollection(Package pkg, List<IBrowserViewerNode> collection) {
        collection.add(pkg);
        collection.addAll(pkg.getEntries());
        pkg.getPackages().forEach(other -> addTreeToCollection(other, collection));
    }


    @Override
    public void render() {
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.F, ImGui.getStyle().getWindowPaddingY());
        super.render();
        ImGui.popStyleVar();
    }

    @Override
    protected void renderFrame() {
        if (this.filterComponent == null) {
            this.filterComponent = new ListFilterComponent<>(createViewerList(), this.searchBarFilter, this.kindFilter);
        }

        this.filterComponent.draw();
        this.search = this.searchBarFilter.getSearchBar().getSearchText().get();

        ImGui.separator();

        ImGui.pushStyleVar(ImGuiStyleVar.CellPadding, 0.F, 4.F);
        ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0);
        ImGui.pushStyleColor(ImGuiCol.HeaderActive, 0);
        ImVec2 extraPadding = ImGui.getStyle().getTouchExtraPadding();
        ImGui.getStyle().setTouchExtraPadding(extraPadding.x, 4.F);

        if (ImGui.beginTable(getId("ViewTable"), 3, ImGuiTableFlags.NoBordersInBodyUntilResize | ImGuiTableFlags.SizingStretchProp | ImGuiTableFlags.Sortable | ImGuiTableFlags.Resizable)) {
            ImGui.tableSetupColumn(" Name");
            ImGui.tableSetupColumn(" Size");
            ImGui.tableSetupColumn(" Type");
            ImGui.tableHeadersRow();
            renderTree(trinity.getExecution().getRootPackage(), 0);
            ImGui.endTable();
        }
        ImGui.popStyleColor(2);
        ImGui.getStyle().setTouchExtraPadding(extraPadding.x, extraPadding.y);
        ImGui.popStyleVar();
    }

    public void renderTree(Package node, int imguiId) {
        ImGui.tableNextRow();
        ImGui.tableNextColumn();
        boolean searching = !this.search.isEmpty();
        ImGui.setNextItemOpen((node.isOpen() && (node.getParent() == null || node.getParent().isOpen())) || searching);
        boolean open = ImGui.treeNodeEx("###" + node.getPath(), ImGuiTreeNodeFlags.SpanFullWidth);
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
            for (Package pkg : node.getPackages()) {
                if (isPackageSearchMatch(pkg)) {
                    renderTree(pkg, imguiId++);
                }
            }

            for (ArchiveEntry archiveEntry : node.getEntries()) {
                if (!this.filterComponent.getFilteredList().contains(archiveEntry)) {
                    continue;
                }

                ImGui.tableNextRow();
                ImGui.tableNextColumn();

                ImGui.treeNodeEx("", ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen |
                        ImGuiTreeNodeFlags.SpanFullWidth);
                ImGui.sameLine();
                archiveEntry.getBrowserViewerNode().draw();

                ImGui.tableNextColumn();
                this.drawSize(archiveEntry.getSize(), archiveEntry.getSizeInBytes());
                ImGui.tableNextColumn();
                ImGui.textUnformatted(archiveEntry.getArchiveEntryTypeName());
            }
        }

        if (open) ImGui.treePop();
    }

    private void drawSize(String sizeText, long sizeInBytes) {
        ImGui.text(sizeText);
        GuiUtil.tooltip(sizeInBytes + "B");
        if (ImGui.isItemClicked(1)) {
            Main.getDisplayManager().getPopupMenu().show(PopupItemBuilder.create().menuItem("Copy Size", () -> SystemUtil.copyToClipboard(String.valueOf(sizeInBytes))));
        }
    }

    private boolean isPackageSearchMatch(Package pkg) {
        for (Package other : pkg.getPackages()) {
            if (isPackageSearchMatch(other)) {
                return true;
            }
        }
        for (ArchiveEntry entry : pkg.getEntries()) {
            if (this.filterComponent.getFilteredList().contains(entry)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesSearch(Package node) {
        return this.search.isEmpty() || node.getName().toLowerCase().contains(this.search);
    }
}
