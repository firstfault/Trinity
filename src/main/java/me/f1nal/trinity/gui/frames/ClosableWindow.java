package me.f1nal.trinity.gui.frames;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.popup.PopupMenuBar;

/**
 * A window that can be opened or closed, possibly with multiple instances.
 */
public abstract class ClosableWindow extends Frame {
    private boolean visible, closed;
    protected final ImBoolean openState = new ImBoolean(true);
    private Runnable closeEvent;
    private boolean closeableByEscape;
    private final int id;
    private static int idCounter;
    private boolean sizeSet;
    protected int windowFlags;
    private boolean focusGained = false;
    private PopupMenuBar menuBar;


    protected ClosableWindow(String title, float width, float height, Trinity trinity) {
        super(title, width, height, trinity);
        id = ++idCounter;
        this.windowFlags |= ImGuiWindowFlags.NoSavedSettings;
    }

    public void setMenuBar(PopupMenuBar menuBar) {
        this.menuBar = menuBar;
        this.windowFlags |= ImGuiWindowFlags.MenuBar;
    }

    public String getId(String suffix) {
        return suffix.concat(this.getClass().getName()).concat(String.valueOf(this.getId()));
    }

    public int getId() {
        return id;
    }

    public void setCloseableByEscape(boolean closeableByEscape) {
        this.closeableByEscape = closeableByEscape;
    }

    @Override
    public void render() {
        if (!visible) {
            this.close();
            return;
        }
        if (!this.sizeSet) {
            ImGui.setNextWindowSize(width, height, ImGuiCond.Always);
            this.sizeSet = true;
        }
        boolean begin = this.beginWindow();

        if (begin) {
            if (!focusGained) {
                this.onFocusGain();
                focusGained = true;
            }
            if (this.menuBar != null) this.menuBar.draw();
            renderFrame();
            if (closeableByEscape && ImGui.isWindowFocused() && ImGui.isKeyDown(ImGui.getKeyIndex(ImGuiKey.Escape))) {
                this.close();
            }
        } else {
            focusGained = false;
        }
        ImGui.end();
        if (!this.openState.get()) {
            this.openState.set(true);
            this.close();
        }
    }

    public boolean isFocusGained() {
        return focusGained;
    }

    protected boolean beginWindow() {
        return ImGui.begin(this.getTitle() + "###" + getClass().getName() + id, this.openState, windowFlags);
    }

    protected void onFocusGain() {

    }

    public void close() {
        this.setVisible(false);
        closed = true;
    }

    public void setCloseEvent(Runnable closeEvent) {
        this.closeEvent = closeEvent;
        if (closed) setVisible(false);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;

        if (!visible) {
            if (this.closeEvent == null) {
                return;
            }
            this.closeEvent.run();
            this.closeEvent = null;
        }
    }

    public boolean isVisible() {
        return visible;
    }
}
