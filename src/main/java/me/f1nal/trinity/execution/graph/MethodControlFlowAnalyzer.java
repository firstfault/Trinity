package me.f1nal.trinity.execution.graph;

import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerClipboardCodec;
import me.f1nal.trinity.util.NameUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Builds JVM basic blocks and every statically representable edge between them. */
public final class MethodControlFlowAnalyzer {
    private MethodControlFlowAnalyzer() {
    }

    public static MethodGraph.ControlFlow analyze(MethodNode method) {
        AbstractInsnNode[] all = method.instructions.toArray();
        List<AbstractInsnNode> executable = new ArrayList<>();
        Map<AbstractInsnNode, Integer> executableIndex = new IdentityHashMap<>();
        Map<AbstractInsnNode, Integer> fullIndex = new IdentityHashMap<>();
        for (int index = 0; index < all.length; index++) {
            AbstractInsnNode instruction = all[index];
            fullIndex.put(instruction, index);
            if (instruction.getOpcode() >= 0) {
                executableIndex.put(instruction, executable.size());
                executable.add(instruction);
            }
        }
        if (executable.isEmpty()) {
            return new MethodGraph.ControlFlow(
                    List.of(new MethodGraph.BasicBlock(0, List.of(), false)), List.of(), 0);
        }

        Map<LabelNode, Integer> labelTargets = labelTargets(all, executableIndex);
        Set<Integer> leaders = new LinkedHashSet<>();
        leaders.add(0);
        for (int index = 0; index < executable.size(); index++) {
            AbstractInsnNode instruction = executable.get(index);
            if (instruction instanceof JumpInsnNode jump) {
                addTarget(leaders, labelTargets, jump.label);
                addNextLeader(leaders, index, executable.size());
            } else if (instruction instanceof TableSwitchInsnNode table) {
                addTarget(leaders, labelTargets, table.dflt);
                table.labels.forEach(label -> addTarget(leaders, labelTargets, label));
                addNextLeader(leaders, index, executable.size());
            } else if (instruction instanceof LookupSwitchInsnNode lookup) {
                addTarget(leaders, labelTargets, lookup.dflt);
                lookup.labels.forEach(label -> addTarget(leaders, labelTargets, label));
                addNextLeader(leaders, index, executable.size());
            } else if (isTerminal(instruction.getOpcode())) {
                addNextLeader(leaders, index, executable.size());
            }
        }

        if (method.tryCatchBlocks != null) {
            for (TryCatchBlockNode tryCatch : method.tryCatchBlocks) {
                addTarget(leaders, labelTargets, tryCatch.start);
                addTarget(leaders, labelTargets, tryCatch.end);
                addTarget(leaders, labelTargets, tryCatch.handler);
            }
        }

        List<Integer> orderedLeaders = leaders.stream().sorted().toList();
        int[] blockForInstruction = new int[executable.size()];
        List<MethodGraph.BasicBlock> blocks = new ArrayList<>(orderedLeaders.size());
        Set<Integer> handlers = exceptionHandlerBlocks(method, labelTargets, orderedLeaders);
        Map<LabelNode, String> labelNames = labelNames(all);
        Function<LabelNode, String> labelNamer = label ->
                labelNames.getOrDefault(label, "L?");

        for (int blockId = 0; blockId < orderedLeaders.size(); blockId++) {
            int start = orderedLeaders.get(blockId);
            int end = blockId + 1 < orderedLeaders.size()
                    ? orderedLeaders.get(blockId + 1) : executable.size();
            List<MethodGraph.InstructionLine> lines = new ArrayList<>(end - start);
            for (int index = start; index < end; index++) {
                AbstractInsnNode instruction = executable.get(index);
                blockForInstruction[index] = blockId;
                lines.add(new MethodGraph.InstructionLine(index,
                        formatInstruction(instruction, labelNamer), kind(instruction), instruction));
            }
            blocks.add(new MethodGraph.BasicBlock(blockId, lines, handlers.contains(blockId)));
        }

        Map<EdgeKey, MethodGraph.FlowEdge> edges = new LinkedHashMap<>();
        for (int blockId = 0; blockId < blocks.size(); blockId++) {
            int start = orderedLeaders.get(blockId);
            int end = blockId + 1 < orderedLeaders.size()
                    ? orderedLeaders.get(blockId + 1) : executable.size();
            AbstractInsnNode last = executable.get(end - 1);
            int nextBlock = blockId + 1 < blocks.size() ? blockId + 1 : -1;

            if (last instanceof JumpInsnNode jump) {
                int target = blockFor(labelTargets.get(jump.label), blockForInstruction);
                if (last.getOpcode() == Opcodes.GOTO) {
                    putEdge(edges, blockId, target, MethodGraph.FlowEdgeKind.JUMP, "");
                } else if (last.getOpcode() == Opcodes.JSR) {
                    putEdge(edges, blockId, target, MethodGraph.FlowEdgeKind.JUMP, "subroutine");
                    putEdge(edges, blockId, nextBlock, MethodGraph.FlowEdgeKind.FALLTHROUGH, "return");
                } else {
                    putEdge(edges, blockId, target, MethodGraph.FlowEdgeKind.TRUE_BRANCH, "true");
                    putEdge(edges, blockId, nextBlock, MethodGraph.FlowEdgeKind.FALSE_BRANCH, "false");
                }
            } else if (last instanceof TableSwitchInsnNode table) {
                addSwitchEdges(edges, blockId, table.dflt, table.labels,
                        table.min, labelTargets, blockForInstruction);
            } else if (last instanceof LookupSwitchInsnNode lookup) {
                addLookupSwitchEdges(edges, blockId, lookup, labelTargets, blockForInstruction);
            } else if (!isTerminal(last.getOpcode()) && last.getOpcode() != Opcodes.RET) {
                putEdge(edges, blockId, nextBlock, MethodGraph.FlowEdgeKind.FALLTHROUGH, "");
            }
        }

        addExceptionEdges(method, fullIndex, executable, orderedLeaders,
                labelTargets, blockForInstruction, edges);
        return new MethodGraph.ControlFlow(blocks, List.copyOf(edges.values()), executable.size());
    }

    private static Map<LabelNode, Integer> labelTargets(AbstractInsnNode[] all,
                                                         Map<AbstractInsnNode, Integer> executableIndex) {
        Map<LabelNode, Integer> targets = new IdentityHashMap<>();
        Integer next = null;
        for (int index = all.length - 1; index >= 0; index--) {
            AbstractInsnNode instruction = all[index];
            Integer current = executableIndex.get(instruction);
            if (current != null) next = current;
            if (instruction instanceof LabelNode label && next != null) targets.put(label, next);
        }
        return targets;
    }

    private static Map<LabelNode, String> labelNames(AbstractInsnNode[] all) {
        Map<LabelNode, String> names = new IdentityHashMap<>();
        int next = 0;
        for (AbstractInsnNode instruction : all) {
            if (instruction instanceof LabelNode label) names.put(label, "L" + next++);
        }
        return names;
    }

    private static Set<Integer> exceptionHandlerBlocks(MethodNode method,
                                                        Map<LabelNode, Integer> targets,
                                                        List<Integer> leaders) {
        if (method.tryCatchBlocks == null) return Set.of();
        Set<Integer> handlers = new LinkedHashSet<>();
        for (TryCatchBlockNode block : method.tryCatchBlocks) {
            Integer target = targets.get(block.handler);
            if (target != null) handlers.add(blockForLeader(target, leaders));
        }
        return handlers;
    }

    private static void addExceptionEdges(MethodNode method,
                                          Map<AbstractInsnNode, Integer> fullIndex,
                                          List<AbstractInsnNode> executable,
                                          List<Integer> leaders,
                                          Map<LabelNode, Integer> labelTargets,
                                          int[] blockForInstruction,
                                          Map<EdgeKey, MethodGraph.FlowEdge> edges) {
        if (method.tryCatchBlocks == null) return;
        for (TryCatchBlockNode tryCatch : method.tryCatchBlocks) {
            Integer startFull = fullIndex.get(tryCatch.start);
            Integer endFull = fullIndex.get(tryCatch.end);
            Integer handlerInstruction = labelTargets.get(tryCatch.handler);
            if (startFull == null || endFull == null || handlerInstruction == null) continue;
            int handlerBlock = blockForInstruction[handlerInstruction];
            String type = tryCatch.type == null ? "finally" : simpleName(tryCatch.type);
            for (int blockId = 0; blockId < leaders.size(); blockId++) {
                int blockStart = leaders.get(blockId);
                int blockEnd = blockId + 1 < leaders.size()
                        ? leaders.get(blockId + 1) : executable.size();
                boolean covered = false;
                for (int index = blockStart; index < blockEnd; index++) {
                    int sourceFull = fullIndex.get(executable.get(index));
                    if (sourceFull >= startFull && sourceFull < endFull) {
                        covered = true;
                        break;
                    }
                }
                if (covered && blockId != handlerBlock) {
                    putEdge(edges, blockId, handlerBlock,
                            MethodGraph.FlowEdgeKind.EXCEPTION, type);
                }
            }
        }
    }

    private static void addSwitchEdges(Map<EdgeKey, MethodGraph.FlowEdge> edges,
                                       int from, LabelNode defaultLabel, List<LabelNode> labels,
                                       int minimum, Map<LabelNode, Integer> targets,
                                       int[] blocks) {
        Map<Integer, List<String>> cases = new LinkedHashMap<>();
        for (int index = 0; index < labels.size(); index++) {
            int target = blockFor(targets.get(labels.get(index)), blocks);
            if (target >= 0) cases.computeIfAbsent(target, ignored -> new ArrayList<>())
                    .add(Integer.toString(minimum + index));
        }
        int defaultTarget = blockFor(targets.get(defaultLabel), blocks);
        if (defaultTarget >= 0) cases.computeIfAbsent(defaultTarget, ignored -> new ArrayList<>())
                .add("default");
        cases.forEach((target, values) -> putEdge(edges, from, target,
                MethodGraph.FlowEdgeKind.SWITCH, String.join(", ", values)));
    }

    private static void addLookupSwitchEdges(Map<EdgeKey, MethodGraph.FlowEdge> edges,
                                             int from, LookupSwitchInsnNode lookup,
                                             Map<LabelNode, Integer> targets, int[] blocks) {
        Map<Integer, List<String>> cases = new LinkedHashMap<>();
        for (int index = 0; index < lookup.labels.size(); index++) {
            int target = blockFor(targets.get(lookup.labels.get(index)), blocks);
            if (target >= 0) cases.computeIfAbsent(target, ignored -> new ArrayList<>())
                    .add(String.valueOf(lookup.keys.get(index)));
        }
        int defaultTarget = blockFor(targets.get(lookup.dflt), blocks);
        if (defaultTarget >= 0) cases.computeIfAbsent(defaultTarget, ignored -> new ArrayList<>())
                .add("default");
        cases.forEach((target, values) -> putEdge(edges, from, target,
                MethodGraph.FlowEdgeKind.SWITCH, String.join(", ", values)));
    }

    private static void putEdge(Map<EdgeKey, MethodGraph.FlowEdge> edges,
                                int from, int to, MethodGraph.FlowEdgeKind kind, String label) {
        if (from < 0 || to < 0) return;
        EdgeKey key = new EdgeKey(from, to, kind);
        MethodGraph.FlowEdge existing = edges.get(key);
        if (existing == null) {
            edges.put(key, new MethodGraph.FlowEdge(from, to, kind, label));
        } else if (!label.isBlank() && !existing.label().contains(label)) {
            edges.put(key, new MethodGraph.FlowEdge(from, to, kind,
                    existing.label().isBlank() ? label : existing.label() + ", " + label));
        }
    }

    private static int blockFor(Integer executableIndex, int[] blocks) {
        return executableIndex == null ? -1 : blocks[executableIndex];
    }

    private static int blockForLeader(int executableIndex, List<Integer> leaders) {
        int position = Collections.binarySearch(leaders, executableIndex);
        if (position >= 0) return position;
        return Math.max(0, -position - 2);
    }

    private static void addTarget(Set<Integer> leaders, Map<LabelNode, Integer> targets,
                                  LabelNode label) {
        Integer target = targets.get(label);
        if (target != null) leaders.add(target);
    }

    private static void addNextLeader(Set<Integer> leaders, int index, int size) {
        if (index + 1 < size) leaders.add(index + 1);
    }

    private static boolean isTerminal(int opcode) {
        return opcode == Opcodes.IRETURN || opcode == Opcodes.LRETURN
                || opcode == Opcodes.FRETURN || opcode == Opcodes.DRETURN
                || opcode == Opcodes.ARETURN || opcode == Opcodes.RETURN
                || opcode == Opcodes.ATHROW;
    }

    private static MethodGraph.InstructionKind kind(AbstractInsnNode instruction) {
        int opcode = instruction.getOpcode();
        if (instruction instanceof MethodInsnNode || opcode == Opcodes.INVOKEDYNAMIC) {
            return MethodGraph.InstructionKind.CALL;
        }
        if (instruction instanceof JumpInsnNode
                || instruction instanceof TableSwitchInsnNode
                || instruction instanceof LookupSwitchInsnNode) {
            return MethodGraph.InstructionKind.BRANCH;
        }
        if (isTerminal(opcode) || opcode == Opcodes.RET) {
            return MethodGraph.InstructionKind.TERMINAL;
        }
        if (opcode == Opcodes.LDC || opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH
                || opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.DCONST_1) {
            return MethodGraph.InstructionKind.DATA;
        }
        return MethodGraph.InstructionKind.NORMAL;
    }

    private static String formatInstruction(AbstractInsnNode instruction,
                                            Function<LabelNode, String> labels) {
        try {
            return AssemblerClipboardCodec.formatInstruction(instruction, labels);
        } catch (RuntimeException ignored) {
            return NameUtil.getOpcodeName(instruction.getOpcode()).toLowerCase(Locale.ROOT);
        }
    }

    private static String simpleName(String internalName) {
        int separator = internalName.lastIndexOf('/');
        return separator == -1 ? internalName : internalName.substring(separator + 1);
    }

    private record EdgeKey(int from, int to, MethodGraph.FlowEdgeKind kind) {
    }
}
