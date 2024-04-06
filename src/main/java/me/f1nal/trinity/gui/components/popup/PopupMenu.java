package me.f1nal.trinity.gui.components.popup;

import imgui.ImColor;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import me.f1nal.trinity.gui.components.ComponentId;
import me.f1nal.trinity.gui.components.popup.items.PopupItem;

import java.util.List;

public class PopupMenu {
    private final String strId = "PopupMenu" + ComponentId.getId(this.getClass());
    private List<PopupItem> popupItems;
    private boolean open;

    public void show(PopupItemBuilder builder) {
        this.popupItems = builder.get();
        this.open = true;
    }

    public static void style(boolean start) {
        if (start) {
            ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 4.F, 4.F);
            ImGui.pushStyleColor(ImGuiCol.HeaderHovered, ImColor.rgba(78, 76, 76, 100));
        } else {
            ImGui.popStyleVar();
            ImGui.popStyleColor();
        }
    }

    public boolean draw() {
        if (this.popupItems == null) {
            return false;
        }

        style(true);
        boolean status = true;
        if (this.open) {
            this.open = false;
            ImGui.openPopup(this.strId);
        }
        if (!ImGui.beginPopup(strId)) {
            status = false;
        } else {
            for (PopupItem popupItem : popupItems) {
                popupItem.draw();
            }
            ImGui.endPopup();
        }
        style(false);

        return status;
    }
}
