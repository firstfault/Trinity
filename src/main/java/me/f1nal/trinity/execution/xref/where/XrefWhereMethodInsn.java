package me.f1nal.trinity.execution.xref.where;

import me.f1nal.trinity.execution.MethodInput;
import org.objectweb.asm.tree.AbstractInsnNode;

public class XrefWhereMethodInsn extends XrefWhereMethod {
    private final AbstractInsnNode insnNode;

    public XrefWhereMethodInsn(MethodInput methodInput, AbstractInsnNode insnNode) {
        super(methodInput);
        this.insnNode = insnNode;
    }

    public AbstractInsnNode getInsnNode() {
        return insnNode;
    }
}
