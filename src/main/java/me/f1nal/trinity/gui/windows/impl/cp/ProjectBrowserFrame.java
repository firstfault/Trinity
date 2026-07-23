package me.f1nal.trinity.gui.windows.impl.cp;

import com.google.common.eventbus.Subscribe;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.events.EventPackageStructureReload;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.packages.ArchiveEntry;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.gui.components.filter.ListFilterComponent;
import me.f1nal.trinity.gui.components.filter.SearchBarFilter;
import me.f1nal.trinity.gui.components.filter.kind.KindFilter;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.gui.viewport.notifications.SimpleCaption;
import me.f1nal.trinity.gui.windows.api.StaticWindow;

import java.util.*;

public class ProjectBrowserFrame extends StaticWindow implements IEventListener {
    private static final String ENTRY_DRAG_PAYLOAD = "TRINITY_PROJECT_ENTRY";
    private final SearchBarFilter<IBrowserViewerNode> searchBarFilter = new SearchBarFilter<>(true);
    private final KindFilter<IBrowserViewerNode> kindFilter = new KindFilter<>(FileKind.values());
    private ListFilterComponent<IBrowserViewerNode> filterComponent;
    private String search;
    private List<ProjectBrowserTreeNodePackage> rootNodes = List.of();

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
        trinity.getExecution().getContainers()
                .forEach(container -> this.addTreeToCollection(container.getRootPackage(), collection));
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
            for (ProjectBrowserTreeNodePackage root : this.rootNodes) root.draw(this);
        }
        ImGui.endChild();
        ImGui.popStyleColor(2);
        ImGui.getStyle().setTouchExtraPadding(extraPadding.x, extraPadding.y);
    }

    void drawEntryDragSource(ArchiveEntry entry) {
        if (!ImGui.beginDragDropSource()) return;

        ImGui.setDragDropPayload(ENTRY_DRAG_PAYLOAD, entry);
        ImGui.text("Move " + entry.getDisplaySimpleName());
        ImGui.endDragDropSource();
    }

    void drawPackageDropTarget(Package targetPackage) {
        if (!ImGui.beginDragDropTarget()) return;

        ArchiveEntry entry = ImGui.acceptDragDropPayload(ENTRY_DRAG_PAYLOAD);
        if (entry != null) this.moveEntry(entry, targetPackage);
        ImGui.endDragDropTarget();
    }

    private void moveEntry(ArchiveEntry entry, Package targetPackage) {
        String destinationName = targetPackage.isArchive()
                ? entry.getDisplayOrRealName()
                : targetPackage.getChildrenPath(entry.getDisplaySimpleName());
        boolean sameContainer = entry.getContainer() == targetPackage.getContainer();
        if (sameContainer && destinationName.equals(entry.getDisplayOrRealName())) return;

        if (this.isDestinationOccupied(entry, targetPackage, destinationName)) {
            Main.getDisplayManager().addNotification(new Notification(NotificationType.WARNING,
                    new SimpleCaption("Move Failed"), ColoredStringBuilder.create()
                    .fmt("An entry named {} already exists in that package.", entry.getDisplaySimpleName()).get()));
            return;
        }

        if (entry.getContainer() != targetPackage.getContainer()) {
            entry.setPackage(targetPackage.getContainer().getRootPackage());
        }
        if (!destinationName.equals(entry.getDisplayOrRealName())) {
            entry.getRenameHandler().renameFully(this.trinity.getRemapper(), destinationName);
        }
    }

    private boolean isDestinationOccupied(ArchiveEntry movingEntry, Package targetPackage, String destinationName) {
        String destinationPath = movingEntry instanceof ClassTarget ? destinationName + ".class" : destinationName;

        boolean classCollision = this.trinity.getExecution().getClassTargetMap().values().stream()
                .filter(target -> target != movingEntry && target.getInput() != null)
                .anyMatch(target -> (target.getDisplayOrRealName() + ".class").equals(destinationPath));
        if (classCollision) return true;

        var resource = movingEntry instanceof me.f1nal.trinity.execution.packages.ResourceArchiveEntry
                ? targetPackage.getContainer().getResource(destinationPath) : null;
        return resource != null && resource != movingEntry;
    }

    public String getSearch() {
        return search;
    }

    private Set<IBrowserViewerNode> filteredSet;

    private void setNodeRoot() {
        this.filteredSet = new HashSet<>(this.filterComponent.getFilteredList());
        this.rootNodes = new ArrayList<>();
        List<ProjectBrowserTreeNodePackage> packages = new ArrayList<>();
        for (var container : trinity.getExecution().getContainers()) {
            Package rootPackage = container.getRootPackage();
            if (!this.isPackageSearchMatch(rootPackage) && !this.filteredSet.contains(rootPackage)) continue;
            ProjectBrowserTreeNodePackage rootNode = new ProjectBrowserTreeNodePackage(rootPackage);
            this.rootNodes.add(rootNode);
            packages.add(rootNode);
        }

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
