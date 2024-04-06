package me.f1nal.trinity.gui.components;

import imgui.ImColor;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import me.f1nal.trinity.execution.AccessFlags;

public class AccessFlagsEditor {
    private final AccessFlags accessFlags;

    public AccessFlagsEditor(AccessFlags accessFlags) {
        this.accessFlags = accessFlags;
    }

    public void draw() {
        ImGui.beginGroup();
        float width = 0.F;
        for (AccessFlags.Flag flag : AccessFlags.getFlags()) {
            boolean classFlag = flag.isClassFlag();
            if (!classFlag) {
                continue;
            }
            boolean set = this.accessFlags.isFlag(flag);
//            boolean warn = set && !classFlag;

            ImGui.pushStyleColor(ImGuiCol.Button, /*warn ? ImColor.rgba(234, 132, 66, 255) : */set ? ImColor.rgba(70, 175, 85, 255) : ImColor.rgba(165, 70, 60, 255));
            if (ImGui.smallButton(flag.getName())) {
                this.accessFlags.toggleFlag(flag);
            }
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip("0x" + Integer.toHexString(flag.getMask()) + /*(warn ? "\nThis flag is not a class flag yet it is set!" : "")*/"");
            }
            if ((width += ImGui.calcTextSize(flag.getName()).x + 28) >= ImGui.getWindowWidth()) {
                width = 0.F;
            } else {
                ImGui.sameLine();
            }
            ImGui.popStyleColor();
        }
        ImGui.endGroup();
    }
}
