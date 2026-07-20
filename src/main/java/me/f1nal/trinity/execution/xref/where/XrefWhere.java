package me.f1nal.trinity.execution.xref.where;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiMouseButton;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.gui.components.FontSettings;
import me.f1nal.trinity.gui.components.filter.kind.IKindType;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.popup.PopupMenu;
import me.f1nal.trinity.gui.navigation.NavigationAction;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerPreviewRenderer;
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
    public final void followInDecompiler() {
        followInDecompiler(NavigationAction.FOLLOW_XREF);
    }

    public abstract void followInDecompiler(NavigationAction action);

    public Input<?> getInput() {
        return null;
    }

    protected void drawPreview(DecompilerPreviewRenderer renderer, Input<?> input,
                               boolean highlightOwnerClass) {
        renderer.drawInputPreview(input);
    }

    public void hover() {
        hover(false);
    }

    public void hover(boolean highlightOwnerClass) {
        FontSettings font = Main.getPreferences().getDecompilerFont();
        font.pushFont();
        ImGui.beginTooltip();
        Input<?> input = getInput();
        if (input == null) {
            ImGui.text(getText());
        } else {
            DecompilerPreviewRenderer renderer = new DecompilerPreviewRenderer(Main.getTrinity());
            drawPreview(renderer, input, highlightOwnerClass);
            renderer.finish();
        }
        ImGui.endTooltip();
        font.popFont();
    }

    public void controls(PopupMenu popupMenu, Trinity trinity) {
        controls(popupMenu, trinity, false);
    }

    public void controls(PopupMenu popupMenu, Trinity trinity, boolean highlightOwnerClass) {
        if (ImGui.isItemHovered()) {
            hover(highlightOwnerClass);
            if (ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left)) {
                followInDecompiler();
            } else if (ImGui.isItemClicked(ImGuiMouseButton.Right)) {
                popupMenu.show(menuItem());
            }
        }
    }

    public void draw(IKindType kind, PopupMenu popupMenu, Trinity trinity) {
        draw(kind, popupMenu, trinity, false);
    }

    public void draw(IKindType kind, PopupMenu popupMenu, Trinity trinity, boolean highlightOwnerClass) {
        float rectSize = 12.F * Main.getPreferences().getDefaultFont().getSize() / 15F;
        ImGui.invisibleButton("XrefWhereButton", rectSize, rectSize);
        ImVec2 min = ImGui.getItemRectMin();
        ImVec2 max = ImGui.getItemRectMax();
        float yOffset = 1.5F;
        ImGui.getWindowDrawList().addRectFilled(min.x, min.y + yOffset, max.x, max.y + yOffset, kind.getColor(), 1.F);
        GuiUtil.tooltip(kind.getName());
        ImGui.sameLine(0.F, 4.F);
        ImGui.text(getText());
        controls(popupMenu, trinity, highlightOwnerClass);
    }

    @Override
    public String toString() {
        return getText();
    }
}
