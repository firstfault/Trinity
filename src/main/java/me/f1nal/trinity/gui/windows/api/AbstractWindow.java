package me.f1nal.trinity.gui.windows.api;

import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.util.NameUtil;

public abstract class AbstractWindow {
    private static final float COVERED_DIALOG_POSITION = -100_000.F;
    protected String title;
    protected final float width, height;
    protected Trinity trinity;
    private boolean dialog;
    private boolean dialogCovered;
    private boolean centerOnNextOpen;
    private boolean restoreDialogPosition;
    private boolean dialogPositionKnown;
    private float dialogPositionX;
    private float dialogPositionY;
    private Runnable childWindowRenderer;
    private boolean disposed;
    private int currentDockId;
    /**
     * If this window is currently visible.
     */
    private boolean visible;

    protected AbstractWindow(String title, float width, float height, Trinity trinity) {
        this.title = NameUtil.cleanNewlines(title);
        this.width = width;
        this.height = height;
        this.trinity = trinity;
    }

    public Trinity getTrinity() {
        return trinity;
    }

    protected abstract void renderFrame();

    /** Draws the shared context menu inside this window's ImGui popup/ID scope. */
    protected final boolean drawSharedContextMenu() {
        return Main.getDisplayManager() != null
                && Main.getDisplayManager().getPopupMenu().draw();
    }

    public final void setChildWindowRenderer(Runnable childWindowRenderer) {
        this.childWindowRenderer = childWindowRenderer;
    }

    protected final void renderChildWindows() {
        if (this.childWindowRenderer != null) {
            this.childWindowRenderer.run();
        }
    }

    public void render() {
        ImGui.setNextWindowSize(width, height, ImGuiCond.FirstUseEver);
        ImGui.begin(getTitle());
        renderFrame();
        ImGui.end();
    }

    public void close() {
        this.setVisible(false);
    }

    /**
     * Permanently releases resources owned by this window.
     *
     * <p>Disposal is terminal and idempotent. Closing a static window may only
     * hide it, so the window manager invokes this separately when it actually
     * removes a window from its ownership.</p>
     */
    public final synchronized void dispose() {
        if (this.disposed) return;
        this.disposed = true;
        this.onDispose();
    }

    /** Called exactly once when this window is permanently discarded. */
    protected void onDispose() {
    }

    public final synchronized boolean isDisposed() {
        return this.disposed;
    }

    public void setVisible(boolean visible) {
        boolean opening = visible && !this.visible;
        this.visible = visible;
        if (opening) {
            if (this.isDialog()) {
                this.centerOnNextOpen = true;
            }
            this.onOpen();
        }
    }

    /** Called whenever a previously closed window is opened. */
    protected void onOpen() {
    }

    public final boolean isVisible() {
        return visible;
    }

    public final boolean isDialog() {
        return dialog;
    }

    protected final void setDialog(boolean dialog) {
        this.dialog = dialog;
        if (dialog && !this.visible) {
            this.centerOnNextOpen = true;
        }
    }

    public final void setDialogCovered(boolean dialogCovered) {
        this.dialogCovered = dialogCovered;
    }

    protected final boolean isDialogCovered() {
        return dialogCovered;
    }

    protected final int applyDialogWindowFlags(int flags) {
        return this.isDialog() ? flags | ImGuiWindowFlags.NoDocking : flags;
    }

    protected final void applyOpeningPosition() {
        ImGuiViewport viewport = ImGui.getMainViewport();
        if (this.isDialog() && this.isDialogCovered()) {
            if (!this.dialogPositionKnown) {
                this.dialogPositionX = viewport.getWorkCenterX() - this.width * 0.5F;
                this.dialogPositionY = viewport.getWorkCenterY() - this.height * 0.5F;
                this.dialogPositionKnown = true;
            }
            ImGui.setNextWindowPos(COVERED_DIALOG_POSITION, COVERED_DIALOG_POSITION, ImGuiCond.Always);
            this.restoreDialogPosition = true;
            this.centerOnNextOpen = false;
            return;
        }
        if (this.restoreDialogPosition) {
            ImGui.setNextWindowPos(this.dialogPositionX, this.dialogPositionY, ImGuiCond.Always);
            this.restoreDialogPosition = false;
            return;
        }
        if (!this.centerOnNextOpen) return;

        ImGui.setNextWindowDockID(0);
        ImGui.setNextWindowPos(viewport.getWorkCenterX(), viewport.getWorkCenterY(),
                ImGuiCond.Always, 0.5F, 0.5F);
        this.centerOnNextOpen = false;
    }

    protected final void captureDialogPosition() {
        if (!this.isDialog() || this.isDialogCovered()) return;
        this.dialogPositionX = ImGui.getWindowPosX();
        this.dialogPositionY = ImGui.getWindowPosY();
        this.dialogPositionKnown = true;
    }

    /** Records the dock leaf established by the current Begin call. */
    protected final void captureCurrentDockId() {
        this.currentDockId = this.isDialog() ? 0 : ImGui.getWindowDockID();
    }

    /** Returns the dock leaf reported by this window's latest Begin call. */
    public final int getCurrentDockId() {
        return currentDockId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Returns the complete ImGui identifier used to render this window.
     */
    public String getImGuiWindowName() {
        return this.getTitle();
    }

    /**
     * Returns whether ImGui has seen this window at least once.
     */
    public boolean hasRendered() {
        return false;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
