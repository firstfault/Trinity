package me.f1nal.trinity.execution.xref.where;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.navigation.NavigationAction;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerPreviewRenderer;
import org.objectweb.asm.tree.AbstractInsnNode;

public class XrefWhereMethodInsn extends XrefWhereMethod {
    private final AbstractInsnNode insnNode;
    private final boolean previewConstant;
    private final Object constantValue;

    public XrefWhereMethodInsn(MethodInput methodInput, AbstractInsnNode insnNode) {
        this(methodInput, insnNode, false, null);
    }

    public XrefWhereMethodInsn(MethodInput methodInput, AbstractInsnNode insnNode, Object constantValue) {
        this(methodInput, insnNode, true, constantValue);
    }

    private XrefWhereMethodInsn(MethodInput methodInput, AbstractInsnNode insnNode,
                                boolean previewConstant, Object constantValue) {
        super(methodInput);
        this.insnNode = insnNode;
        this.previewConstant = previewConstant;
        this.constantValue = constantValue;
    }

    public AbstractInsnNode getInsnNode() {
        return insnNode;
    }

    @Override
    protected void drawPreview(DecompilerPreviewRenderer renderer, Input<?> input,
                               boolean highlightOwnerClass) {
        if (previewConstant) {
            renderer.drawMethodConstantUsagePreview(getInput(), insnNode, constantValue);
        } else {
            renderer.drawMethodUsagePreview(getInput(), insnNode, highlightOwnerClass);
        }
    }

    @Override
    public void followInDecompiler(NavigationAction action) {
        NavigationAction navigationAction = this.previewConstant
                ? NavigationAction.FOLLOW_CONSTANT : action;
        Main.getDisplayManager().followDecompilerView(this.getInput(), this.insnNode, navigationAction,
                this.previewConstant ? formatConstant(this.constantValue) : null);
    }

    private static String formatConstant(Object value) {
        if (value == null) return "null";
        if (value instanceof String string) {
            return '"' + string.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t") + '"';
        }
        if (value instanceof Long) return value + "L";
        if (value instanceof Float) return value + "F";
        if (value instanceof Double) return value + "D";
        return String.valueOf(value);
    }
}
