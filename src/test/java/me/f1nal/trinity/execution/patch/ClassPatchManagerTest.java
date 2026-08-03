package me.f1nal.trinity.execution.patch;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassPatchManagerTest {
    @Test
    void enumFieldOrderingFollowsTheRequestedPresentation() {
        ClassNode enumView = enumWithOrdinaryFieldFirst();
        ClassPatchManager.patchForDecompilation(enumView, false);
        assertEquals("VALUE", enumView.fields.get(0).name);

        ClassNode classView = enumWithOrdinaryFieldFirst();
        ClassPatchManager.patchForDecompilation(classView, true);
        assertEquals("other", classView.fields.get(0).name);
    }

    private static ClassNode enumWithOrdinaryFieldFirst() {
        ClassNode node = new ClassNode();
        node.access = Opcodes.ACC_ENUM;
        node.fields.add(new FieldNode(0, "other", "I", null, null));
        node.fields.add(new FieldNode(Opcodes.ACC_ENUM, "VALUE", "Lexample/Kind;", null, null));
        return node;
    }
}
