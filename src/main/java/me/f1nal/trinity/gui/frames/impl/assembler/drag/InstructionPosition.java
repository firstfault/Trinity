package me.f1nal.trinity.gui.frames.impl.assembler.drag;

import me.f1nal.trinity.gui.frames.impl.assembler.InstructionComponent;
import me.f1nal.trinity.gui.frames.impl.assembler.InstructionList;

public class InstructionPosition {
    private final InstructionList list;
    private final InstructionComponent component;
    private final int index;

    public InstructionPosition(InstructionList list, InstructionComponent component, int index) {
        this.list = list;
        this.component = component;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public InstructionComponent getComponent() {
        return component;
    }

    public InstructionList getList() {
        return list;
    }
}
