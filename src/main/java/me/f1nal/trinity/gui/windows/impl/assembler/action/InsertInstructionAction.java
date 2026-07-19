package me.f1nal.trinity.gui.windows.impl.assembler.action;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerFrame;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionComponent;
import me.f1nal.trinity.keybindings.Bindable;

public final class InsertInstructionAction implements InstructionAction {
    @Override
    public void execute(AssemblerFrame assemblerFrame, InstructionComponent instructionComponent) {
        assemblerFrame.openInsertDialog(assemblerFrame.getInstructions().indexOf(instructionComponent));
    }

    @Override
    public String getName() {
        return "Insert";
    }

    @Override
    public Bindable getKeyBinding() {
        return Main.getKeyBindManager().ASSEMBLER_INSERT;
    }
}
