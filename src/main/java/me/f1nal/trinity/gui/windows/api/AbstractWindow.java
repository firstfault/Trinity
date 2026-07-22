package me.f1nal.trinity.gui.windows.api;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.util.NameUtil;

public abstract class AbstractWindow {
    protected String title;
    protected final float width, height;
    protected Trinity trinity;
    private boolean dialog;
    private Runnable childWindowRenderer;
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

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public final boolean isVisible() {
        return visible;
    }

    public final boolean isDialog() {
        return dialog;
    }

    protected final void setDialog(boolean dialog) {
        this.dialog = dialog;
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
