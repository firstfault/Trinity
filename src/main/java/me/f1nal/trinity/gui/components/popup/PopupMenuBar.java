package me.f1nal.trinity.gui.components.popup;

import imgui.ImGui;
import me.f1nal.trinity.gui.components.popup.items.PopupItem;

import java.util.List;

public class PopupMenuBar {
    private List<PopupItem> popupItems;

    public PopupMenuBar(PopupItemBuilder builder) {
        this.set(builder);
    }

    public void set(PopupItemBuilder builder) {
        this.popupItems = builder.get();
    }

    public void draw() {
        PopupMenu.style(true);
        ImGui.beginMenuBar();
        for (PopupItem popupItem : popupItems) {
            popupItem.draw();
        }
        ImGui.endMenuBar();
        PopupMenu.style(false);
    }
}
