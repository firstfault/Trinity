package me.f1nal.trinity.execution.packages;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.events.EventPackageStructureReload;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.packages.other.ExtractArchiveEntryRunnable;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.IconFamily;
import me.f1nal.trinity.gui.components.events.MouseClickType;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantSearchFrame;
import me.f1nal.trinity.gui.windows.impl.cp.BrowserViewerNode;
import me.f1nal.trinity.gui.windows.impl.cp.IBrowserViewerNode;
import me.f1nal.trinity.gui.windows.impl.cp.IRenameHandler;
import me.f1nal.trinity.gui.windows.impl.entryviewer.ArchiveEntryViewerWindow;
import me.f1nal.trinity.gui.windows.impl.project.EditJarWindow;
import me.f1nal.trinity.util.ByteUtil;
import me.f1nal.trinity.util.SystemUtil;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

public abstract class ArchiveEntry implements IBrowserViewerNode, IRenameHandler {
    private Package targetPackage;
    private ProjectContainer container;
    private final String size;
    private final int sizeInBytes;
    private final BrowserViewerNode browserViewerNode;
    private ArchiveEntryViewerType[] viewerTypes;
    private ZipEntryMetadata zipMetadata;

    protected ArchiveEntry(int sizeInBytes) {
        this(sizeInBytes, ZipEntryMetadata.createDefault());
    }

    protected ArchiveEntry(int sizeInBytes, ZipEntryMetadata zipMetadata) {
        this.sizeInBytes = sizeInBytes;
        this.zipMetadata = zipMetadata == null ? ZipEntryMetadata.createDefault() : zipMetadata;
        this.size = ByteUtil.getHumanReadableByteCountSI(sizeInBytes);
        this.browserViewerNode = new BrowserViewerNode(getIcon(), IconFamily.CODICON,
                () -> this.getKind() == null ? this.getIconColor() : this.getKind().getColor(),
                this::getDisplaySimpleName, this.getRenameHandler());
        this.browserViewerNode.setLeftClickOnRelease(true);
        this.browserViewerNode.addMouseClickHandler(clickType -> {
            if (clickType == MouseClickType.RIGHT_CLICK) {
                Main.getDisplayManager().getPopupMenu().show(this.createPopup(PopupItemBuilder.create()));
            } else if (clickType == MouseClickType.LEFT_CLICK) {
                ArchiveEntryViewerType[] availableViewerTypes = this.getViewerTypes();
                if (availableViewerTypes.length != 0) {
                    this.openViewer(availableViewerTypes[0]);
                }
            }
        });
    }

    public ArchiveEntryViewerWindow<?> getDefaultViewer() {
        return this.getViewerTypes()[0].getWindow(this);
    }

    public final ArchiveEntryViewerType[] getViewerTypes() {
        if (this.viewerTypes == null) {
            this.viewerTypes = Arrays.stream(ArchiveEntryViewerType.values())
                    .filter(type -> type.getValid().test(this))
                    .toArray(ArchiveEntryViewerType[]::new);
        }
        return this.viewerTypes;
    }

    public abstract void setName(String newName);
    protected abstract int getIconColor();
    protected abstract String getIcon();

    @Override
    public BrowserViewerNode getBrowserViewerNode() {
        return browserViewerNode;
    }

    public Package getPackage() {
        return targetPackage;
    }

    public ProjectContainer getContainer() {
        return container;
    }

    void assignContainer(ProjectContainer container) {
        this.container = container;
    }

    public void setPackage(Package root) {
        Main.assertRenderThread();
        ProjectContainer previousContainer = this.container;
        if (getPackage() != null) {
            getPackage().remove(this);
        }
        if (previousContainer != null) previousContainer.unregister(this);

        String realName = this.getDisplayOrRealName();
        while (realName.contains("//")) realName = realName.replace("//", "/");
        Package targetPackage = root;
        int index, count = 0;
        while ((index = realName.indexOf('/')) != -1) {
            if (++count >= 32) {
                index = realName.lastIndexOf('/');
            }
            targetPackage = targetPackage.createPackage(realName.substring(0, index));
            realName = realName.substring(index + 1);
        }
        this.targetPackage = targetPackage;
        this.container = root.getPackageHierarchy().getContainer();
        if (previousContainer != null && previousContainer != this.container) {
            this.zipMetadata.setOrder(Integer.MAX_VALUE);
        }
        if (this.container != null) this.container.register(this);
        targetPackage.getEntries().add(this);
        this.targetPackage.getEntries().sort(Comparator.comparing(ArchiveEntry::getDisplaySimpleName));
        Main.getTrinity().getEventManager().postEvent(new EventPackageStructureReload());
    }

    /**
     * Creates a popup for this archive entry. Inheritors can override the super method and adapt the builder correspondingly.
     */
    public PopupItemBuilder createPopup(PopupItemBuilder builder) {
        if (getPackage() == null) throw new NullPointerException(String.format("Archive entry '%s' does not have a package.", this.getDisplayOrRealName()));

        this.addOpenActions(builder);
        this.addEntryActions(builder);
        if (this.getContainer() != null && this.getContainer().isJar()) {
            builder.menuItem("Edit JAR Entry...", () -> EditJarWindow.openEntry(Main.getTrinity(), this));
        }

        builder.separator();
        if (this.getBrowserViewerNode().isRenameAvailable()) {
            builder.menuItem(this.getRenameActionLabel(), () -> this.getBrowserViewerNode().beginRenaming());
        }
        this.addAfterRenameActions(builder);
        builder.menu("Copy", this::addCopyActions)
                .menuItem(FontAwesomeIcons.FileDownload + " Extract " + this.getExtractActionName() + "...",
                        new ExtractArchiveEntryRunnable(this));

        if (this instanceof ResourceArchiveEntry resource) {
            builder.separator().menuItem(FontAwesomeIcons.TrashAlt + " Delete Resource...",
                    () -> this.confirmResourceDeletion(resource));
        }
        return builder;
    }

    private void addOpenActions(PopupItemBuilder builder) {
        ArchiveEntryViewerType[] available = this.getViewerTypes();
        if (available.length == 0) return;

        ArchiveEntryViewerType defaultViewer = available[0];
        builder.menuItem("Open in " + defaultViewer.getName(), () -> this.openViewer(defaultViewer));
        if (available.length > 1) {
            builder.menu("Open With", open -> {
                for (int i = 1; i < available.length; i++) {
                    ArchiveEntryViewerType viewerType = available[i];
                    open.menuItem(viewerType.getName(), () -> this.openViewer(viewerType));
                }
            });
        }
    }

    /** Adds actions specific to an entry subtype directly below its open actions. */
    protected void addEntryActions(PopupItemBuilder builder) {
        if (this instanceof ResourceArchiveEntry) {
            builder.menuItem("Find Resource References", this::openResourceReferenceSearch);
        }
    }

    /** Adds the copy formats appropriate for this entry subtype. */
    protected void addCopyActions(PopupItemBuilder copy) {
        String name = this.getDisplaySimpleName();
        String path = this.getDisplayOrRealName();
        copy.menuItem("Name", () -> SystemUtil.copyToClipboard(name));
        if (!path.equals(name)) {
            copy.menuItem("Resource Path", () -> SystemUtil.copyToClipboard(path));
        }
    }

    /** Adds actions that belong with rename/extract rather than open/inspect. */
    protected void addAfterRenameActions(PopupItemBuilder builder) {
    }

    protected String getRenameActionLabel() {
        return this instanceof ClassTarget ? "Rename Class..." : "Rename File...";
    }

    private String getExtractActionName() {
        return this instanceof ClassTarget ? "Class" : "File";
    }

    private void confirmResourceDeletion(ResourceArchiveEntry resource) {
        Main.getWindowManager()
                .dialog("Delete Resource")
                .message("Delete " + resource.getDisplayOrRealName() + " from this project?")
                .confirm("Delete Resource", () -> {
                    Main.getWindowManager().closeAll(window ->
                            window instanceof ArchiveEntryViewerWindow<?> viewer
                                    && viewer.getArchiveEntry() == resource);
                    Main.getTrinity().getExecution().deleteResource(resource);
                })
                .show();
    }

    private void openResourceReferenceSearch() {
        ConstantSearchFrame searchFrame = Main.getWindowManager().addStaticWindow(ConstantSearchFrame.class);
        searchFrame.setStringSearchTerm(this.getDisplayOrRealName());
        Main.getWindowManager().requestFocus(searchFrame);
    }

    protected final void openViewer(ArchiveEntryViewerType viewerType) {
        if (viewerType == ArchiveEntryViewerType.DECOMPILER
                && this instanceof ClassTarget classTarget && classTarget.getInput() != null) {
            Main.getDisplayManager().openDecompilerView(classTarget.getInput());
            return;
        }
        Main.getWindowManager().addClosableWindow(viewerType.getWindow(this));
    }

    /**
     * Gets the file content to a byte[] array for file saving purposes.
     * @return byte[] data of this entry.
     */
    public abstract byte[] extract();

    public abstract String getRealName();
    public abstract String getDisplayOrRealName();
    public abstract String getArchiveEntryTypeName();

    private String getSimpleName(String name) {
        return name.substring(name.lastIndexOf('/') + 1);
    }
    public String getDisplaySimpleName() {
        return getSimpleName(getDisplayOrRealName());
    }
    public String getRealSimpleName() {
        return getSimpleName(getRealName());
    }
    public final String getSize() {
        return size;
    }

    public int getSizeInBytes() {
        return sizeInBytes;
    }

    public ZipEntryMetadata getZipMetadata() {
        return zipMetadata;
    }

    public void setZipMetadata(ZipEntryMetadata zipMetadata) {
        this.zipMetadata = Objects.requireNonNull(zipMetadata);
    }

    @Override
    public boolean matches(String searchTerm) {
        return getDisplaySimpleName().contains(searchTerm);
    }

    @Override
    public boolean matchesIgnoreCase(String searchTerm) {
        return getDisplaySimpleName().toLowerCase(Locale.ROOT).contains(searchTerm.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArchiveEntry that = (ArchiveEntry) o;
        return Objects.equals(getRealName(), that.getRealName())
                && Objects.equals(container == null ? null : container.getId(),
                        that.container == null ? null : that.container.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRealName(), container == null ? null : container.getId());
    }
}
