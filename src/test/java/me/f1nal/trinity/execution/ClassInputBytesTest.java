package me.f1nal.trinity.execution;

import me.f1nal.trinity.events.EventClassModified;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassInputBytesTest {
    @Test
    void retainsOriginalBytesUntilBytecodeIsModified() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "example/Test", null, "java/lang/Object", null);
        writer.visitEnd();
        byte[] original = writer.toByteArray();
        ClassNode node = new ClassNode();
        new ClassReader(original).accept(node, 0);
        ClassTarget target = new ClassTarget(node.name, original.length);
        ClassInput input = new ClassInput(null, node, target, original, "custom/Test.class", false);
        target.setInput(input);

        assertFalse(input.isRebuildRequired());
        assertArrayEquals(original, input.getExportBytes());
        assertArrayEquals(original, target.extract());

        new EventClassModified(input);
        assertTrue(input.isRebuildRequired());
        assertTrue(input.getBytecodeRevision() > 0);
    }

    @Test
    void doesNotClearNewerChangesAfterAnExportFinishes() {
        ClassNode node = new ClassNode();
        node.name = "example/Test";
        ClassTarget target = new ClassTarget(node.name, 0);
        ClassInput input = new ClassInput(null, node, target);
        target.setInput(input);
        long exportRevision = input.getBytecodeRevision();

        input.markRebuildRequired();
        input.markRebuilt(new byte[]{1}, "example/Test.class", exportRevision);

        assertTrue(input.isRebuildRequired());
    }
}
