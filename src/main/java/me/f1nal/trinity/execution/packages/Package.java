package me.f1nal.trinity.execution.packages;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.object.DatabasePackage;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.events.MouseClickType;
import me.f1nal.trinity.gui.components.filter.kind.IKindType;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.impl.cp.BrowserViewerNode;
import me.f1nal.trinity.gui.windows.impl.cp.IBrowserViewerNode;
import me.f1nal.trinity.gui.windows.impl.cp.RenameHandler;
import me.f1nal.trinity.remap.Remapper;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.SystemUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        this.packageHierarchy = new PackageHierarchy(this, database);
        this.addToHierarchy();
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
        this.browserViewerNode = new BrowserViewerNode(null,
                this.isArchive() ? () -> CodeColorScheme.ARCHIVE_REF : () -> CodeColorScheme.PACKAGE,
                this::getDisplayName,
                this::rename);
        this.browserViewerNode.addMouseClickHandler(clickType -> {
            if (clickType == MouseClickType.RIGHT_CLICK) {
                PopupItemBuilder popup = PopupItemBuilder.create().
                        menu("Copy", (copy) -> {
                            copy.menuItem("Full Path", () -> SystemUtil.copyToClipboard(this.getPrettyPath().replace('.', '/')))
                                    .menuItem("Name", () -> SystemUtil.copyToClipboard(this.getDisplayName()));
                        }).
                        menu("Create...", (menu) -> {
                            menu.menuItem("Empty File", () -> {
                                ArchiveEntry newFile = Main.getTrinity().getExecution().createResource(this, "New File", new byte[0]);
                                if (newFile != null) {
                                    newFile.getBrowserViewerNode().beginRenaming();
                                }
                            });
                        });

                if (browserViewerNode.isRenameAvailable()) popup.menuItem("Rename", () -> this.getBrowserViewerNode().beginRenaming());

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
            this.packageHierarchy.getDatabase().setName(newName);
        } else {
            final Map<RenameHandler, String> renames = new HashMap<>();
            final int startLength = this.getPrettyPath().length() - this.getName().length();
            final int endLength = startLength + this.getName().length();
            this.addPackageRename(this, renames, newName, startLength, endLength);
            renames.forEach((renameHandler, fullNewName) -> renameHandler.rename(remapper, fullNewName));
        }
    }

    private void addPackageRename(Package pkg, Map<RenameHandler, String> renames, String newName, int startLength, int endLength) {
        pkg.getPackages().forEach(otherPackage -> this.addPackageRename(otherPackage, renames, newName, startLength, endLength));
        pkg.getEntries().forEach(archiveEntry -> renames.put(archiveEntry.getRenameHandler(), archiveEntry.getDisplayOrRealName().substring(0, startLength) + newName + archiveEntry.getDisplayOrRealName().substring(endLength)));
    }

    private String getDisplayName() {
        return this.isArchive() ? packageHierarchy.getDatabase().getName() : this.getName();
    }

    public boolean isArchive() {
        return this.getParent() == null;
    }

    private void updateBrowserViewerNode() {
        this.browserViewerNode.setIcon(this.isArchive() ? FontAwesomeIcons.FileArchive : this.isOpen() ? FontAwesomeIcons.FolderOpen : FontAwesomeIcons.Folder);
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
    }

    public PackageHierarchy getPackageHierarchy() {
        return packageHierarchy;
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

        if (archiveEntries.isEmpty()) {
            packageHierarchy.getPathToPackage().remove(this.getInternalPath());
            if (parent != null) parent.getPackages().remove(this);
        }
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
        return new DatabasePackage(this.getInternalPath(), this.isOpen());
    }

    @Override
    public IKindType getKind() {
        return null;
    }

    @Override
    public boolean matches(String searchTerm) {
        return name.contains(searchTerm);
    }

    public String getChildrenPath(String fileName) {
        String path = this.getPrettyPath().replace('.', '/');
        if (!path.isEmpty()) path += "/";
        path += fileName;
        return path;
    }
}
