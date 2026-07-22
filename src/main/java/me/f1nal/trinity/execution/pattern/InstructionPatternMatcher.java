package me.f1nal.trinity.execution.pattern;

import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerClipboardCodec;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Matches compiled instruction patterns against one method at a time. */
public final class InstructionPatternMatcher {
    private InstructionPatternMatcher() {
    }

    public static List<InstructionPatternMatch> findAll(MethodInput method, InstructionPattern pattern) {
        List<Candidate> candidates = candidates(method, pattern.includeMetadata());
        List<InstructionPatternMatch> matches = new ArrayList<>();
        for (int start = 0; start < candidates.size(); start++) {
            int end = matchAt(pattern, candidates, start);
            if (end <= start) continue;
            List<Candidate> matched = candidates.subList(start, end);
            List<AbstractInsnNode> instructions = matched.stream().map(Candidate::instruction).toList();
            String formatted = String.join("\n", matched.stream().map(Candidate::formatted).toList());
            matches.add(new InstructionPatternMatch(method, instructions, formatted));
        }
        return matches;
    }

    private static int matchAt(InstructionPattern pattern, List<Candidate> candidates, int start) {
        List<State> states = List.of(new State(start, Map.of()));
        for (InstructionPattern.Element element : pattern.elements) {
            Map<StateKey, State> next = new LinkedHashMap<>();
            if (element instanceof InstructionPattern.Gap) {
                for (State state : states) {
                    for (int index = state.index(); index <= candidates.size(); index++) {
                        State candidate = new State(index, state.labels());
                        next.putIfAbsent(new StateKey(index, state.labels()), candidate);
                    }
                }
            } else {
                for (State state : states) {
                    if (state.index() >= candidates.size()) continue;
                    Map<String, String> labels = new LinkedHashMap<>(state.labels());
                    boolean matches = element instanceof InstructionPattern.AnyInstruction
                            || matches((InstructionPattern.InstructionLine) element,
                            candidates.get(state.index()), labels);
                    if (!matches) continue;
                    State candidate = new State(state.index() + 1, Map.copyOf(labels));
                    next.putIfAbsent(new StateKey(candidate.index(), candidate.labels()), candidate);
                }
            }
            if (next.isEmpty()) return -1;
            states = new ArrayList<>(next.values());
        }
        return states.stream().mapToInt(State::index).min().orElse(-1);
    }

    private static boolean matches(InstructionPattern.InstructionLine pattern,
                                   Candidate candidate, Map<String, String> labels) {
        List<String> tokens = candidate.tokens();
        if (tokens.isEmpty() || !tokens.get(0).equalsIgnoreCase(pattern.opcode())) return false;
        if (tokens.size() - 1 != pattern.operands().size()) return false;
        for (int i = 0; i < pattern.operands().size(); i++) {
            if (!pattern.operands().get(i).matches(tokens.get(i + 1), labels)) return false;
        }
        return true;
    }

    private static List<Candidate> candidates(MethodInput method, boolean includeMetadata) {
        Map<LabelNode, String> labels = new IdentityHashMap<>();
        List<Candidate> candidates = new ArrayList<>();
        for (AbstractInsnNode instruction : method.getInstructions()) {
            if (!includeMetadata && isMetadata(instruction)) continue;
            String formatted = AssemblerClipboardCodec.formatInstruction(instruction,
                    label -> labels.computeIfAbsent(label, ignored -> "L" + labels.size()));
            candidates.add(new Candidate(instruction, formatted,
                    AssemblerClipboardCodec.tokenize(formatted)));
        }
        return candidates;
    }

    private static boolean isMetadata(AbstractInsnNode instruction) {
        return instruction instanceof LabelNode || instruction instanceof FrameNode
                || instruction instanceof LineNumberNode;
    }

    private record Candidate(AbstractInsnNode instruction, String formatted, List<String> tokens) {
    }

    private record State(int index, Map<String, String> labels) {
    }

    private record StateKey(int index, Map<String, String> labels) {
    }
}
