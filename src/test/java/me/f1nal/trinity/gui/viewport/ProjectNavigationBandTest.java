package me.f1nal.trinity.gui.viewport;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectNavigationBandTest {
    @Test
    void estimatesEncodedInstructionsWithoutCountingMetadata() {
        ClassNode owner = new ClassNode();
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "example", "()V", null, null);
        LabelNode label = new LabelNode();
        method.instructions.add(label);
        method.instructions.add(new LineNumberNode(1, label));
        method.instructions.add(new InsnNode(Opcodes.NOP));
        method.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 10));
        method.instructions.add(new IntInsnNode(Opcodes.SIPUSH, 1000));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        owner.methods.add(method);

        assertEquals(7L, ProjectNavigationBand.estimateExecutableBytecodeSize(owner));
    }
}
