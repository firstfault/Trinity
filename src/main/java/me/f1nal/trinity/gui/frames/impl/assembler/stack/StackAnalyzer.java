package me.f1nal.trinity.gui.frames.impl.assembler.stack;

import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class analyzing stack of a list of instructions.
 */
public class StackAnalyzer {
    private final Map<AbstractInsnNode, InstructionStack> stackMap = new LinkedHashMap<>();

    public StackAnalyzer(Collection<AbstractInsnNode> instructions) {

    }
}
