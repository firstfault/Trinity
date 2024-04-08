package me.f1nal.trinity.execution.packages;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.packages.other.ExtractArchiveEntryRunnable;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.events.MouseClickType;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.frames.impl.cp.BrowserViewerNode;
import me.f1nal.trinity.gui.frames.impl.cp.IBrowserViewerNode;
import me.f1nal.trinity.gui.frames.impl.cp.IRenameHandler;
import me.f1nal.trinity.gui.frames.impl.cp.RenameHandler;
import me.f1nal.trinity.gui.frames.impl.entryviewer.ArchiveEntryViewerWindow;
import me.f1nal.trinity.util.ByteUtil;
import me.f1nal.trinity.util.SystemUtil;

import java.util.Arrays;
import java.util.Objects;

public abstract class ArchiveEntry implements IBrowserViewerNode, IRenameHandler {
    private Package targetPackage;
    private final String size;
    private final int sizeInBytes;
    private final BrowserViewerNode browserViewerNode;
    private final ArchiveEntryViewerType[] viewerTypes;

    protected ArchiveEntry(int sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
        this.size = ByteUtil.getHumanReadableByteCountSI(sizeInBytes);
        this.viewerTypes = Arrays.stream(ArchiveEntryViewerType.values()).filter(type -> type.getValid().test(this)).toArray(ArchiveEntryViewerType[]::new);
        this.browserViewerNode = new BrowserViewerNode(getIcon(), this.getKind() == null ? this::getIconColor : () -> this.getKind().getColor(), this::getDisplaySimpleName, this.getRenameHandler());
        this.browserViewerNode.addMouseClickHandler(clickType -> {
            if (clickType == MouseClickType.RIGHT_CLICK) {
                Main.getDisplayManager().getPopupMenu().show(this.createPopup(PopupItemBuilder.create()));
            } else if (clickType == MouseClickType.LEFT_CLICK) {
                if (this.viewerTypes.length != 0) {
                    Main.getDisplayManager().addClosableWindow(this.viewerTypes[0].getWindow(this));
                }
            }
        });
    }

    public ArchiveEntryViewerWindow<?> getDefaultViewer() {
        return viewerTypes[0].getWindow(this);
    }

    public final ArchiveEntryViewerType[] getViewerTypes() {
        return viewerTypes;
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

    public void setPackage(Package root) {
        if (getPackage() != null) {
            getPackage().remove(this);
        }

        String realName = this.getDisplayOrRealName();
        Package targetPackage = root;
        int index;
        while ((index = realName.indexOf('/')) != -1) {
            targetPackage = targetPackage.createPackage(realName.substring(0, index));
            realName = realName.substring(index + 1);
        }
        this.targetPackage = targetPackage;
        targetPackage.getEntries().add(this);
    }

    /**
     * Creates a popup for this archive entry. Inheritors can override the super method and adapt the builder correspondingly.
     */
    public PopupItemBuilder createPopup(PopupItemBuilder builder) {
        if (getPackage() == null) throw new NullPointerException(String.format("Archive entry '%s' does not have a package.", this.getDisplayOrRealName()));

        return builder.
                menu("Open", (open) -> {
                    for (ArchiveEntryViewerType viewerType : this.getViewerTypes()) {
                        open.menuItem(viewerType.getName(), () -> Main.getDisplayManager().addClosableWindow(viewerType.getWindow(this)));
                    }
                }).
                menuItem("Copy Path", () -> SystemUtil.copyToClipboard(this.getDisplayOrRealName())).
                separator().
                predicate(() -> getPackage() != null && getPackage().isOpen() && getBrowserViewerNode().isRenameAvailable(),
                        items -> items.menuItem("Rename", () -> this.getBrowserViewerNode().beginRenaming())).
                predicate(() -> this instanceof ResourceArchiveEntry,
                        items -> items.menuItem(FontAwesomeIcons.TrashAlt + " Delete", () -> Main.getTrinity().getExecution().deleteResource((ResourceArchiveEntry) this))).
                menuItem(FontAwesomeIcons.FileDownload + " Extract", new ExtractArchiveEntryRunnable(this));
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

    @Override
    public boolean matches(String searchTerm) {
        return getDisplayOrRealName().contains(searchTerm);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArchiveEntry that = (ArchiveEntry) o;
        return Objects.equals(getRealName(), that.getRealName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRealName());
    }
}
