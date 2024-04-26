package me.f1nal.trinity.execution.xref.where;

import imgui.ImGui;
import imgui.ImVec2;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.filter.kind.IKindType;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.popup.PopupMenu;
import me.f1nal.trinity.util.GuiUtil;

public abstract class XrefWhere {
    private final String name;

    protected XrefWhere(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    public abstract PopupItemBuilder menuItem();
    public abstract String getText();
    public abstract void followInDecompiler();

    public void hover() {
        ImGui.beginTooltip();
        ImGui.text(getText());
        ImGui.endTooltip();
    }

    public void controls(PopupMenu popupMenu, Trinity trinity) {
        if (ImGui.isItemHovered()) {
            hover();
            if (ImGui.isItemClicked(1)) {
                popupMenu.show(menuItem());
            }
        }
    }

    public void draw(IKindType kind, PopupMenu popupMenu, Trinity trinity) {
        float rectSize = 12.F * Main.getPreferences().getDefaultFont().getSize() / 15F;
        ImGui.invisibleButton("XrefWhereButton", rectSize, rectSize);
        ImVec2 min = ImGui.getItemRectMin();
        ImVec2 max = ImGui.getItemRectMax();
        float yOffset = 1.5F;
        ImGui.getWindowDrawList().addRectFilled(min.x, min.y + yOffset, max.x, max.y + yOffset, kind.getColor(), 1.F);
        GuiUtil.tooltip(kind.getName());
        ImGui.sameLine(0.F, 4.F);
        ImGui.text(getText());
        controls(popupMenu, trinity);
    }

    @Override
    public String toString() {
        return getText();
    }
}
