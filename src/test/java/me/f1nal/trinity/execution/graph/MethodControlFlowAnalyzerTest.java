package me.f1nal.trinity.execution.graph;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodControlFlowAnalyzerTest {
    @Test
    void separatesConditionalBranchesIntoTrueAndFalsePaths() {
        MethodNode method = method();
        LabelNode falseBranch = new LabelNode();
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(new JumpInsnNode(Opcodes.IFEQ, falseBranch));
        method.instructions.add(new InsnNode(Opcodes.ICONST_1));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        method.instructions.add(falseBranch);
        method.instructions.add(new InsnNode(Opcodes.ICONST_2));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));

        MethodGraph.ControlFlow flow = MethodControlFlowAnalyzer.analyze(method);

        assertEquals(3, flow.blocks().size());
        assertTrue(flow.edges().stream().anyMatch(edge -> edge.fromBlock() == 0
                && edge.toBlock() == 2
                && edge.kind() == MethodGraph.FlowEdgeKind.TRUE_BRANCH));
        assertTrue(flow.edges().stream().anyMatch(edge -> edge.fromBlock() == 0
                && edge.toBlock() == 1
                && edge.kind() == MethodGraph.FlowEdgeKind.FALSE_BRANCH));
    }

    @Test
    void preservesSwitchCasesThatShareATarget() {
        MethodNode method = method();
        LabelNode first = new LabelNode();
        LabelNode shared = new LabelNode();
        LabelNode fallback = new LabelNode();
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(new LookupSwitchInsnNode(fallback,
                new int[]{1, 2, 3}, new LabelNode[]{first, shared, shared}));
        method.instructions.add(first);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.instructions.add(shared);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.instructions.add(fallback);
        method.instructions.add(new InsnNode(Opcodes.RETURN));

        MethodGraph.ControlFlow flow = MethodControlFlowAnalyzer.analyze(method);

        assertEquals(4, flow.blocks().size());
        assertTrue(flow.edges().stream().anyMatch(edge -> edge.kind() == MethodGraph.FlowEdgeKind.SWITCH
                && edge.label().equals("2, 3")));
        assertTrue(flow.edges().stream().anyMatch(edge -> edge.kind() == MethodGraph.FlowEdgeKind.SWITCH
                && edge.label().equals("default")));
    }

    @Test
    void addsExceptionPathsToHandlerBlocks() {
        MethodNode method = method();
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode handler = new LabelNode();
        method.instructions.add(start);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "sample/Target", "run", "()V", false));
        method.instructions.add(end);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.instructions.add(handler);
        method.instructions.add(new VarInsnNode(Opcodes.ASTORE, 1));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.tryCatchBlocks = List.of(new TryCatchBlockNode(
                start, end, handler, "java/lang/Exception"));

        MethodGraph.ControlFlow flow = MethodControlFlowAnalyzer.analyze(method);

        MethodGraph.BasicBlock handlerBlock = flow.blocks().stream()
                .filter(MethodGraph.BasicBlock::exceptionHandler).findFirst().orElseThrow();
        assertTrue(flow.edges().stream().anyMatch(edge -> edge.toBlock() == handlerBlock.id()
                && edge.kind() == MethodGraph.FlowEdgeKind.EXCEPTION
                && edge.label().equals("Exception")));
    }

    @Test
    void retainsLoopBackEdgesWithoutRecursing() {
        MethodNode method = method();
        LabelNode loop = new LabelNode();
        method.instructions.add(loop);
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(new JumpInsnNode(Opcodes.IFEQ, loop));
        method.instructions.add(new InsnNode(Opcodes.RETURN));

        MethodGraph.ControlFlow flow = MethodControlFlowAnalyzer.analyze(method);

        assertEquals(2, flow.blocks().size());
        assertTrue(flow.edges().stream().anyMatch(edge -> edge.fromBlock() == 0
                && edge.toBlock() == 0
                && edge.kind() == MethodGraph.FlowEdgeKind.TRUE_BRANCH));
    }

    private static MethodNode method() {
        return new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "test", "()V", null, null);
    }
}
