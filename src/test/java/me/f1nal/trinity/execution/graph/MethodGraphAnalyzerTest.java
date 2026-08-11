package me.f1nal.trinity.execution.graph;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.util.UnsafeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodGraphAnalyzerTest {
    private Execution execution;
    private MethodInput first;
    private MethodInput second;
    private MethodInput third;

    @BeforeEach
    void createCallGraph() throws Exception {
        execution = emptyExecution();
        first = install("sample/First", method("run",
                call("sample/Second", "run"),
                call("outside/Library", "work"),
                new InsnNode(Opcodes.RETURN)));
        second = install("sample/Second", method("run",
                call("sample/Third", "run"),
                new InsnNode(Opcodes.RETURN)));
        third = install("sample/Third", method("run",
                call("sample/First", "run"),
                new InsnNode(Opcodes.RETURN)));
    }

    @Test
    void depthLimitsResolvedAndExternalCallees() {
        MethodGraph graph = analyze(first, 1, MethodGraph.Direction.CALLS, true);

        assertEquals(3, graph.nodes().size());
        assertTrue(graph.nodes().containsKey(MethodGraphAnalyzer.key(first)));
        assertTrue(graph.nodes().containsKey(MethodGraphAnalyzer.key(second)));
        assertTrue(graph.nodes().keySet().stream()
                .anyMatch(key -> key.owner().equals("outside/Library")));
        assertFalse(graph.nodes().containsKey(MethodGraphAnalyzer.key(third)));
    }

    @Test
    void infiniteDepthTerminatesAtCycles() {
        MethodGraph graph = analyze(first, MethodGraphAnalyzer.INFINITE_DEPTH,
                MethodGraph.Direction.CALLS, true);

        assertEquals(4, graph.nodes().size());
        assertTrue(graph.calls().stream().anyMatch(edge ->
                edge.caller().equals(MethodGraphAnalyzer.key(third))
                        && edge.callee().equals(MethodGraphAnalyzer.key(first))));
    }

    @Test
    void callerDirectionWalksBackToEntryMethods() {
        MethodGraph graph = analyze(third, 2, MethodGraph.Direction.CALLERS, false);

        assertEquals(3, graph.nodes().size());
        assertTrue(graph.nodes().containsKey(MethodGraphAnalyzer.key(first)));
        assertTrue(graph.nodes().containsKey(MethodGraphAnalyzer.key(second)));
        assertTrue(graph.nodes().get(MethodGraphAnalyzer.key(first)).rank() < 0);
    }

    @Test
    void collapsedContentProducesACompactCallGraphWithoutControlFlow() {
        MethodGraph expanded = analyze(first, 2, MethodGraph.Direction.CALLS, true);
        MethodGraph collapsed = analyze(first, 2, MethodGraph.Direction.CALLS, true, true);

        assertTrue(collapsed.methodContentCollapsed());
        assertEquals(expanded.nodes().keySet(), collapsed.nodes().keySet());
        assertEquals(expanded.calls(), collapsed.calls());
        assertEquals(0, collapsed.basicBlockCount());
        assertEquals(0, collapsed.flowEdgeCount());
        assertTrue(collapsed.nodes().values().stream().allMatch(node ->
                node.flow() == null && node.blocks().isEmpty()));
        assertTrue(collapsed.nodes().get(MethodGraphAnalyzer.key(first)).height()
                < expanded.nodes().get(MethodGraphAnalyzer.key(first)).height());
    }

    private MethodGraph analyze(MethodInput root, int depth,
                                MethodGraph.Direction direction, boolean external) {
        return analyze(root, depth, direction, external, false);
    }

    private MethodGraph analyze(MethodInput root, int depth,
                                MethodGraph.Direction direction, boolean external,
                                boolean collapseMethodContent) {
        return new MethodGraphAnalyzer(execution).analyze(root,
                new MethodGraphAnalyzer.Request(depth, direction, external,
                        collapseMethodContent, 16.F, 7.F),
                () -> false);
    }

    private MethodInput install(String name, MethodNode method) {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = name;
        node.superName = "java/lang/Object";
        node.methods.add(method);
        ClassTarget target = new ClassTarget(name, 0);
        ClassInput input = new ClassInput(execution, node, target);
        target.setInput(input);
        MethodInput methodInput = new MethodInput(method, input);
        input.addInput(methodInput);
        execution.addClassTarget(target);
        execution.getClassList().add(input);
        return methodInput;
    }

    private static MethodNode method(String name, org.objectweb.asm.tree.AbstractInsnNode... instructions) {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                name, "()V", null, null);
        for (org.objectweb.asm.tree.AbstractInsnNode instruction : instructions) {
            method.instructions.add(instruction);
        }
        return method;
    }

    private static MethodInsnNode call(String owner, String name) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, "()V", false);
    }

    private static Execution emptyExecution() throws Exception {
        Execution execution = (Execution) UnsafeUtil.getUnsafe().allocateInstance(Execution.class);
        setField(execution, "classTargetMap", new HashMap<String, ClassTarget>());
        setField(execution, "classInputList", new ArrayList<ClassInput>());
        return execution;
    }

    private static void setField(Execution execution, String name, Object value) throws Exception {
        Field field = Execution.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(execution, value);
    }
}
