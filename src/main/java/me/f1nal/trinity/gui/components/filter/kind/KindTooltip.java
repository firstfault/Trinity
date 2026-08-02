package me.f1nal.trinity.gui.components.filter.kind;

import imgui.ImGui;
import imgui.flag.ImGuiStyleVar;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Draws the hover details for a kind filter entry or xref kind marker. */
public final class KindTooltip {
    private static final float DESCRIPTION_WIDTH = 360.F;

    private KindTooltip() {
    }

    public static void draw(IKindType kind) {
        draw(kind, Collections.emptySet());
    }

    public static void draw(IKindType kind, Collection<String> presentTypeNames) {
        if (!ImGui.isItemHovered()) return;
        if (!(kind instanceof IDetailedKindType detailed)) {
            ImGui.setTooltip(kind.getName());
            return;
        }

        ImGui.beginTooltip();
        ImGui.textColored(kind.getColor(), detailed.getName());
        ImGui.pushTextWrapPos(ImGui.getCursorPosX() + DESCRIPTION_WIDTH);
        ImGui.textColored(CodeColorScheme.TEXT, detailed.getDescription());
        ImGui.popTextWrapPos();
        ImGui.separator();
//        ImGui.textColored(CodeColorScheme.DISABLED, "Types");
        drawTypes(detailed, presentTypeNames);
        ImGui.endTooltip();
    }

    private static void drawTypes(IDetailedKindType detailed,
                                  Collection<String> presentTypeNames) {
        List<String> types = detailed.getTypeNames();
        float rightEdge = ImGui.getCursorScreenPosX() + DESCRIPTION_WIDTH;
        boolean first = true;
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 6.F, 1.F);

        for (String type : types) {
            boolean present = presentTypeNames.stream()
                    .anyMatch(resultType -> detailed.matchesTypeName(type, resultType));
            int color = present ? CodeColorScheme.TEXT : CodeColorScheme.DISABLED;
            float rowHeight = ImGui.getTextLineHeight();
            float markerSize = Math.max(5.F, rowHeight * 0.38F);
            float itemWidth = markerSize + 4.F + ImGui.calcTextSize(type).x;
            if (!first && ImGui.getItemRectMaxX() + 6.F + itemWidth <= rightEdge) {
                ImGui.sameLine(0.F, 6.F);
            }
            float markerX = ImGui.getCursorScreenPosX();
            float markerY = ImGui.getCursorScreenPosY()
                    + (rowHeight - markerSize) * 0.5F + 1.F;
            ImGui.getWindowDrawList().addRectFilled(
                    markerX, markerY, markerX + markerSize, markerY + markerSize,
                    color, 1.5F);
            ImGui.dummy(markerSize, rowHeight);
            ImGui.sameLine(0.F, 4.F);
            ImGui.textColored(color, type);
            first = false;
        }
        ImGui.popStyleVar();
    }
}
