package me.f1nal.trinity.gui.components.popup.items;

import imgui.ImGui;

import java.util.List;

public class PopupItemMenu extends PopupItem {
    private final String label;
    private final List<PopupItem> popupItems;

    public PopupItemMenu(String label, List<PopupItem> popupItems) {
        this.label = label;
        this.popupItems = popupItems;
    }

    @Override
    public void draw() {
        if (ImGui.beginMenu(this.label)) {
            popupItems.forEach(PopupItem::draw);
            ImGui.endMenu();
        }
    }
}
