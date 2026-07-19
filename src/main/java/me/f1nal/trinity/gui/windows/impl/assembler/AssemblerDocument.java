package me.f1nal.trinity.gui.windows.impl.assembler;

import me.f1nal.trinity.execution.MethodInput;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Detached, lossless working copy used by the assembler.
 */
public final class AssemblerDocument {
    private final MethodInput methodInput;
    private MethodNode method;
    private String savedFingerprint;
    private String liveFingerprint;

    public AssemblerDocument(MethodInput methodInput) {
        this.methodInput = methodInput;
        reload();
    }

    public void reload() {
        this.method = cloneMethod(methodInput.getNode());
        this.savedFingerprint = fingerprint(method);
        this.liveFingerprint = fingerprint(methodInput.getNode());
    }

    public MethodNode getMethod() {
        return method;
    }

    public void replaceWorkingMethod(MethodNode method) {
        this.method = cloneMethod(method);
    }

    public InsnList getInstructions() {
        return method.instructions;
    }

    public Map<LabelNode, LabelNode> createIdentityLabelMap() {
        Map<LabelNode, LabelNode> labels = new IdentityHashMap<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LabelNode label) {
                labels.put(label, label);
            }
        }
        return labels;
    }

    public void replaceInstructionOrder(List<AbstractInsnNode> orderedInstructions) {
        InsnList instructions = method.instructions;
        for (AbstractInsnNode instruction : instructions.toArray()) {
            instructions.remove(instruction);
        }
        orderedInstructions.forEach(instructions::add);
    }

    public boolean isDirty() {
        return !savedFingerprint.equals(fingerprint(method));
    }

    public boolean hasExternalChanges() {
        return !liveFingerprint.equals(fingerprint(methodInput.getNode()));
    }

    public void acceptExternalVersionForOverwrite() {
        this.liveFingerprint = fingerprint(methodInput.getNode());
    }

    public MethodNode buildCandidate(List<AbstractInsnNode> orderedInstructions) {
        replaceInstructionOrder(orderedInstructions);
        return cloneMethod(method);
    }

    public void commit(MethodNode candidate) {
        MethodNode live = methodInput.getNode();
        List<String> labelNames = new java.util.ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LabelNode label) {
                labelNames.add(methodInput.getLabelTable().getLabel(label.getLabel()).getName());
            }
        }
        MethodNode committed = cloneMethod(candidate);

        live.instructions = committed.instructions;
        live.tryCatchBlocks = committed.tryCatchBlocks;
        live.localVariables = committed.localVariables;
        live.visibleLocalVariableAnnotations = committed.visibleLocalVariableAnnotations;
        live.invisibleLocalVariableAnnotations = committed.invisibleLocalVariableAnnotations;
        live.maxStack = committed.maxStack;
        live.maxLocals = committed.maxLocals;

        int labelIndex = 0;
        for (AbstractInsnNode instruction : live.instructions) {
            if (instruction instanceof LabelNode label && labelIndex < labelNames.size()) {
                methodInput.getLabelTable().getLabel(label.getLabel()).getNameProperty().set(labelNames.get(labelIndex++));
            }
        }

        this.savedFingerprint = fingerprint(this.method);
        this.liveFingerprint = fingerprint(live);
    }

    public static MethodNode cloneMethod(MethodNode source) {
        MethodNode clone = new MethodNode(Opcodes.ASM9, source.access, source.name, source.desc,
                source.signature, source.exceptions == null ? null : source.exceptions.toArray(String[]::new));
        source.accept(clone);
        return clone;
    }

    public static String fingerprint(MethodNode method) {
        Textifier textifier = new Textifier();
        method.accept(new TraceMethodVisitor(textifier));
        StringWriter writer = new StringWriter();
        textifier.print(new PrintWriter(writer));
        return method.access + "\n" + method.name + "\n" + method.desc + "\n"
                + method.maxStack + "\n" + method.maxLocals + "\n" + writer;
    }
}
