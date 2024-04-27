package me.f1nal.trinity.refactor.globalrename.impl;

import me.f1nal.trinity.refactor.globalrename.GlobalRenameType;
import me.f1nal.trinity.refactor.globalrename.api.Rename;
import me.f1nal.trinity.refactor.globalrename.api.GlobalRenameContext;
import me.f1nal.trinity.util.InstructionUtil;
import me.f1nal.trinity.util.NameUtil;
import me.f1nal.trinity.execution.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnumFieldsGlobalRename extends GlobalRenameType {
    public EnumFieldsGlobalRename() {
        super("Enum Field Naming", "Recovers enum field namings.");
    }

    @Override
    public void drawInputs() {
    }

    @Override
    public void refactor(GlobalRenameContext context) {
        for (ClassInput classInput : context.execution().getClassList()) {
            if (classInput.getAccessFlags().isEnum()) {
                MethodInput clinit = classInput.getMethod("<clinit>", "()V");

                if (clinit == null) {
                    continue;
                }

                Map<MemberDetails, FieldInput> targetFields = new HashMap<>();
                AbstractInsnNode[] instructions = clinit.getInstructions().toArray();

                for (FieldInput fieldInput : classInput.getFieldMap().values()) {
                    if (fieldInput.getAccessFlags().isEnum()) {
                        targetFields.put(fieldInput.getDetails(), fieldInput);
                    }
                }

                for (AbstractInsnNode instruction : instructions) {
                    if (instruction.getOpcode() == Opcodes.NEW) {
                        try {
                            this.processNewSeq(classInput, (TypeInsnNode) instruction, targetFields, context.renames());
                        } catch (Throwable throwable) {
                            // Handling nulls for every instruction is annoying
                        }
                    }
                }
            }
        }
    }

    private void processNewSeq(ClassInput classInput, TypeInsnNode newInsn, Map<MemberDetails, FieldInput> targetFields, List<Rename> renames) {
        if (!NameUtil.internalToNormal(newInsn.desc).equals(classInput.getFullName())) {
            return;
        }
        AbstractInsnNode nxt = newInsn;
        if ((nxt = nxt.getNext()).getOpcode() != Opcodes.DUP) {
            return;
        }
        if (!((nxt = nxt.getNext()) instanceof LdcInsnNode) || !(((LdcInsnNode) nxt).cst instanceof String)) {
            return;
        }
        String cst = (String) ((LdcInsnNode) nxt).cst;
        if (!InstructionUtil.isIntegerInstruction((nxt = nxt.getNext()))) {
            return;
        }

        final int maxDepth = 8;
        for (int i = 0; i < maxDepth; i++) {
            nxt = nxt.getNext();

            if (nxt.getOpcode() == Opcodes.PUTSTATIC) {
                FieldInsnNode fin = (FieldInsnNode) nxt;

                FieldInput fieldInput = targetFields.remove(new MemberDetails(fin));
                if (fieldInput != null) {
                    renames.add(new Rename(fieldInput, cst));
                }
            }
        }
    }
}
