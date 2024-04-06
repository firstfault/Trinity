package me.f1nal.trinity.gui.components.popup.items;

import imgui.ImGui;

public class PopupItemSeparator extends PopupItem {
    @Override
    public void draw() {
        ImGui.separator();
    }
}
