package me.f1nal.trinity.gui.frames.impl.assembler.args;

import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.gui.frames.impl.assembler.fields.InstructionField;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractInsnArgument {
    /**
     * Instruction details shown in the list of all instructions.
     */
    private final List<ColoredString> detailsText = new ArrayList<>();
    private final List<InstructionField> fields = new ArrayList<>();

    public List<ColoredString> getDetailsText() {
        return detailsText;
    }

    public List<InstructionField> getFields() {
        return fields;
    }

    public abstract AbstractInsnArgument copy();
}
