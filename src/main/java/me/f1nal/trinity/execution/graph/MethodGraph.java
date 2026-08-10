package me.f1nal.trinity.execution.graph;

import me.f1nal.trinity.execution.MethodInput;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;
import java.util.Map;

/** Immutable call graph with a control-flow graph embedded in every resolved method. */
public record MethodGraph(MethodKey root,
                          Map<MethodKey, MethodNode> nodes,
                          List<CallEdge> calls,
                          Bounds bounds,
                          int basicBlockCount,
                          int flowEdgeCount) {
    public MethodGraph {
        nodes = Map.copyOf(nodes);
        calls = List.copyOf(calls);
    }

    public enum Direction {
        CALLS("Calls"),
        CALLERS("Callers"),
        BOTH("Both");

        private final String label;

        Direction(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public record MethodKey(String owner, String name, String descriptor) {
        public String displayOwner() {
            int separator = owner.lastIndexOf('/');
            return separator == -1 ? owner : owner.substring(separator + 1);
        }

        public String symbol() {
            return owner + '.' + name + descriptor;
        }
    }

    public record MethodNode(MethodKey key,
                             MethodInput method,
                             int depth,
                             int rank,
                             boolean root,
                             ControlFlow flow,
                             float x,
                             float y,
                             float width,
                             float height,
                             List<BlockLayout> blocks) {
        public MethodNode {
            blocks = List.copyOf(blocks);
        }

        public boolean external() {
            return method == null;
        }
    }

    public record CallEdge(MethodKey caller,
                           MethodKey callee,
                           int callSites,
                           boolean dynamicDispatch) {
    }

    public record ControlFlow(List<BasicBlock> blocks,
                              List<FlowEdge> edges,
                              int instructionCount) {
        public ControlFlow {
            blocks = List.copyOf(blocks);
            edges = List.copyOf(edges);
        }
    }

    public record BasicBlock(int id,
                             List<InstructionLine> instructions,
                             boolean exceptionHandler) {
        public BasicBlock {
            instructions = List.copyOf(instructions);
        }
    }

    public record InstructionLine(int instructionIndex,
                                  String text,
                                  InstructionKind kind,
                                  AbstractInsnNode instruction) {
    }

    public enum InstructionKind {
        NORMAL,
        DATA,
        CALL,
        BRANCH,
        TERMINAL
    }

    public record FlowEdge(int fromBlock,
                           int toBlock,
                           FlowEdgeKind kind,
                           String label) {
    }

    public enum FlowEdgeKind {
        FALLTHROUGH,
        TRUE_BRANCH,
        FALSE_BRANCH,
        JUMP,
        SWITCH,
        EXCEPTION
    }

    public record BlockLayout(int blockId,
                              float x,
                              float y,
                              float width,
                              float height,
                              int visibleInstructionCount,
                              int hiddenInstructionCount) {
    }

    public record Bounds(float minX, float minY, float maxX, float maxY) {
        public float width() {
            return maxX - minX;
        }

        public float height() {
            return maxY - minY;
        }
    }
}
