package me.f1nal.trinity.execution.pattern;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerClipboardCodec;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerDocument;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerValidationResult;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerValidator;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PatternReplaceSession {

    public enum State { RUNNING, FINISHED }

    public record ReplaceResult(MethodInput method, int replacements) {}

    private final Trinity trinity;
    private final InstructionPattern searchPattern;
    private final String replacementText;
    private final List<MethodInput> methods;
    private final List<ReplaceResult> results = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private int methodIndex;
    private int totalReplacements;
    private State state = State.RUNNING;

    public PatternReplaceSession(Trinity trinity, InstructionPattern searchPattern, String replacementText) {
        this.trinity = trinity;
        this.searchPattern = searchPattern;
        this.replacementText = replacementText;
        this.methods = trinity.getExecution().getClassList().stream()
                .flatMap(c -> c.getMethodMap().values().stream())
                .filter(m -> m.getOwningClass().getDeclaredMethod(m.getName(), m.getDescriptor()) == m)
                .toList();
    }

    public void advance(long budgetNanos) {
        if (state != State.RUNNING) return;
        long deadline = System.nanoTime() + Math.max(100_000L, budgetNanos);
        do {
            MethodInput method = methods.get(methodIndex++);
            try {
                int replaced = replaceInMethod(method);
                if (replaced > 0) {
                    results.add(new ReplaceResult(method, replaced));
                    totalReplacements += replaced;
                }
            } catch (Throwable t) {
                String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
                errors.add(method.getOwningClass().getRealName() + "#"
                        + method.getName() + method.getDescriptor() + ": " + msg);
            }
        } while (methodIndex < methods.size() && System.nanoTime() < deadline);

        if (methodIndex >= methods.size()) {
            state = State.FINISHED;
        }
    }

    private int replaceInMethod(MethodInput method) {
        List<InstructionPatternMatch> matches = InstructionPatternMatcher.findAll(method, searchPattern);
        if (matches.isEmpty()) return 0;

        AssemblerDocument doc = new AssemblerDocument(method);
        MethodNode workingMethod = doc.getMethod();
        InsnList insnList = workingMethod.instructions;

        Set<AbstractInsnNode> matchedSet = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<AbstractInsnNode> matchHeads = Collections.newSetFromMap(new IdentityHashMap<>());
        for (InstructionPatternMatch match : matches) {
            for (AbstractInsnNode insn : match.instructions()) {
                matchedSet.add(insn);
            }
            if (!match.instructions().isEmpty()) {
                matchHeads.add(match.instructions().get(0));
            }
        }

        List<AbstractInsnNode> newOrder = new ArrayList<>();
        for (AbstractInsnNode insn : insnList) {
            if (!matchedSet.contains(insn)) {
                newOrder.add(insn);
            } else if (matchHeads.contains(insn)) {
                for (AbstractInsnNode rep : parseReplacement(workingMethod)) {
                    newOrder.add(rep.clone(new IdentityHashMap<>()));
                }
            }
        }

        MethodNode candidate = doc.buildCandidate(newOrder);
        AssemblerValidationResult validation = AssemblerValidator.validate(method.getOwningClass(), candidate);
        if (!validation.isValid()) {
            throw new IllegalStateException(String.join("; ", validation.getErrors()));
        }

        doc.commit(candidate);
        trinity.getExecution().getXrefMap().refreshMethod(method);
        trinity.getEventManager().postEvent(new EventMemberModified(method));
        return matches.size();
    }

    private List<AbstractInsnNode> parseReplacement(MethodNode context) {
        Map<String, LabelNode> existingLabels = new LinkedHashMap<>();
        for (AbstractInsnNode insn : context.instructions) {
            if (insn instanceof LabelNode label) {
                existingLabels.put("L" + existingLabels.size(), label);
            }
        }
        AssemblerClipboardCodec.ParsedInstructions parsed = AssemblerClipboardCodec.parse(
                replacementText,
                name -> existingLabels.getOrDefault(name, new LabelNode()));
        return parsed.instructions();
    }

    public boolean isFinished() { return state == State.FINISHED; }
    public float progress() { return methods.isEmpty() ? 1f : (float) methodIndex / methods.size(); }
    public int methodsProcessed() { return methodIndex; }
    public int methodCount() { return methods.size(); }
    public int totalReplacements() { return totalReplacements; }
    public int methodsModified() { return results.size(); }
    public List<ReplaceResult> results() { return List.copyOf(results); }
    public List<String> errors() { return List.copyOf(errors); }
}
