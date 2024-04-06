package me.f1nal.trinity.gui.frames.impl.assembler.stack;

public class InstructionStack {
    private final JavaStack stack;

    public InstructionStack(JavaStack stack) {
        this.stack = stack;
    }

    public JavaStack getStack() {
        return stack;
    }
}
