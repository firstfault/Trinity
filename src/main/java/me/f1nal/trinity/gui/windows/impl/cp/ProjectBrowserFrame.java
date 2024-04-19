package me.f1nal.trinity.gui.windows.impl.cp;

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
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.util.ByteUtil;
import me.f1nal.trinity.util.GuiUtil;
import me.f1nal.trinity.util.SystemUtil;

import java.util.*;

public class ProjectBrowserFrame extends StaticWindow implements IEventListener {
    private final SearchBarFilter<IBrowserViewerNode> searchBarFilter = new SearchBarFilter<>();
    private final KindFilter<IBrowserViewerNode> kindFilter = new KindFilter<>(FileKind.values());
    private ListFilterComponent<IBrowserViewerNode> filterComponent;
    private String search;
    private ProjectBrowserTreeNodePackage rootNode;

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
            this.filterComponent.addFilterChangeListener(this::setNodeRoot);
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
            this.rootNode.draw(this);
            ImGui.endTable();
        }
        ImGui.popStyleColor(2);
        ImGui.getStyle().setTouchExtraPadding(extraPadding.x, extraPadding.y);
        ImGui.popStyleVar();
    }

    public String getSearch() {
        return search;
    }

    private Set<IBrowserViewerNode> filteredSet;

    private void setNodeRoot() {
        this.filteredSet = new HashSet<>(this.filterComponent.getFilteredList());
        this.rootNode = new ProjectBrowserTreeNodePackage(trinity.getExecution().getRootPackage());
        List<ProjectBrowserTreeNodePackage> packages = new ArrayList<>();
        packages.add(this.rootNode);

        while (!packages.isEmpty()) {
            ProjectBrowserTreeNodePackage[] array = packages.toArray(ProjectBrowserTreeNodePackage[]::new);
            packages.clear();
            for (ProjectBrowserTreeNodePackage pkg : array) {
                this.addChildrenNodes(pkg, packages);
            }
        }
    }

    private void addChildrenNodes(ProjectBrowserTreeNodePackage node, List<ProjectBrowserTreeNodePackage> packages) {
        for (Package pkg : node.getNode().getPackages()) {
            if (!this.isPackageSearchMatch(pkg)) {
                continue;
            }

            ProjectBrowserTreeNodePackage pkgNode = new ProjectBrowserTreeNodePackage(pkg);
            node.addChild(pkgNode);
            packages.add(pkgNode);
        }

        for (ArchiveEntry entry : node.getNode().getEntries()) {
            if (!this.filteredSet.contains(entry)) {
                continue;
            }

            node.addChild(new ProjectBrowserTreeNodeEntry(entry));
        }
    }

    private boolean isPackageSearchMatch(Package pkg) {
        for (Package other : pkg.getPackages()) {
            if (isPackageSearchMatch(other)) {
                return true;
            }
        }
        for (ArchiveEntry entry : pkg.getEntries()) {
            if (this.filteredSet.contains(entry)) {
                return true;
            }
        }
        return false;
    }
}
