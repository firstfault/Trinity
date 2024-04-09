package me.f1nal.trinity.gui.windows.api;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.util.NameUtil;

public abstract class AbstractWindow {
    protected String title;
    protected final float width, height;
    protected Trinity trinity;
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

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
