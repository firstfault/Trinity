package me.f1nal.trinity.gui.frames.impl.assembler.line;

import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;

public class MethodOpcodeSource {
    private final List<AbstractInsnNode> componentLinkedInstructions;
    private final int opcode;

    public MethodOpcodeSource(List<AbstractInsnNode> componentLinkedInstructions, int opcode) {
        this.componentLinkedInstructions = componentLinkedInstructions;
        this.opcode = opcode;
    }

    public List<AbstractInsnNode> getComponentLinkedInstructions() {
        return componentLinkedInstructions;
    }

    public int getOpcode() {
        return opcode;
    }
}
