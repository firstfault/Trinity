package me.f1nal.trinity.gui.components.filter.misc;

import imgui.ImGui;
import me.f1nal.trinity.gui.components.MemorableCheckboxComponent;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;

import java.awt.event.KeyEvent;

public class ShowFilterOption {
    private final MemorableCheckboxComponent showFilter;
    /**
     * Tells us if we just toggled the filter and should set focus to related items.
     */
    private boolean setFocus;

    public ShowFilterOption(String id) {
        this.showFilter = new MemorableCheckboxComponent(id.concat("ShowFilter"), false);
    }

    public boolean isSetFocus() {
        if (!this.setFocus) {
            return false;
        }

        this.setFocus = false;
        return true;
    }

    public void runControls() {
        if (ImGui.isWindowFocused() && ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(KeyEvent.VK_F)) {
            this.toggle();
        }
    }

    public MemorableCheckboxComponent getShowFilter() {
        return showFilter;
    }

    public void menuItem(PopupItemBuilder builder) {
        builder.menuItem("Show Filter", "Ctrl+F", showFilter.getState(), this::toggle);
    }

    private void toggle() {
        this.setFocus = true;
        showFilter.toggleState();
    }
}
