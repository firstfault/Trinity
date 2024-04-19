package me.f1nal.trinity.gui.windows.impl.assembler.args;

import imgui.ImVec4;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.gui.windows.impl.assembler.fields.InstructionField;

import java.util.ArrayList;
import java.util.List;

public abstract class InstructionOperand {
    /**
     * Instruction details shown in the list of all instructions.
     */
    private final List<ColoredString> detailsText = new ArrayList<>();
    private final List<InstructionField> fields = new ArrayList<>();
    private ImVec4 bounds;

    public void setBounds(ImVec4 bounds) {
        this.bounds = bounds;
    }

    public ImVec4 getBounds() {
        return bounds;
    }

    public List<ColoredString> getDetailsText() {
        return detailsText;
    }

    public List<InstructionField> getFields() {
        return fields;
    }

    public abstract InstructionOperand copy();
}
