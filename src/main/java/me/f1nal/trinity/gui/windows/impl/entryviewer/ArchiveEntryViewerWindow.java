package me.f1nal.trinity.gui.windows.impl.entryviewer;

import imgui.ImGui;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.execution.packages.ArchiveEntry;
import me.f1nal.trinity.execution.packages.ResourceArchiveEntry;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.gui.viewport.notifications.ICaption;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.util.ByteUtil;

public abstract class ArchiveEntryViewerWindow<T extends ArchiveEntry> extends ClosableWindow {
    private final T archiveEntry;
    private boolean bringToFocus;
    private boolean docked;

    public ArchiveEntryViewerWindow(Trinity trinity, T archiveEntry) {
        super("ArchiveEntryViewer", 700, 600, trinity);
        this.archiveEntry = archiveEntry;
    }

    @Override
    public String getTitle() {
        return getArchiveEntry().getDisplayOrRealName();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            // TODO: Window flag?
            this.bringToFocus = true;
        }
        super.setVisible(visible);
    }

    @Override
    protected void onFocusGain() {
    }

    @Override
    protected boolean beginWindow() {
        final boolean state = super.beginWindow();
        if (ImGui.isItemHovered() && ImGui.isMouseReleased(1)) {
            PopupItemBuilder tabPopup = PopupItemBuilder.create();

            tabPopup.menuItem("Close", this::close);
            tabPopup.menuItem("Close Others", () -> Main.getWindowManager().closeAll(wnd -> wnd instanceof ArchiveEntryViewerWindow && wnd != this));
            tabPopup.menuItem("Close All", () -> Main.getWindowManager().closeAll(wnd -> wnd instanceof ArchiveEntryViewerWindow<?>));
            tabPopup.separator();

            Main.getDisplayManager().getPopupMenu().show(archiveEntry.createPopup(tabPopup));
        }
        return state;
    }

    @Override
    public void render() {
        if (this.isVisible()) {
            if (this.bringToFocus) {
                ImGui.setNextWindowFocus();
                this.bringToFocus = false;
            }
            if (!docked) {
                ImGui.setNextWindowDockID(123);
                docked = true;
            }
        }
        super.render();
    }

    public T getArchiveEntry() {
        return archiveEntry;
    }

    @Override
    protected abstract void renderFrame();

    protected void saveBytes(byte[] bytes, ICaption caption) {
        if (!(this.getArchiveEntry() instanceof ResourceArchiveEntry)) {
            throw new RuntimeException("Why are we saving a non-resource?");
        }
        trinity.getExecution().saveResource((ResourceArchiveEntry) this.getArchiveEntry(), bytes);
        Main.getDisplayManager().addNotification(new Notification(NotificationType.SUCCESS, caption, ColoredStringBuilder.create()
                .fmt("Saved {} to {}", ByteUtil.getHumanReadableByteCountSI(bytes.length), getArchiveEntry().getDisplaySimpleName()).get()));
    }

    @Override
    public boolean isAlreadyOpen(ClosableWindow otherWindow) {
        return otherWindow instanceof ArchiveEntryViewerWindow<?> && ((ArchiveEntryViewerWindow<?>) otherWindow).archiveEntry.equals(this.archiveEntry) && otherWindow.getClass().isAssignableFrom(this.getClass());
    }
}
