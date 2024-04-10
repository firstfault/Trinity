package me.f1nal.trinity.gui.windows.impl.assembler.action;

import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerFrame;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionComponent;
import org.lwjgl.glfw.GLFW;

public final class DeleteInstructionAction implements InstructionAction {
    @Override
    public void execute(AssemblerFrame assemblerFrame, InstructionComponent instructionComponent) {
        assemblerFrame.deleteInstruction(instructionComponent);
    }

    @Override
    public String getName() {
        return "Delete";
    }

    @Override
    public int getKey() {
        return GLFW.GLFW_KEY_DELETE;
    }
}
