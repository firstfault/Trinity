package me.f1nal.trinity.gui.frames.impl.entryviewer.impl.decompiler;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import me.f1nal.trinity.util.GuiUtil;

import java.util.function.Supplier;

public class DecompilerLineText {
    private final String text;
    private final DecompilerComponent component;

    public DecompilerLineText(String text, DecompilerComponent component) {
        this.text = text;
        this.component = component;
    }

    public DecompilerComponent getComponent() {
        return component;
    }

    public String getText() {
        return text;
    }

    public void render() {
        final DecompilerComponentRenameState renameState = component.getRenameState();

        ImGui.pushStyleColor(ImGuiCol.Text, this.component.getColorFunction().get());

        if (renameState != null) {
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0.F, 0.F);
            ImGui.pushStyleColor(ImGuiCol.FrameBg, ImGui.getColorU32(ImGuiCol.WindowBg));

            renameState.drawInputBox();

            ImGui.popStyleVar();
            ImGui.popStyleColor();
        } else {
            ImGui.text(text);
        }

        ImGui.popStyleColor();
    }
}