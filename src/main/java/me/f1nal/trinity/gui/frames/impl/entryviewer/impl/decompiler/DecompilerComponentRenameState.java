package me.f1nal.trinity.gui.frames.impl.entryviewer.impl.decompiler;

import imgui.ImGui;
import imgui.flag.ImGuiKey;
import imgui.type.ImString;
import me.f1nal.trinity.gui.components.ComponentId;
import me.f1nal.trinity.util.GuiUtil;

import java.util.Objects;

public class DecompilerComponentRenameState {
    private final DecompilerComponent component;
    private ImString text;
    private String id = ComponentId.getId(this.getClass());
    private boolean focusGrabbed;
    private float minWidth = 25.F;

    public DecompilerComponentRenameState(DecompilerComponent component) {
        this.component = component;
        this.text = new ImString(Objects.requireNonNullElse(component.getRenameHandler().getFullName(), component.getText()), 0x200);
        this.minWidth = Math.max(ImGui.calcTextSize(component.getText()).x, this.minWidth);
    }

    public void drawInputBox() {
        if (!this.focusGrabbed) {
            ImGui.setKeyboardFocusHere();
        }

        final float width = Math.min(Math.max(ImGui.calcTextSize(text.get()).x + 8.F, this.minWidth), 255);
        ImGui.pushItemWidth(width);
        GuiUtil.smallWidget(() -> ImGui.inputText("###" + this.id, this.text));
        ImGui.popItemWidth();

        if (ImGui.isKeyDown(ImGui.getKeyIndex(ImGuiKey.Enter)) || (this.focusGrabbed && GuiUtil.isMouseClickedElsewhere())) {
            component.stopRenaming(this.text.get());
        } else if (ImGui.isKeyDown(ImGui.getKeyIndex(ImGuiKey.Escape))) {
            component.stopRenaming(null);
        }
        this.focusGrabbed = true;
    }
}
