package me.f1nal.trinity.gui.windows.impl.cp;

import com.google.common.eventbus.Subscribe;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.events.EventPackageStructureReload;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.packages.ArchiveEntry;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.gui.components.filter.ListFilterComponent;
import me.f1nal.trinity.gui.components.filter.SearchBarFilter;
import me.f1nal.trinity.gui.components.filter.kind.KindFilter;
import me.f1nal.trinity.gui.windows.api.StaticWindow;

import java.util.*;

public class ProjectBrowserFrame extends StaticWindow implements IEventListener {
    private final SearchBarFilter<IBrowserViewerNode> searchBarFilter = new SearchBarFilter<>(true);
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

    @Subscribe
    public void onPackageStructureReload(EventPackageStructureReload event) {
        this.refreshFilterComponent();
    }

    @Subscribe
    public void onClassModified(EventClassModified event) {
        this.refreshFilterComponent();
    }

    @Subscribe
    public void onMemberModified(EventMemberModified event) {
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

        ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0);
        ImGui.pushStyleColor(ImGuiCol.HeaderActive, 0);
        ImVec2 extraPadding = ImGui.getStyle().getTouchExtraPadding();
        ImGui.getStyle().setTouchExtraPadding(extraPadding.x, 3.F);

        if (ImGui.beginChild(getId("ViewTree"), 0.F, 0.F)) {
            for (ProjectBrowserTreeNode<?> child : this.rootNode.getChildren()) {
                child.draw(this);
            }
        }
        ImGui.endChild();
        ImGui.popStyleColor(2);
        ImGui.getStyle().setTouchExtraPadding(extraPadding.x, extraPadding.y);
    }

    public String getSearch() {
        return search;
    }

    private Set<IBrowserViewerNode> filteredSet;

    private void setNodeRoot() {
        this.filteredSet = new HashSet<>(this.filterComponent.getFilteredList());
        Package rootPackage = trinity.getExecution().getRootPackage();
        rootPackage.setOpenForced(true);
        this.rootNode = new ProjectBrowserTreeNodePackage(rootPackage);
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

            ProjectBrowserTreeNodePackage pkgNode = new ProjectBrowserTreeNodePackage(
                    this.createCompactPackageChain(pkg));
            node.addChild(pkgNode);
            packages.add(pkgNode);
        }

        for (ArchiveEntry entry : node.getNode().getEntries()) {
            if (!this.filteredSet.contains(entry)) {
                continue;
            }

            ProjectBrowserTreeNodeEntry entryNode = new ProjectBrowserTreeNodeEntry(entry);
            /*if (entry instanceof ClassTarget classTarget && classTarget.getInput() != null) {
                classTarget.getInput().getFieldMap().values().stream()
                        .filter(field -> field.getOwningClass() == classTarget.getInput())
                        .map(ProjectBrowserMemberNode::new)
                        .map(ProjectBrowserTreeNodeMember::new)
                        .forEach(entryNode::addChild);
                classTarget.getInput().getMethodMap().values().stream()
                        .filter(method -> method.getOwningClass() == classTarget.getInput())
                        .map(ProjectBrowserMemberNode::new)
                        .map(ProjectBrowserTreeNodeMember::new)
                        .forEach(entryNode::addChild);
            }*/
            node.addChild(entryNode);
        }
    }

    static List<Package> createCompactPackageChain(Package first) {
        List<Package> chain = new ArrayList<>();
        Package current = first;
        chain.add(current);
        while (current.getEntries().isEmpty() && current.getPackages().size() == 1) {
            current = current.getPackages().get(0);
            chain.add(current);
        }
        return chain;
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
