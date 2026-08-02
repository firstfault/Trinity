package me.f1nal.trinity.execution.xref.where;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiMouseButton;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.gui.components.FontSettings;
import me.f1nal.trinity.gui.components.filter.kind.IKindType;
import me.f1nal.trinity.gui.components.filter.kind.KindTooltip;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.popup.PopupMenu;
import me.f1nal.trinity.gui.navigation.NavigationAction;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerPreviewRenderer;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

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
        controls(popupMenu, trinity, highlightOwnerClass, this::menuItem);
    }

    public void controls(PopupMenu popupMenu, Trinity trinity, boolean highlightOwnerClass,
                         Supplier<PopupItemBuilder> contextMenu) {
        if (ImGui.isItemHovered()) {
            hover(highlightOwnerClass);
            if (ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left)) {
                followInDecompiler();
            } else if (ImGui.isItemClicked(ImGuiMouseButton.Right)) {
                popupMenu.show(contextMenu.get());
            }
        }
    }

    public void draw(IKindType kind, PopupMenu popupMenu, Trinity trinity) {
        draw(kind, popupMenu, trinity, false);
    }

    public void draw(IKindType kind, PopupMenu popupMenu, Trinity trinity, boolean highlightOwnerClass) {
        draw(kind, popupMenu, trinity, highlightOwnerClass, Collections.emptySet());
    }

    public void draw(IKindType kind, PopupMenu popupMenu, Trinity trinity,
                     boolean highlightOwnerClass, Collection<String> presentTypeNames) {
        draw(kind, popupMenu, trinity, highlightOwnerClass, presentTypeNames, this::menuItem);
    }

    public void draw(IKindType kind, PopupMenu popupMenu, Trinity trinity,
                     boolean highlightOwnerClass, Collection<String> presentTypeNames,
                     Supplier<PopupItemBuilder> contextMenu) {
        float rectSize = 12.F * Main.getPreferences().getDefaultFont().getSize() / 15F;
        ImGui.invisibleButton("XrefWhereButton", rectSize, rectSize);
        ImVec2 min = ImGui.getItemRectMin();
        ImVec2 max = ImGui.getItemRectMax();
        float yOffset = 1.5F;
        ImGui.getWindowDrawList().addRectFilled(min.x, min.y + yOffset, max.x, max.y + yOffset, kind.getColor(), 1.F);
        KindTooltip.draw(kind, presentTypeNames);
        ImGui.sameLine(0.F, 4.F);
        String text = getText();
        ImVec2 textSize = ImGui.calcTextSize(text);
        ImGui.invisibleButton("XrefWhereText", Math.max(1.F, textSize.x), ImGui.getTextLineHeight());
        ImVec2 textPosition = ImGui.getItemRectMin();
        ImGui.getWindowDrawList().addText(textPosition.x, textPosition.y,
                ImGui.getColorU32(ImGuiCol.Text), text);
        controls(popupMenu, trinity, highlightOwnerClass, contextMenu);
    }

    @Override
    public String toString() {
        return getText();
    }
}
