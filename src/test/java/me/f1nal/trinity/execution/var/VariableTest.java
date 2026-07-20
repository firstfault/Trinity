package me.f1nal.trinity.execution.var;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.MethodInput;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariableTest {
    @Test
    void refusesEmptyAndWhitespaceOnlyNames() {
        Variable variable = variable();

        assertFalse(variable.setName(""));
        assertFalse(variable.setName("   "));
        assertEquals("var0", variable.getName());
    }

    @Test
    void retainsTheLastValidNameIfAnEditorBufferBecomesEmpty() {
        Variable variable = variable();
        assertTrue(variable.setName("counter"));

        variable.getNameProperty().set("");

        assertEquals("counter", variable.getName());
    }

    @Test
    void refusesAnotherVariableNameInTheSameMethod() {
        VariableTable table = variableTable();
        Variable first = table.getVariable(0);
        Variable second = table.getVariable(1);
        assertTrue(first.setName("counter"));
        assertTrue(second.setName("value"));

        assertFalse(second.setName("counter"));
        assertEquals("value", second.getName());

        second.getNameProperty().set("counter");
        assertEquals("value", second.getName());
        assertEquals("value", second.getNameProperty().get());
    }

    private static Variable variable() {
        return variableTable().getVariable(0);
    }

    private static VariableTable variableTable() {
        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        classNode.name = "sample/Owner";
        classNode.superName = "java/lang/Object";
        ClassTarget target = new ClassTarget(classNode.name, 0);
        ClassInput owner = new ClassInput(null, classNode, target);
        MethodInput method = new MethodInput(
                new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC, "test", "()V", null, null), owner);
        return method.getVariableTable();
    }
}
