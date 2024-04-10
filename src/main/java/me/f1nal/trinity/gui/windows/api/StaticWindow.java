package me.f1nal.trinity.gui.windows.api;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;
import me.f1nal.trinity.Trinity;

/**
 * A window that is blocked to one instance.
 */
public abstract class StaticWindow extends AbstractWindow {
    private boolean setSize;
    private String id;
    private final ImBoolean open = new ImBoolean(true);
    protected int windowFlags;

    public StaticWindow(String title, float width, float height, Trinity trinity) {
        super(title, width, height, trinity);
        this.id = "StaticWnd" + title;
    }

    public String getId() {
        return id;
    }

    protected String getId(String suffix) {
        return suffix.concat(String.valueOf(this.getId()));
    }

    @Override
    public void render() {
        if (!this.isVisible()) {
            return;
        }
        if (!setSize) {
            ImGui.setNextWindowSize(width, height, ImGuiCond.Always);
            setSize = true;
        }

        String title = getTitle() + this.getId("###BeginWnd");
        if (isCloseable()) {
            ImGui.begin(title, open, windowFlags);
        } else {
            ImGui.begin(title, windowFlags);
        }
        renderFrame();
        ImGui.end();
        if (!open.get()) {
            this.open.set(true);
            this.close();
        }
    }

    public boolean isCloseable() {
        return true;
    }
}
