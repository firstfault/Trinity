package me.f1nal.trinity.gui.windows.impl.assembler.action;

import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerFrame;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionComponent;

public interface InstructionAction {
    void execute(AssemblerFrame assemblerFrame, InstructionComponent instructionComponent);
    String getName();
    int getKey();
}
