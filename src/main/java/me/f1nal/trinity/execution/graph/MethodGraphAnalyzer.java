package me.f1nal.trinity.execution.graph;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.asm.AsmValueWalker;
import me.f1nal.trinity.execution.graph.MethodGraph.BlockLayout;
import me.f1nal.trinity.execution.graph.MethodGraph.CallEdge;
import me.f1nal.trinity.execution.graph.MethodGraph.ControlFlow;
import me.f1nal.trinity.execution.graph.MethodGraph.Direction;
import me.f1nal.trinity.execution.graph.MethodGraph.MethodKey;
import me.f1nal.trinity.execution.graph.MethodGraph.MethodNode;
import me.f1nal.trinity.execution.hierarchy.MemberResolver;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** Builds and lays out the inter-method graph without touching ImGui. */
public final class MethodGraphAnalyzer {
    public static final int INFINITE_DEPTH = -1;
    private static final int MAX_VISIBLE_BLOCK_INSTRUCTIONS = 8;
    private static final int MAX_INSTRUCTION_CHARACTERS = 72;
    private static final float METHOD_HEADER_HEIGHT = 48.F;
    private static final float METHOD_PADDING = 12.F;
    private static final float BLOCK_HEADER_HEIGHT = 21.F;
    private static final float BLOCK_PADDING_X = 8.F;
    private static final float BLOCK_PADDING_Y = 5.F;
    private static final float BLOCK_GAP_X = 36.F;
    private static final float BLOCK_GAP_Y = 42.F;
    private static final float METHOD_GAP_X = 180.F;
    private static final float METHOD_GAP_Y = 110.F;

    private final Execution execution;
    private final Map<MethodKey, List<Link>> outgoingCache = new HashMap<>();
    private Map<MethodKey, List<Link>> reverseCalls;

    public MethodGraphAnalyzer(Execution execution) {
        this.execution = Objects.requireNonNull(execution, "execution");
    }

    public MethodGraph analyze(MethodInput root, Request request, BooleanSupplier cancelled) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(cancelled, "cancelled");
        checkCancelled(cancelled);

        MethodKey rootKey = key(root);
        Map<MethodKey, Discovered> discovered = new LinkedHashMap<>();
        ArrayDeque<MethodKey> queue = new ArrayDeque<>();
        discovered.put(rootKey, new Discovered(root, 0, 0));
        queue.add(rootKey);

        while (!queue.isEmpty()) {
            checkCancelled(cancelled);
            MethodKey currentKey = queue.removeFirst();
            Discovered current = discovered.get(currentKey);
            if (request.depth() != INFINITE_DEPTH && current.depth() >= request.depth()) continue;

            if (request.direction() != Direction.CALLERS && current.method() != null) {
                for (Link link : outgoing(current.method(), request.includeExternal())) {
                    discover(discovered, queue, link.target(), current.depth() + 1,
                            current.rank() + 1);
                }
            }
            if (request.direction() != Direction.CALLS) {
                for (Link link : reverseCalls(cancelled).getOrDefault(currentKey, List.of())) {
                    discover(discovered, queue, link.source(), current.depth() + 1,
                            current.rank() - 1);
                }
            }
        }

        Map<CallKey, MutableCallEdge> callEdges = new LinkedHashMap<>();
        for (Map.Entry<MethodKey, Discovered> entry : discovered.entrySet()) {
            checkCancelled(cancelled);
            MethodInput caller = entry.getValue().method();
            if (caller == null) continue;
            for (Link link : outgoing(caller, request.includeExternal())) {
                if (!discovered.containsKey(link.target().key())) continue;
                CallKey edgeKey = new CallKey(entry.getKey(), link.target().key());
                callEdges.computeIfAbsent(edgeKey, ignored -> new MutableCallEdge())
                        .add(link.callSites(), link.dynamic());
            }
        }

        Map<MethodKey, NodeDraft> drafts = new LinkedHashMap<>();
        int blockCount = 0;
        int flowEdgeCount = 0;
        for (Map.Entry<MethodKey, Discovered> entry : discovered.entrySet()) {
            checkCancelled(cancelled);
            Discovered value = entry.getValue();
            ControlFlow flow = value.method() == null ? null
                    : MethodControlFlowAnalyzer.analyze(value.method().getNode());
            NodeDraft draft = layoutMethod(entry.getKey(), value, flow, request);
            drafts.put(entry.getKey(), draft);
            if (flow != null) {
                blockCount += flow.blocks().size();
                flowEdgeCount += flow.edges().size();
            }
        }

        Map<MethodKey, MethodNode> nodes = layoutCallGraph(
                rootKey, drafts, request.direction());
        List<CallEdge> calls = callEdges.entrySet().stream()
                .map(entry -> new CallEdge(entry.getKey().caller(), entry.getKey().callee(),
                        entry.getValue().count, entry.getValue().dynamic))
                .sorted(Comparator.comparing((CallEdge edge) -> edge.caller().symbol())
                        .thenComparing(edge -> edge.callee().symbol()))
                .toList();
        return new MethodGraph(rootKey, nodes, calls, bounds(nodes), blockCount, flowEdgeCount);
    }

    private void discover(Map<MethodKey, Discovered> discovered, ArrayDeque<MethodKey> queue,
                          Target target, int depth, int rank) {
        Discovered previous = discovered.get(target.key());
        if (previous != null && previous.depth() <= depth) return;
        discovered.put(target.key(), new Discovered(target.method(), depth, rank));
        queue.addLast(target.key());
    }

    private List<Link> outgoing(MethodInput method, boolean includeExternal) {
        List<Link> all = outgoingCache.computeIfAbsent(key(method), ignored -> scanOutgoing(method));
        return includeExternal ? all : all.stream().filter(link -> link.target().method() != null).toList();
    }

    private List<Link> scanOutgoing(MethodInput method) {
        Map<TargetKey, MutableLink> links = new LinkedHashMap<>();
        for (AbstractInsnNode instruction : method.getInstructions()) {
            if (instruction instanceof MethodInsnNode invocation) {
                addInvocationLinks(method, invocation, false, links);
            } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                for (Object argument : dynamic.bsmArgs) {
                    AsmValueWalker.walk(argument, value -> {
                        if (value instanceof Handle handle) addHandleLink(method, handle, links);
                    });
                }
            }
        }
        return links.values().stream().map(MutableLink::freeze).toList();
    }

    private void addHandleLink(MethodInput caller, Handle handle,
                               Map<TargetKey, MutableLink> links) {
        int opcode = handleOpcode(handle.getTag());
        if (opcode == -1 || !handle.getDesc().startsWith("(")) return;
        MethodInsnNode invocation = new MethodInsnNode(opcode, handle.getOwner(),
                handle.getName(), handle.getDesc(), handle.isInterface());
        addInvocationLinks(caller, invocation, true, links);
    }

    private void addInvocationLinks(MethodInput caller, MethodInsnNode invocation, boolean dynamic,
                                    Map<TargetKey, MutableLink> links) {
        Collection<MethodInput> resolved = MemberResolver.resolveInvocationTargets(
                execution, caller.getOwningClass(), invocation);
        if (resolved.isEmpty()) {
            MethodKey key = new MethodKey(invocation.owner, invocation.name, invocation.desc);
            mergeLink(links, new Target(key, null), dynamic);
            return;
        }
        for (MethodInput target : resolved) {
            mergeLink(links, new Target(key(target), target), dynamic
                    || resolved.size() > 1);
        }
    }

    private static void mergeLink(Map<TargetKey, MutableLink> links,
                                  Target target, boolean dynamic) {
        TargetKey targetKey = new TargetKey(target.key());
        links.computeIfAbsent(targetKey, ignored -> new MutableLink(target))
                .add(dynamic);
    }

    private Map<MethodKey, List<Link>> reverseCalls(BooleanSupplier cancelled) {
        if (reverseCalls != null) return reverseCalls;
        Map<MethodKey, List<Link>> reverse = new HashMap<>();
        List<ClassInput> classes = List.copyOf(execution.getClassList());
        for (ClassInput classInput : classes) {
            checkCancelled(cancelled);
            for (MethodInput caller : List.copyOf(classInput.getMethodMap().values())) {
                Target callerTarget = new Target(key(caller), caller);
                for (Link outgoing : outgoing(caller, true)) {
                    reverse.computeIfAbsent(outgoing.target().key(), ignored -> new ArrayList<>())
                            .add(new Link(callerTarget, outgoing.target(),
                                    outgoing.callSites(), outgoing.dynamic()));
                }
            }
        }
        reverse.replaceAll((ignored, links) -> List.copyOf(links));
        this.reverseCalls = Map.copyOf(reverse);
        return this.reverseCalls;
    }

    private NodeDraft layoutMethod(MethodKey key, Discovered discovered,
                                   ControlFlow flow, Request request) {
        if (flow == null) {
            float width = Math.max(230.F, Math.min(440.F,
                    (key.displayOwner().length() + key.name().length() + 6) * request.characterWidth()));
            return new NodeDraft(key, discovered, null, width, 62.F, List.of());
        }

        Map<Integer, Integer> ranks = blockRanks(flow);
        Map<Integer, List<BlockDraft>> layers = new LinkedHashMap<>();
        for (MethodGraph.BasicBlock block : flow.blocks()) {
            int visible = Math.min(MAX_VISIBLE_BLOCK_INSTRUCTIONS, block.instructions().size());
            int hidden = Math.max(0, block.instructions().size() - visible);
            int longest = Math.max(8, ("B" + block.id()).length());
            for (int index = 0; index < visible; index++) {
                longest = Math.max(longest, Math.min(MAX_INSTRUCTION_CHARACTERS,
                        block.instructions().get(index).text().length()));
            }
            if (hidden > 0) longest = Math.max(longest, ("... " + hidden + " more").length());
            float width = Math.max(150.F, longest * request.characterWidth()
                    + BLOCK_PADDING_X * 2.F);
            int bodyLines = Math.max(1, visible + (hidden > 0 ? 1 : 0));
            float height = BLOCK_HEADER_HEIGHT + BLOCK_PADDING_Y * 2.F
                    + bodyLines * request.lineHeight();
            BlockDraft draft = new BlockDraft(block.id(), width, height, visible, hidden);
            layers.computeIfAbsent(ranks.getOrDefault(block.id(), 0), ignored -> new ArrayList<>())
                    .add(draft);
        }

        int headerCharacters = Math.min(MAX_INSTRUCTION_CHARACTERS,
                key.displayOwner().length() + key.name().length()
                        + Math.min(42, key.descriptor().length()) + 4);
        float contentWidth = Math.max(190.F,
                headerCharacters * request.characterWidth());
        for (List<BlockDraft> layer : layers.values()) {
            float layerWidth = (float) layer.stream().mapToDouble(BlockDraft::width).sum()
                    + BLOCK_GAP_X * Math.max(0, layer.size() - 1);
            contentWidth = Math.max(contentWidth, layerWidth);
        }
        float y = METHOD_HEADER_HEIGHT + METHOD_PADDING;
        List<BlockLayout> layouts = new ArrayList<>();
        for (List<BlockDraft> layer : layers.values()) {
            float layerWidth = (float) layer.stream().mapToDouble(BlockDraft::width).sum()
                    + BLOCK_GAP_X * Math.max(0, layer.size() - 1);
            float layerHeight = (float) layer.stream().mapToDouble(BlockDraft::height).max().orElse(0.D);
            float x = METHOD_PADDING + (contentWidth - layerWidth) * 0.5F;
            for (BlockDraft block : layer) {
                layouts.add(new BlockLayout(block.id(), x, y, block.width(), block.height(),
                        block.visible(), block.hidden()));
                x += block.width() + BLOCK_GAP_X;
            }
            y += layerHeight + BLOCK_GAP_Y;
        }
        float width = contentWidth + METHOD_PADDING * 2.F;
        float height = Math.max(90.F, y - BLOCK_GAP_Y + METHOD_PADDING);
        return new NodeDraft(key, discovered, flow, width, height, layouts);
    }

    private static Map<Integer, Integer> blockRanks(ControlFlow flow) {
        Map<Integer, List<Integer>> outgoing = new HashMap<>();
        for (MethodGraph.FlowEdge edge : flow.edges()) {
            if (edge.kind() == MethodGraph.FlowEdgeKind.EXCEPTION) continue;
            outgoing.computeIfAbsent(edge.fromBlock(), ignored -> new ArrayList<>())
                    .add(edge.toBlock());
        }
        Map<Integer, Integer> ranks = new HashMap<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        if (!flow.blocks().isEmpty()) {
            ranks.put(flow.blocks().get(0).id(), 0);
            queue.add(0);
        }
        while (!queue.isEmpty()) {
            int block = queue.removeFirst();
            int nextRank = ranks.get(block) + 1;
            for (int target : outgoing.getOrDefault(block, List.of())) {
                if (ranks.putIfAbsent(target, nextRank) == null) queue.addLast(target);
            }
        }
        int unconnectedRank = ranks.values().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;
        for (MethodGraph.BasicBlock block : flow.blocks()) {
            if (!ranks.containsKey(block.id())) ranks.put(block.id(), unconnectedRank++);
        }
        return ranks;
    }

    private static Map<MethodKey, MethodNode> layoutCallGraph(MethodKey root,
                                                              Map<MethodKey, NodeDraft> drafts,
                                                              Direction direction) {
        Map<Integer, List<NodeDraft>> layers = new LinkedHashMap<>();
        drafts.values().stream()
                .sorted(Comparator.comparingInt((NodeDraft draft) -> draft.discovered().rank())
                        .thenComparing(draft -> draft.key().symbol()))
                .forEach(draft -> layers.computeIfAbsent(draft.discovered().rank(),
                        ignored -> new ArrayList<>()).add(draft));

        Map<Integer, Float> layerWidths = new HashMap<>();
        layers.forEach((rank, layer) -> layerWidths.put(rank,
                (float) layer.stream().mapToDouble(NodeDraft::width).max().orElse(0.D)));
        Map<Integer, Float> layerX = new HashMap<>();
        layerX.put(0, 0.F);
        int maximumRank = layers.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        int minimumRank = layers.keySet().stream().mapToInt(Integer::intValue).min().orElse(0);
        for (int rank = 1; rank <= maximumRank; rank++) {
            float previous = layerX.getOrDefault(rank - 1, 0.F);
            layerX.put(rank, previous + layerWidths.getOrDefault(rank - 1, 0.F) + METHOD_GAP_X);
        }
        for (int rank = -1; rank >= minimumRank; rank--) {
            float next = layerX.getOrDefault(rank + 1, 0.F);
            layerX.put(rank, next - layerWidths.getOrDefault(rank, 0.F) - METHOD_GAP_X);
        }

        Map<MethodKey, MethodNode> nodes = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<NodeDraft>> entry : layers.entrySet()) {
            List<NodeDraft> layer = entry.getValue();
            float totalHeight = (float) layer.stream().mapToDouble(NodeDraft::height).sum()
                    + METHOD_GAP_Y * Math.max(0, layer.size() - 1);
            float y = -totalHeight * 0.5F;
            for (NodeDraft draft : layer) {
                float x = layerX.getOrDefault(entry.getKey(), 0.F);
                MethodNode node = new MethodNode(draft.key(), draft.discovered().method(),
                        draft.discovered().depth(), draft.discovered().rank(),
                        draft.key().equals(root), draft.flow(), x, y,
                        draft.width(), draft.height(), draft.blocks());
                nodes.put(draft.key(), node);
                y += draft.height() + METHOD_GAP_Y;
            }
        }

        MethodNode rootNode = nodes.get(root);
        if (rootNode != null) {
            float offsetY = -rootNode.y();
            Map<MethodKey, MethodNode> shifted = new LinkedHashMap<>();
            nodes.forEach((key, node) -> shifted.put(key, new MethodNode(node.key(), node.method(),
                    node.depth(), node.rank(), node.root(), node.flow(), node.x(), node.y() + offsetY,
                    node.width(), node.height(), node.blocks())));
            return Map.copyOf(shifted);
        }
        return Map.copyOf(nodes);
    }

    private static MethodGraph.Bounds bounds(Map<MethodKey, MethodNode> nodes) {
        if (nodes.isEmpty()) return new MethodGraph.Bounds(0.F, 0.F, 1.F, 1.F);
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (MethodNode node : nodes.values()) {
            minX = Math.min(minX, node.x());
            minY = Math.min(minY, node.y());
            maxX = Math.max(maxX, node.x() + node.width());
            maxY = Math.max(maxY, node.y() + node.height());
        }
        return new MethodGraph.Bounds(minX, minY, maxX, maxY);
    }

    private static int handleOpcode(int tag) {
        return switch (tag) {
            case Opcodes.H_INVOKEVIRTUAL -> Opcodes.INVOKEVIRTUAL;
            case Opcodes.H_INVOKESTATIC -> Opcodes.INVOKESTATIC;
            case Opcodes.H_INVOKESPECIAL, Opcodes.H_NEWINVOKESPECIAL -> Opcodes.INVOKESPECIAL;
            case Opcodes.H_INVOKEINTERFACE -> Opcodes.INVOKEINTERFACE;
            default -> -1;
        };
    }

    public static MethodKey key(MethodInput method) {
        MemberDetails details = method.getDetails();
        return new MethodKey(details.getOwner(), details.getName(), details.getDesc());
    }

    private static void checkCancelled(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Method graph analysis was superseded");
        }
    }

    public record Request(int depth, Direction direction, boolean includeExternal,
                          float lineHeight, float characterWidth) {
        public Request {
            if (depth < INFINITE_DEPTH || depth == 0) {
                throw new IllegalArgumentException("Depth must be positive or infinite");
            }
            Objects.requireNonNull(direction, "direction");
            lineHeight = Math.max(10.F, lineHeight);
            characterWidth = Math.max(4.F, characterWidth);
        }
    }

    private record Target(MethodKey key, MethodInput method) {
    }

    private record TargetKey(MethodKey key) {
    }

    private record Link(Target source, Target target, int callSites, boolean dynamic) {
    }

    private static final class MutableLink {
        private final Target target;
        private int count;
        private boolean dynamic;

        private MutableLink(Target target) {
            this.target = target;
        }

        private void add(boolean dynamic) {
            count++;
            this.dynamic |= dynamic;
        }

        private Link freeze() {
            return new Link(null, target, count, dynamic);
        }
    }

    private record Discovered(MethodInput method, int depth, int rank) {
    }

    private record CallKey(MethodKey caller, MethodKey callee) {
    }

    private static final class MutableCallEdge {
        private int count;
        private boolean dynamic;

        private void add(int count, boolean dynamic) {
            this.count += count;
            this.dynamic |= dynamic;
        }
    }

    private record BlockDraft(int id, float width, float height, int visible, int hidden) {
    }

    private record NodeDraft(MethodKey key, Discovered discovered, ControlFlow flow,
                             float width, float height, List<BlockLayout> blocks) {
    }
}
