package me.f1nal.trinity.execution.asm;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsmValueWalkerTest {
    @Test
    void walksNestedAnnotationsDynamicConstantsHandlesAndArguments() {
        Handle bootstrap = new Handle(
                Opcodes.H_INVOKESTATIC, "bootstrap/Owner", "make", "()V", false);
        ConstantDynamic dynamic = new ConstantDynamic(
                "value", "Ljava/lang/Object;", bootstrap,
                Type.getType("Largument/Type;"));
        AnnotationNode nested = new AnnotationNode("Lannotation/Nested;");
        nested.values = new ArrayList<>(List.of("value", dynamic));
        AnnotationNode root = new AnnotationNode("Lannotation/Root;");
        root.values = new ArrayList<>(List.of("nested", nested));
        List<Object> visited = new ArrayList<>();

        AsmValueWalker.walk(root, visited::add);

        assertTrue(visited.containsAll(List.of(root, nested, dynamic, bootstrap,
                Type.getType("Largument/Type;"))));
    }

    @Test
    void stopsAtIdentityCyclesWithoutDroppingTheRoot() {
        List<Object> cycle = new ArrayList<>();
        cycle.add(cycle);
        List<Object> visited = new ArrayList<>();

        AsmValueWalker.walk(cycle, visited::add);

        assertEquals(1, visited.size());
        assertSame(cycle, visited.get(0));
    }
}
