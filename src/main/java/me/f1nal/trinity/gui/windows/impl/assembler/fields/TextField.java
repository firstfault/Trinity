package me.f1nal.trinity.gui.windows.impl.assembler.fields;

import imgui.ImGui;
import imgui.type.ImString;
import me.f1nal.trinity.gui.components.ComponentId;

public class TextField extends InstructionField {
    private final String label;
    private final ImString text;
    private final String id = ComponentId.getId(this.getClass());

    public TextField(String label, ImString text) {
        this.label = label;
        this.text = text;
    }
    public String getLabel() {
        return label;
    }

    public ImString getText() {
        return text;
    }

    @Override
    public void draw() {
        ImGui.inputText(this.getLabel() + "###" + this.id, this.getText());
    }
}
