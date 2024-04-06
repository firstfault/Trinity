package me.f1nal.trinity.util;

import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.ImVec2;
import imgui.type.ImBoolean;

import java.util.function.Supplier;

public class GuiUtil {
    public static void popupLogic(String strId, Runnable runnable) {
        if (ImGui.isItemHovered()) {
            if (ImGui.isMouseClicked(1)) {
                ImGui.openPopup(strId);
            }
        }
        if (ImGui.beginPopup(strId)) {
            runnable.run();
            ImGui.endPopup();
        }
    }

    public static void disabledWidget(boolean disabled, Runnable draw) {
        disabledWidget(disabled, () -> {
            draw.run();
            return true;
        });
    }

    public static <T> T disabledWidget(boolean disabled, Supplier<T> draw) {
        if (disabled) ImGui.beginDisabled();
        T t = draw.get();
        if (disabled) ImGui.endDisabled();
        return t;
    }

    public static boolean smallCheckbox(String label, ImBoolean active) {
        return smallWidget(() -> ImGui.checkbox(label, active));
    }

    public static boolean smallCheckbox(String label, boolean active) {
        return smallWidget(() -> ImGui.checkbox(label, active));
    }

    public static void smallWidget(Runnable widget) {
        smallWidget(() -> {
            widget.run();
            return true;
        });
    }

    public static <T> T smallWidget(Supplier<T> widget) {
        ImGuiStyle style = ImGui.getStyle();
        ImVec2 framePadding = style.getFramePadding();
        style.setFramePadding(framePadding.x, 0.F);
        T t = widget.get();
        style.setFramePadding(framePadding.x, framePadding.y);
        return t;
    }

    public static void tooltip(String tooltip) {
        if (ImGui.isItemHovered()) ImGui.setTooltip(tooltip);
    }

    public static void informationTooltip(String tooltip) {
        ImGui.sameLine(0.F, 0.F);
        ImGui.textDisabled(" (?)");
        tooltip(tooltip);
    }
}
