package me.f1nal.trinity.gui.frames.impl.assembler.stack.processor;

import me.f1nal.trinity.gui.frames.impl.assembler.stack.InstructionStack;
import me.f1nal.trinity.gui.frames.impl.assembler.stack.JavaStack;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;

public abstract class InstructionStackProcessorManager {
    private static final InstructionStackProcessor<?>[] processors = new InstructionStackProcessor[256];

    public static InstructionStack getStack(AbstractInsnNode insnNode, JavaStack stack) {
        if (insnNode.getOpcode() != -1) {
            InstructionStackProcessor<?> processor = processors[insnNode.getOpcode()];
            processor.process(insnNode, stack);
        }
        return new InstructionStack(stack);
    }

    static {
        processors[Opcodes.NOP] = new InstructionStackProcessor<InsnNode>();
//        processors[Opcodes.ACONST_NULL] = new InstructionStackProcessor<InsnNode>().stack(s -> s.push(new StackElement(Type.OBJECT)));
    }
}
