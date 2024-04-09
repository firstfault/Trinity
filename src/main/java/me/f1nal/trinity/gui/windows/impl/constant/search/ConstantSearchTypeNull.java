package me.f1nal.trinity.gui.windows.impl.constant.search;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.xref.XrefKind;
import me.f1nal.trinity.execution.xref.where.XrefWhereMethodInsn;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewCache;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;

public class ConstantSearchTypeNull extends ConstantSearchType {
    public ConstantSearchTypeNull(Trinity trinity) {
        super("Constant Null", trinity);
    }

    @Override
    public boolean draw() {
        return true;
    }

    @Override
    public void populate(List<ConstantViewCache> list) {
        for (ClassInput classInput : getTrinity().getExecution().getClassList()) {
            for (MethodInput methodInput : classInput.getMethodList().values()) {
                for (AbstractInsnNode insnNode : methodInput.getInstructions()) {
                    if (insnNode.getOpcode() == Opcodes.ACONST_NULL) {
                        list.add(new ConstantViewCache("Constant Null", new XrefWhereMethodInsn(methodInput, insnNode), XrefKind.LITERAL));
                    }
                }
            }
        }
    }
}
