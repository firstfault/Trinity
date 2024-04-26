package me.f1nal.trinity.gui.windows.impl.assembler.action;

import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerFrame;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionComponent;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

public interface InstructionAction {
    void execute(AssemblerFrame assemblerFrame, InstructionComponent instructionComponent);
    String getName();
    int getKey();

    default String getKeyName() {
        return Objects.requireNonNullElse(GLFW.glfwGetKeyName(getKey(), 0), "").toUpperCase();
    }
}
