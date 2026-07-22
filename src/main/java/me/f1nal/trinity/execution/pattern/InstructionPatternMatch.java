package me.f1nal.trinity.execution.pattern;

import me.f1nal.trinity.execution.MethodInput;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;

public record InstructionPatternMatch(MethodInput method, List<AbstractInsnNode> instructions,
                                      String formattedInstructions) {
    public InstructionPatternMatch {
        instructions = List.copyOf(instructions);
    }

    public AbstractInsnNode firstInstruction() {
        return instructions.isEmpty() ? null : instructions.get(0);
    }

    public AbstractInsnNode navigationInstruction() {
        return instructions.stream().filter(instruction -> instruction.getOpcode() >= 0)
                .findFirst().orElse(firstInstruction());
    }

    public AbstractInsnNode lastInstruction() {
        return instructions.isEmpty() ? null : instructions.get(instructions.size() - 1);
    }
}
