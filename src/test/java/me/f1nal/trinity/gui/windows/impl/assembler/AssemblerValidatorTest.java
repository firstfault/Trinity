package me.f1nal.trinity.gui.windows.impl.assembler;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssemblerValidatorTest {
    @Test
    void acceptsSerializableMethodAndRejectsForeignLabels() {
        ClassNode ownerNode = new ClassNode(Opcodes.ASM9);
        ownerNode.version = Opcodes.V17;
        ownerNode.access = Opcodes.ACC_PUBLIC;
        ownerNode.name = "sample/Owner";
        ownerNode.superName = "java/lang/Object";
        MethodNode valid = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "value", "()I", null, null);
        valid.instructions.add(new InsnNode(Opcodes.ICONST_1));
        valid.instructions.add(new InsnNode(Opcodes.IRETURN));
        valid.maxStack = 1;
        ownerNode.methods.add(valid);
        ClassTarget target = new ClassTarget(ownerNode.name, 0);
        ClassInput owner = new ClassInput(null, ownerNode, target);
        target.setInput(owner);

        assertTrue(AssemblerValidator.validate(owner, valid).isValid());

        MethodNode invalid = AssemblerDocument.cloneMethod(valid);
        invalid.instructions.insert(new JumpInsnNode(Opcodes.GOTO, new LabelNode()));
        AssemblerValidationResult result = AssemblerValidator.validate(owner, invalid);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("does not reference a label")));
    }
}
