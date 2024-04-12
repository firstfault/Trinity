package me.f1nal.trinity.gui.components.popup.items;

import imgui.ImGui;
import me.f1nal.trinity.gui.components.popup.PopupMenuState;

public class PopupItemSeparator extends PopupItem {
    @Override
    public void draw(PopupMenuState state) {
        if (!state.canSeparate) {
            return;
        }
        ImGui.separator();
        state.canSeparate = true;
    }
}
