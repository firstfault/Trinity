package me.f1nal.trinity.gui.frames.impl.assembler.line;

import imgui.ImGui;
import imgui.ImVec2;
import me.f1nal.trinity.gui.frames.impl.assembler.InstructionComponent;

public class InstructionDrag {
    private final InstructionComponent component;
    private ImVec2 mousePos;
    private final int index;

    public InstructionDrag(InstructionComponent component, ImVec2 mousePos, int index) {
        this.component = component;
        this.mousePos = mousePos;
        this.index = index;
    }

    public InstructionComponent getComponent() {
        return component;
    }

    public ImVec2 getMousePos() {
        return mousePos;
    }

    public int getIndex() {
        return index;
    }

    public void resetMousePos() {
        this.mousePos = ImGui.getMousePos();
    }
}
