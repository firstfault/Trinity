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
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.ArrayList;
import java.util.List;

public class Package implements IDatabaseSavable<DatabasePackage>, IBrowserViewerNode {
    private final String name;
    private final String prettyPath;
    private final String path;
    private Package parent;
    private final List<Package> packages = new ArrayList<>();
    private final List<ArchiveEntry> classTargets = new ArrayList<>();
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
        this.path = this.createPath();
        if (parent != null) {
            this.packageHierarchy = parent.getPackageHierarchy();
            parent.getPackages().add(this);
            this.addToHierarchy();
        }
        this.browserViewerNode = new BrowserViewerNode(null,
                this.isArchive() ? () -> CodeColorScheme.ARCHIVE_REF : () -> CodeColorScheme.PACKAGE,
                this::getDisplayName,
                this.isArchive() ? (r, newName) -> packageHierarchy.getDatabase().setName(newName) : null);
        this.browserViewerNode.addMouseClickHandler(clickType -> {
            if (clickType == MouseClickType.RIGHT_CLICK) {
                PopupItemBuilder popup = PopupItemBuilder.create().
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
        this.packageHierarchy.getPathToPackage().put(this.getPath(), this);
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

    public String getPath() {
        return path;
    }

    public Package getParent() {
        return parent;
    }

    public List<Package> getPackages() {
        return packages;
    }

    public List<ArchiveEntry> getEntries() {
        return classTargets;
    }

    public void remove(ArchiveEntry classTarget) {
        classTargets.remove(classTarget);

        if (classTargets.isEmpty()) {
            packageHierarchy.getPathToPackage().remove(this.getPath());
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
        return new DatabasePackage(this.getPath(), this.isOpen());
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
