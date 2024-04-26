package me.f1nal.trinity.gui.windows.api;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.popup.PopupMenuBar;
import org.checkerframework.checker.units.qual.C;

/**
 * A window that can be opened or closed, possibly with multiple instances.
 */
public abstract class ClosableWindow extends AbstractWindow {
    protected final ImBoolean openState = new ImBoolean(true);
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

    public PopupMenuBar getMenuBar() {
        return menuBar;
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
        if (!this.isVisible()) {
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

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (!visible) {
            Main.getWindowManager().getClosableWindows().remove(this);
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

    /**
     * This method is meant to be overridden by inheritors.
     * <p>
     *     This check is performed before adding another {@link ClosableWindow} to the window manager. If {@code true} is returned, then
     *     adding of the aforementioned new window is blocked.
     * </p>
     * <p>
     *     Parent method will always return {@code false}
     * </p>
     * @param otherWindow Window to check.
     * @return If another window of this type is open.
     */
    public boolean isAlreadyOpen(ClosableWindow otherWindow) {
        return false;
    }
}
