package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import imgui.ImGui;
import imgui.flag.ImGuiKey;
import imgui.type.ImString;
import me.f1nal.trinity.gui.components.ComponentId;
import me.f1nal.trinity.util.GuiUtil;

import java.util.Objects;

public class DecompilerComponentRenameState {
    private static final float EMPTY_INPUT_WIDTH = 4.F;
    private static final float MAX_INPUT_WIDTH = 255.F;

    private final DecompilerComponent component;
    private ImString text;
    private String id = ComponentId.getId(this.getClass());
    private boolean focusGrabbed;

    public DecompilerComponentRenameState(DecompilerComponent component) {
        this.component = component;
        this.text = new ImString(Objects.requireNonNullElse(component.getRenameHandler().getFullName(), component.getText()), 0x200);
    }

    public void drawInputBox() {
        if (!this.focusGrabbed) {
            ImGui.setKeyboardFocusHere();
        }

        final float width = Math.min(Math.max(ImGui.calcTextSize(text.get()).x, EMPTY_INPUT_WIDTH),
                MAX_INPUT_WIDTH);
        ImGui.pushItemWidth(width);
        GuiUtil.smallWidget(() -> ImGui.inputText("###" + this.id, this.text));
        ImGui.popItemWidth();

        if (ImGui.isKeyDown(ImGuiKey.Enter) || (this.focusGrabbed && GuiUtil.isFocusLostOnItem())) {
            component.stopRenaming(this.text.get());
        } else if (ImGui.isKeyDown(ImGuiKey.Escape)) {
            component.stopRenaming(null);
        }
        this.focusGrabbed = true;
    }
}
