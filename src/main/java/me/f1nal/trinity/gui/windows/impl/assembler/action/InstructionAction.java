package me.f1nal.trinity.gui.windows.impl.assembler.action;

import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerFrame;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionComponent;
import me.f1nal.trinity.keybindings.Bindable;

public interface InstructionAction {
    void execute(AssemblerFrame assemblerFrame, InstructionComponent instructionComponent);
    String getName();
    Bindable getKeyBinding();

    default boolean isKeyPressed() {
        return this.getKeyBinding().isPressed();
    }

    default String getKeyName() {
        return this.getKeyBinding().getKeyName();
    }
}
