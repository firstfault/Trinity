package me.f1nal.trinity.gui.windows.api;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiFocusedFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.popup.PopupMenuBar;

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
    private boolean windowFocused;
    private boolean closeRequested;
    private boolean rendered;
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
            this.windowFocused = false;
            return;
        }
        if (!this.sizeSet) {
            ImGui.setNextWindowSize(width, height, ImGuiCond.Always);
            this.sizeSet = true;
        }
        boolean begin = this.beginWindow();
        this.rendered = true;
        this.windowFocused = begin && ImGui.isWindowFocused(ImGuiFocusedFlags.RootAndChildWindows);

        if (begin) {
            if (!focusGained) {
                this.onFocusGain();
                focusGained = true;
            }
            if (this.menuBar != null) this.menuBar.draw();
            renderFrame();
            if ((closeableByEscape || this.isDialog())
                    && ImGui.isWindowFocused(ImGuiFocusedFlags.RootAndChildWindows)
                    && ImGui.isKeyPressed(ImGuiKey.Escape, false)) {
                this.close();
            }
            this.renderChildWindows();
        } else {
            focusGained = false;
        }
        if (this.isDialog()) {
            if (begin) {
                if (!this.openState.get() || !this.isVisible()) ImGui.closeCurrentPopup();
                ImGui.endPopup();
            }
        } else {
            ImGui.end();
        }
        if (!this.openState.get()) {
            this.openState.set(true);
            this.close();
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            this.closeRequested = false;
        }
        super.setVisible(visible);

        if (!visible) {
            Main.getWindowManager().getClosableWindows().remove(this);
        }
    }

    @Override
    public void close() {
        this.closeRequested = true;
        super.close();
    }

    public boolean isCloseRequested() {
        return closeRequested;
    }

    public boolean isFocusGained() {
        return focusGained;
    }

    /** Returns whether this window or one of its children was focused on its latest rendered frame. */
    public boolean isWindowFocused() {
        return windowFocused;
    }

    protected boolean beginWindow() {
        this.applyOpeningPosition();

        int flags = this.applyDialogWindowFlags(this.windowFlags);
        if (!this.isDialog()) return ImGui.begin(this.getImGuiWindowName(), this.openState, flags);

        String name = this.getImGuiWindowName();
        if (!ImGui.isPopupOpen(name)) ImGui.openPopup(name);
        return ImGui.beginPopupModal(name, this.openState, flags);
    }

    @Override
    public String getImGuiWindowName() {
        return this.getTitle() + "###" + getClass().getName() + id;
    }

    @Override
    public boolean hasRendered() {
        return rendered;
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
