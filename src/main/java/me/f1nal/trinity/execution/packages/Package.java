package me.f1nal.trinity.execution.packages;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.object.DatabasePackage;
import me.f1nal.trinity.events.EventPackageStructureReload;
import me.f1nal.trinity.gui.components.CodiconIcons;
import me.f1nal.trinity.gui.components.IconFamily;
import me.f1nal.trinity.gui.components.events.MouseClickType;
import me.f1nal.trinity.gui.components.filter.kind.IKindType;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.impl.cp.BrowserViewerNode;
import me.f1nal.trinity.gui.windows.impl.cp.IBrowserViewerNode;
import me.f1nal.trinity.gui.windows.impl.cp.RenameHandler;
import me.f1nal.trinity.gui.windows.impl.bytecode.BytecodeEditorLauncher;
import me.f1nal.trinity.gui.windows.impl.ExportJarWindow;
import me.f1nal.trinity.gui.windows.impl.project.RemoveProjectContainerPopup;
import me.f1nal.trinity.gui.windows.impl.project.EditJarWindow;
import me.f1nal.trinity.execution.packages.other.ExportLooseFilesRunnable;
import me.f1nal.trinity.remap.Remapper;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.SystemUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Package implements IDatabaseSavable<DatabasePackage>, IBrowserViewerNode {
    private final String name;
    private final String prettyPath;
    private final String internalPath;
    private Package parent;
    private final List<Package> packages = new ArrayList<>();
    private final List<ArchiveEntry> archiveEntries = new ArrayList<>();
    private PackageHierarchy packageHierarchy;
    private final BrowserViewerNode browserViewerNode;
    private boolean open;

    public Package(Database database) {
        this("RootPackage", null);
        this.packageHierarchy = new PackageHierarchy(this, database, null);
        this.addToHierarchy();
        this.updateBrowserViewerNode();
    }

    public Package(ProjectContainer container, Database database) {
        this("RootPackage", null);
        this.packageHierarchy = new PackageHierarchy(this, database, container);
        this.addToHierarchy();
        this.updateBrowserViewerNode();
    }

    private Package(String name, Package parent) {
        this.name = name;
        this.parent = parent;
        this.prettyPath = this.createPrettyPath();
        this.internalPath = this.createPath();
        if (parent != null) {
            this.packageHierarchy = parent.getPackageHierarchy();
            parent.getPackages().add(this);
            this.addToHierarchy();
        }
        this.browserViewerNode = new BrowserViewerNode(null, IconFamily.CODICON,
                () -> this.isArchive() && this.getContainer() != null && this.getContainer().isJar()
                        ? CodeColorScheme.ARCHIVE_REF : CodeColorScheme.PACKAGE,
                this::getDisplayName,
                this::rename);
        this.browserViewerNode.addMouseClickHandler(clickType -> {
            if (clickType == MouseClickType.RIGHT_CLICK) {
                PopupItemBuilder popup = PopupItemBuilder.create();
                ProjectContainer container = this.getContainer();
                if (this.isArchive() && container != null && container.isJar()) {
                    popup.menuItem("Edit JAR...", () -> Main.getWindowManager()
                            .addClosableWindow(new EditJarWindow(Main.getTrinity(), container)))
                            .menuItem("Export JAR...", () -> Main.getWindowManager()
                            .addClosableWindow(new ExportJarWindow(Main.getTrinity(), container)))
                            .separator();
                } else if (this.isArchive() && container != null) {
                    popup.menuItem("Export Directory...", new ExportLooseFilesRunnable(container))
                            .separator();
                }
                popup.
                        menu("Copy", (copy) -> {
                            copy.menuItem("Full Path", () -> SystemUtil.copyToClipboard(this.getPrettyPath().replace('.', '/')))
                                    .menuItem("Name", () -> SystemUtil.copyToClipboard(this.getDisplayName()));
                        }).
                        menuItem("Add Class", () -> BytecodeEditorLauncher.addClass(this)).
                        menu("Create...", (menu) -> {
                            menu.menuItem("Empty File", () -> {
                                ArchiveEntry newFile = Main.getTrinity().getExecution().createResource(this, "New File", new byte[0]);
                                if (newFile != null) {
                                    newFile.getBrowserViewerNode().beginRenaming();
                                }
                            });
                        });

                if (browserViewerNode.isRenameAvailable()) popup.menuItem("Rename", () -> this.getBrowserViewerNode().beginRenaming());
                if (this.isArchive() && container != null && container.isJar()) {
                    popup.separator().menuItem("Remove Archive...", () -> Main.getWindowManager()
                            .addPopup(new RemoveProjectContainerPopup(Main.getTrinity(), container)));
                }

                Main.getDisplayManager().getPopupMenu().show(popup);
            }
        });
        this.updateBrowserViewerNode();
    }

    public void rename(Remapper remapper, String newName) {
        if (newName.equals(this.getDisplayName()) || newName.isEmpty()) {
            return;
        }

        if (this.isArchive()) {
            ProjectContainer container = this.packageHierarchy.getContainer();
            if (container != null) {
                container.setName(newName);
                Main.getTrinity().getEventManager().postEvent(new EventPackageStructureReload());
            }
            else if (this.packageHierarchy.getDatabase() != null) this.packageHierarchy.getDatabase().setName(newName);
        } else {
            final Map<RenameHandler, String> renames = new HashMap<>();
            final int startLength = this.getPrettyPath().length() - this.getName().length();
            final int endLength = startLength + this.getName().length();
            this.addPackageRename(this, renames, newName, startLength, endLength);
            renames.forEach((renameHandler, fullNewName) -> renameHandler.renameFully(remapper, fullNewName));
        }
    }

    private void addPackageRename(Package pkg, Map<RenameHandler, String> renames, String newName, int startLength, int endLength) {
        pkg.getPackages().forEach(otherPackage -> this.addPackageRename(otherPackage, renames, newName, startLength, endLength));
        pkg.getEntries().forEach(archiveEntry -> renames.put(archiveEntry.getRenameHandler(), archiveEntry.getDisplayOrRealName().substring(0, startLength) + newName + archiveEntry.getDisplayOrRealName().substring(endLength)));
    }

    private String getDisplayName() {
        if (!this.isArchive()) return this.getName();
        ProjectContainer container = packageHierarchy.getContainer();
        if (container != null) return container.getName();
        return packageHierarchy.getDatabase() == null ? this.getName() : packageHierarchy.getDatabase().getName();
    }

    public boolean isArchive() {
        return this.getParent() == null;
    }

    private void updateBrowserViewerNode() {
        ProjectContainer container = this.packageHierarchy == null ? null : this.packageHierarchy.getContainer();
        this.browserViewerNode.setIcon(this.isArchive()
                ? container != null && container.getKind() == ProjectContainerKind.LOOSE
                    ? CodiconIcons.FOLDER
                    : CodiconIcons.ARCHIVE
                : this.isOpen() ? CodiconIcons.FOLDER_OPENED : CodiconIcons.FOLDER);
    }

    private void addToHierarchy() {
        this.packageHierarchy.getPathToPackage().put(this.getInternalPath(), this);
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        Package parent = this;
        while (true) {
            parent = parent.parent;
            if (parent == null) break;
            if (!parent.isOpen()) {
                this.open = false;
                return;
            }
        }
        this.open = open;
        this.updateBrowserViewerNode();
    }

    public void setOpenForced(boolean open) {
        this.open = open;
        this.updateBrowserViewerNode();
    }

    public PackageHierarchy getPackageHierarchy() {
        return packageHierarchy;
    }

    public ProjectContainer getContainer() {
        return packageHierarchy.getContainer();
    }

    private String createPath() {
        StringBuilder path = new StringBuilder();
        Package parent = this;
        do {
            path.append(parent.getName()).append(".");
        } while ((parent = parent.parent) != null);
        return path.toString();
    }

    private String createPrettyPath() {
        String path = "";
        Package parent = this;
        while (true) {
            if (parent.parent == null) {
                break;
            }
            path = parent.getName() + "." + path;
            parent = parent.parent;
            if (parent == null) break;
        }
        if (path.endsWith(".")) path = path.substring(0, path.length() - 1);
        return path;
    }

    public String getName() {
        return name;
    }

    public String getInternalPath() {
        return internalPath;
    }

    public Package getParent() {
        return parent;
    }

    public List<Package> getPackages() {
        return packages;
    }

    public List<ArchiveEntry> getEntries() {
        return archiveEntries;
    }

    public void remove(ArchiveEntry classTarget) {
        archiveEntries.remove(classTarget);
        this.pruneIfEmpty();
    }

    private void pruneIfEmpty() {
        if (!archiveEntries.isEmpty() || !packages.isEmpty() || parent == null) return;
        packageHierarchy.getPathToPackage().remove(this.getInternalPath());
        Package oldParent = parent;
        oldParent.getPackages().remove(this);
        oldParent.pruneIfEmpty();
    }

    public Package createPackage(String name) {
        for (Package pkg : packages) {
            if (pkg.getName().equals(name)) {
                return pkg;
            }
        }
        return new Package(name, this);
    }

    public String getPrettyPath() {
        return this.prettyPath;
    }

    @Override
    public BrowserViewerNode getBrowserViewerNode() {
        return browserViewerNode;
    }

    @Override
    public DatabasePackage createDatabaseObject() {
        ProjectContainer container = this.getContainer();
        if (container == null) return null;
        return new DatabasePackage(container.getId().toString(), this.getInternalPath(), this.isOpen());
    }

    @Override
    public IKindType getKind() {
        return null;
    }

    @Override
    public boolean matches(String searchTerm) {
        return getDisplayName().contains(searchTerm);
    }

    @Override
    public boolean matchesIgnoreCase(String searchTerm) {
        return getDisplayName().toLowerCase(Locale.ROOT).contains(searchTerm.toLowerCase(Locale.ROOT));
    }

    public String getChildrenPath(String fileName) {
        String path = this.getPrettyPath().replace('.', '/');
        if (!path.isEmpty()) path += "/";
        path += fileName;
        return path;
    }
}
