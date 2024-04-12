package me.f1nal.trinity.gui.components.popup.items;

import imgui.ImGui;
import me.f1nal.trinity.gui.components.popup.PopupMenuState;

import java.util.List;

public class PopupItemMenu extends PopupItem {
    private final String label;
    private final List<PopupItem> popupItems;

    public PopupItemMenu(String label, List<PopupItem> popupItems) {
        this.label = label;
        this.popupItems = popupItems;
    }

    @Override
    public void draw(PopupMenuState state) {
        if (ImGui.beginMenu(this.label)) {
            PopupMenuState newState = new PopupMenuState();
            popupItems.forEach(popupItem -> popupItem.draw(newState));
            ImGui.endMenu();
        }
        state.canSeparate = true;
    }
}
