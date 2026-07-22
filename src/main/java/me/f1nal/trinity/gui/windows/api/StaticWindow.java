package me.f1nal.trinity.gui.windows.api;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import me.f1nal.trinity.Trinity;

/**
 * A window that is blocked to one instance.
 */
public abstract class StaticWindow extends AbstractWindow {
    private boolean setSize;
    private boolean rendered;
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

        String title = this.getImGuiWindowName();
        int flags = this.windowFlags | (this.isDialog() ? ImGuiWindowFlags.NoDocking : 0);
        boolean begin;
        if (this.isDialog()) {
            if (!ImGui.isPopupOpen(title)) ImGui.openPopup(title);
            begin = ImGui.beginPopupModal(title, open, flags);
        } else if (isCloseable()) {
            begin = ImGui.begin(title, open, flags);
        } else {
            begin = ImGui.begin(title, flags);
        }
        if (begin) {
            this.rendered = true;
            renderFrame();
            if (this.isDialog() && ImGui.isWindowFocused() && ImGui.isKeyPressed(ImGuiKey.Escape, false)) {
                this.close();
            }
            this.renderChildWindows();
            if (this.isDialog()) {
                if (!this.open.get() || !this.isVisible()) ImGui.closeCurrentPopup();
                ImGui.endPopup();
            } else {
                ImGui.end();
            }
        } else if (!this.isDialog()) {
            ImGui.end();
        }
        if (!open.get()) {
            this.open.set(true);
            this.close();
        }
    }

    @Override
    public String getImGuiWindowName() {
        return getTitle() + this.getId("###BeginWnd");
    }

    @Override
    public boolean hasRendered() {
        return rendered;
    }

    public boolean isCloseable() {
        return true;
    }
}
