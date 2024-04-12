package me.f1nal.trinity.gui.components.popup.items;

import imgui.ImGui;
import me.f1nal.trinity.gui.components.popup.PopupMenuState;

public class PopupItemMenuItem extends PopupItem {
    private final String label;
    private final String shortcut;
    private final boolean selected;
    private final Runnable event;

    public PopupItemMenuItem(String label, String shortcut, boolean selected, Runnable event) {
        this.label = label;
        this.shortcut = shortcut;
        this.selected = selected;
        this.event = event;
    }

    public String getLabel() {
        return label;
    }

    public String getShortcut() {
        return shortcut;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    public void draw(PopupMenuState state) {
        if (ImGui.menuItem(this.label, this.shortcut, this.selected)) {
            event.run();
        }
        state.canSeparate = true;
    }
}
