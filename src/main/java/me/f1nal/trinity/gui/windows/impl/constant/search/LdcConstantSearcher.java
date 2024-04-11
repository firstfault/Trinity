package me.f1nal.trinity.gui.windows.impl.constant.search;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.xref.XrefKind;
import me.f1nal.trinity.execution.xref.where.*;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewCache;
import me.f1nal.trinity.util.InstructionUtil;
import org.objectweb.asm.tree.*;

import java.util.Collections;
import java.util.List;

import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.ICONST_M1;

public abstract class LdcConstantSearcher<T> {
    public void populate(List<ConstantViewCache> list, Execution execution) {
        for (ClassInput classInput : execution.getClassList()) {
            for (MethodInput methodInput : classInput.getMethodList().values()) {
                for (AbstractInsnNode insnNode : methodInput.getInstructions()) {
                    if (insnNode instanceof IincInsnNode) {
                        this.addConstantView(list, ((IincInsnNode) insnNode).incr, new XrefWhereMethodInsn(methodInput, insnNode), XrefKind.LITERAL);
                    } else if (insnNode instanceof LdcInsnNode) {
                        this.addConstantView(list, ((LdcInsnNode) insnNode).cst, new XrefWhereMethodInsn(methodInput, insnNode), XrefKind.LITERAL);
                    } else if (insnNode instanceof InsnNode) {
                        final int opcode = insnNode.getOpcode();
                        if (opcode >= ICONST_M1 && opcode <= DCONST_1) {
                            this.addConstantView(list, InstructionUtil.decodeConstLoad(opcode), new XrefWhereMethodInsn(methodInput, insnNode), XrefKind.LITERAL);
                        }
                    } else if (insnNode instanceof InvokeDynamicInsnNode) {
                        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insnNode;
                        for (Object bsmArg : indy.bsmArgs) {
                            this.addConstantView(list, bsmArg, new XrefWhereMethodInsn(methodInput, indy), XrefKind.LITERAL);
                        }
                    } else if (insnNode instanceof IntInsnNode) {
                        this.addConstantView(list, ((IntInsnNode) insnNode).operand, new XrefWhereMethodInsn(methodInput, insnNode), XrefKind.LITERAL);
                    } else if (insnNode instanceof MultiANewArrayInsnNode) {
                        this.addConstantView(list, ((MultiANewArrayInsnNode) insnNode).dims, new XrefWhereMethodInsn(methodInput, insnNode), XrefKind.LITERAL);
                    }
                }

                this.addAnnotationConstants(list, new XrefWhereMethod(methodInput), methodInput.getNode().invisibleAnnotations);
                this.addAnnotationConstants(list, new XrefWhereMethod(methodInput), methodInput.getNode().visibleAnnotations);
            }

            for (FieldInput fieldInput : classInput.getFieldList().values()) {
                Object value = fieldInput.getNode().value;
                if (value != null) this.addConstantView(list, value, new XrefWhereField(fieldInput), XrefKind.LITERAL);
            }

            this.addAnnotationConstants(list, new XrefWhereClass(classInput), classInput.getNode().invisibleAnnotations);
            this.addAnnotationConstants(list, new XrefWhereClass(classInput), classInput.getNode().visibleAnnotations);
        }
    }

    private void addAnnotationConstants(List<ConstantViewCache> list, XrefWhere where, List<AnnotationNode> annotations) {
        if (annotations != null) for (AnnotationNode annotation : annotations) {
            List<Object> values = annotation.values;
            if (values != null) for (int i = 0, valuesSize = values.size(); i < valuesSize; i += 2) {
                Object value = values.get(i + 1);
                this.addAnnotationValues(list, annotation, where, value);
            }
        }
    }

    private void addAnnotationValues(List<ConstantViewCache> list, AnnotationNode annotationNode, XrefWhere where, Object value) {
        if (value instanceof AnnotationNode) this.addAnnotationConstants(list, where, Collections.singletonList((AnnotationNode) value));
        else if (value instanceof List<?>) ((List<?>) value).forEach(v -> addAnnotationValues(list, annotationNode, where, v));
        else if (isOfType(value)) this.addConstantView(list, value, where, XrefKind.ANNOTATION);
    }

    private void addConstantView(List<ConstantViewCache> list, Object value, XrefWhere where, XrefKind kind) {
        if (!isOfType(value)) {
            return;
        }
        //noinspection unchecked
        String constant = this.convertConstantToText((T) value);
        if (constant == null) {
            return;
        }
        list.add(new ConstantViewCache(constant, where, kind));
    }

    protected abstract boolean isOfType(Object value);
    protected abstract String convertConstantToText(T value);
}
