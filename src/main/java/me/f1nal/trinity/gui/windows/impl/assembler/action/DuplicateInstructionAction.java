package me.f1nal.trinity.gui.windows.impl.assembler.action;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerFrame;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionComponent;
import me.f1nal.trinity.keybindings.Bindable;

public final class DuplicateInstructionAction implements InstructionAction {
    @Override
    public void execute(AssemblerFrame assemblerFrame, InstructionComponent instructionComponent) {
        assemblerFrame.duplicateInstruction(instructionComponent);
    }

    @Override
    public String getName() {
        return "Duplicate";
    }

    @Override
    public Bindable getKeyBinding() {
        return Main.getKeyBindManager().ASSEMBLER_DUPLICATE;
    }
}
