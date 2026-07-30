package me.f1nal.trinity.gui.windows.impl.constant.search;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.constant.InvokeDynamicConstants;
import me.f1nal.trinity.execution.xref.XrefKind;
import me.f1nal.trinity.execution.xref.where.XrefWhere;
import me.f1nal.trinity.execution.xref.where.XrefWhereClass;
import me.f1nal.trinity.execution.xref.where.XrefWhereField;
import me.f1nal.trinity.execution.xref.where.XrefWhereMethod;
import me.f1nal.trinity.execution.xref.where.XrefWhereMethodInsn;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewCache;
import me.f1nal.trinity.util.InstructionUtil;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.ICONST_M1;

/**
 * Searches literal and class-pool constants throughout loaded ASM trees.
 *
 * <p>The historical name is retained for compatibility, but coverage includes
 * LDC instructions, bootstrap methods and arguments, field values, switches,
 * annotation defaults, and every annotation-bearing class/member location.</p>
 */
public abstract class LdcConstantSearcher<T> {
    public void populate(List<ConstantViewCache> list, Execution execution) {
        for (ClassInput classInput : new ArrayList<>(execution.getClassList())) {
            populateClass(list, classInput);
        }
    }

    void populateClass(List<ConstantViewCache> list, ClassInput classInput) {
        XrefWhereClass classWhere = new XrefWhereClass(classInput);
        addAnnotations(list, classWhere, classInput.getNode().visibleAnnotations);
        addAnnotations(list, classWhere, classInput.getNode().invisibleAnnotations);
        addAnnotations(list, classWhere, classInput.getNode().visibleTypeAnnotations);
        addAnnotations(list, classWhere, classInput.getNode().invisibleTypeAnnotations);

        if (classInput.getNode().recordComponents != null) {
            for (RecordComponentNode component : classInput.getNode().recordComponents) {
                addAnnotations(list, classWhere, component.visibleAnnotations);
                addAnnotations(list, classWhere, component.invisibleAnnotations);
                addAnnotations(list, classWhere, component.visibleTypeAnnotations);
                addAnnotations(list, classWhere, component.invisibleTypeAnnotations);
            }
        }

        for (FieldInput fieldInput : classInput.getFieldMap().values()) {
            addField(list, fieldInput);
        }
        for (MethodInput methodInput : classInput.getMethodMap().values()) {
            addMethod(list, methodInput);
        }
    }

    private void addField(List<ConstantViewCache> list, FieldInput fieldInput) {
        FieldNode field = fieldInput.getNode();
        XrefWhereField where = new XrefWhereField(fieldInput);
        addConstantTree(list, field.value, where, XrefKind.LITERAL);
        addAnnotations(list, where, field.visibleAnnotations);
        addAnnotations(list, where, field.invisibleAnnotations);
        addAnnotations(list, where, field.visibleTypeAnnotations);
        addAnnotations(list, where, field.invisibleTypeAnnotations);
    }

    private void addMethod(List<ConstantViewCache> list, MethodInput methodInput) {
        MethodNode method = methodInput.getNode();
        XrefWhereMethod methodWhere = new XrefWhereMethod(methodInput);
        addConstantTree(list, method.annotationDefault, methodWhere, XrefKind.ANNOTATION);
        addAnnotations(list, methodWhere, method.visibleAnnotations);
        addAnnotations(list, methodWhere, method.invisibleAnnotations);
        addAnnotations(list, methodWhere, method.visibleTypeAnnotations);
        addAnnotations(list, methodWhere, method.invisibleTypeAnnotations);
        addParameterAnnotations(list, methodWhere, method.visibleParameterAnnotations);
        addParameterAnnotations(list, methodWhere, method.invisibleParameterAnnotations);
        addAnnotations(list, methodWhere, method.visibleLocalVariableAnnotations);
        addAnnotations(list, methodWhere, method.invisibleLocalVariableAnnotations);
        if (method.tryCatchBlocks != null) {
            for (TryCatchBlockNode block : method.tryCatchBlocks) {
                addAnnotations(list, methodWhere, block.visibleTypeAnnotations);
                addAnnotations(list, methodWhere, block.invisibleTypeAnnotations);
            }
        }

        for (AbstractInsnNode instruction : method.instructions) {
            addAnnotations(list, methodWhere, instruction.visibleTypeAnnotations);
            addAnnotations(list, methodWhere, instruction.invisibleTypeAnnotations);
            addInstructionConstants(list, methodInput, instruction);
        }
    }

    private void addInstructionConstants(List<ConstantViewCache> list,
                                         MethodInput methodInput,
                                         AbstractInsnNode instruction) {
        List<Object> seen = new ArrayList<>();
        Set<Object> recursionStack = Collections.newSetFromMap(new IdentityHashMap<>());
        if (instruction instanceof IincInsnNode increment) {
            addInstructionConstantTree(list, methodInput, instruction,
                    increment.incr, seen, recursionStack);
        } else if (instruction instanceof LdcInsnNode ldc) {
            addInstructionConstantTree(list, methodInput, instruction,
                    ldc.cst, seen, recursionStack);
        } else if (instruction instanceof InsnNode) {
            int opcode = instruction.getOpcode();
            if (opcode >= ICONST_M1 && opcode <= DCONST_1) {
                addInstructionConstantTree(list, methodInput, instruction,
                        InstructionUtil.decodeConstLoad(opcode), seen, recursionStack);
            }
        } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
            addInstructionConstantTree(list, methodInput, instruction,
                    dynamic.bsm, seen, recursionStack);
            for (Object constant : InvokeDynamicConstants.resolve(dynamic)) {
                addInstructionConstantTree(list, methodInput, instruction,
                        constant, seen, recursionStack);
            }
        } else if (instruction instanceof IntInsnNode operand) {
            addInstructionConstantTree(list, methodInput, instruction,
                    operand.operand, seen, recursionStack);
        } else if (instruction instanceof MultiANewArrayInsnNode array) {
            addInstructionConstantTree(list, methodInput, instruction,
                    array.dims, seen, recursionStack);
        } else if (instruction instanceof LookupSwitchInsnNode lookup) {
            for (Integer key : lookup.keys) {
                addInstructionConstantTree(list, methodInput, instruction,
                        key, seen, recursionStack);
            }
        } else if (instruction instanceof TableSwitchInsnNode table) {
            int value = table.min;
            for (int index = 0; index < table.labels.size(); index++, value++) {
                addInstructionConstantTree(list, methodInput, instruction,
                        value, seen, recursionStack);
            }
        }
    }

    private void addInstructionConstantTree(List<ConstantViewCache> list,
                                            MethodInput methodInput,
                                            AbstractInsnNode instruction,
                                            Object value,
                                            List<Object> seen,
                                            Set<Object> recursionStack) {
        if (value == null) return;
        int occurrence = 0;
        for (Object previous : seen) {
            if (Objects.equals(previous, value)) occurrence++;
        }
        seen.add(value);
        addConstantView(list, value,
                new XrefWhereMethodInsn(methodInput, instruction, value, occurrence),
                XrefKind.LITERAL);
        if (!isContainer(value) || !recursionStack.add(value)) return;
        try {
            forEachNestedConstant(value, nested -> addInstructionConstantTree(
                    list, methodInput, instruction, nested, seen, recursionStack));
        } finally {
            recursionStack.remove(value);
        }
    }

    private void addParameterAnnotations(List<ConstantViewCache> list,
                                         XrefWhere where,
                                         List<AnnotationNode>[] annotations) {
        if (annotations == null) return;
        for (List<AnnotationNode> parameter : annotations) {
            addAnnotations(list, where, parameter);
        }
    }

    private void addAnnotations(List<ConstantViewCache> list,
                                XrefWhere where,
                                List<? extends AnnotationNode> annotations) {
        if (annotations == null) return;
        for (AnnotationNode annotation : annotations) {
            if (annotation == null || annotation.values == null) continue;
            for (int index = 1; index < annotation.values.size(); index += 2) {
                addConstantTree(list, annotation.values.get(index), where, XrefKind.ANNOTATION);
            }
        }
    }

    private void addConstantTree(List<ConstantViewCache> list,
                                 Object value,
                                 XrefWhere where,
                                 XrefKind kind) {
        addConstantTree(list, value, where, kind,
                Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private void addConstantTree(List<ConstantViewCache> list,
                                 Object value,
                                 XrefWhere where,
                                 XrefKind kind,
                                 Set<Object> recursionStack) {
        if (value == null) return;
        addConstantView(list, value, where, kind);
        if (!isContainer(value) || !recursionStack.add(value)) return;
        try {
            forEachNestedConstant(value,
                    nested -> addConstantTree(list, nested, where, kind, recursionStack));
        } finally {
            recursionStack.remove(value);
        }
    }

    private void forEachNestedConstant(Object value,
                                       java.util.function.Consumer<Object> consumer) {
        if (value instanceof AnnotationNode annotation) {
            if (annotation.values != null) {
                for (int index = 1; index < annotation.values.size(); index += 2) {
                    consumer.accept(annotation.values.get(index));
                }
            }
        } else if (value instanceof ConstantDynamic dynamic) {
            consumer.accept(dynamic.getBootstrapMethod());
            for (int index = 0;
                 index < dynamic.getBootstrapMethodArgumentCount(); index++) {
                consumer.accept(dynamic.getBootstrapMethodArgument(index));
            }
        } else if (value instanceof List<?> values) {
            values.forEach(consumer);
        } else if (value.getClass().isArray()
                && !(value instanceof String[])) {
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                consumer.accept(Array.get(value, index));
            }
        }
    }

    private static boolean isContainer(Object value) {
        return value instanceof AnnotationNode
                || value instanceof ConstantDynamic
                || value instanceof List<?>
                || value.getClass().isArray() && !(value instanceof String[]);
    }

    private void addConstantView(List<ConstantViewCache> list,
                                 Object value,
                                 XrefWhere where,
                                 XrefKind kind) {
        if (!isOfType(value)) return;
        //noinspection unchecked
        String constant = convertConstantToText((T) value);
        if (constant != null) {
            list.add(new ConstantViewCache(constant, where, kind));
        }
    }

    protected abstract boolean isOfType(Object value);

    protected abstract String convertConstantToText(T value);
}
