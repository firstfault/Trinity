package me.f1nal.trinity.execution.constant;

import com.google.common.eventbus.Subscribe;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.util.InstructionUtil;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Opcodes;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ConstantStatisticsCache implements IEventListener {
    private final Execution execution;
    private volatile Map<ConstantKey, Integer> occurrences = Map.of();
    private volatile boolean dirty = true;

    public ConstantStatisticsCache(Execution execution) {
        this.execution = execution;
    }

    public int getOccurrences(Object value) {
        ConstantKey key = ConstantKey.of(value);
        if (key == null) {
            return 0;
        }
        if (dirty) {
            rebuild();
        }
        return occurrences.getOrDefault(key, 0);
    }

    private synchronized void rebuild() {
        if (!dirty) {
            return;
        }
        Map<ConstantKey, Integer> rebuilt = new HashMap<>();
        for (ClassInput classInput : new ArrayList<>(execution.getClassList())) {
            addClass(rebuilt, classInput);
        }
        occurrences = Map.copyOf(rebuilt);
        dirty = false;
    }

    private static void addClass(Map<ConstantKey, Integer> constants, ClassInput classInput) {
        addAnnotations(constants, classInput.getNode().visibleAnnotations);
        addAnnotations(constants, classInput.getNode().invisibleAnnotations);
        addAnnotations(constants, classInput.getNode().visibleTypeAnnotations);
        addAnnotations(constants, classInput.getNode().invisibleTypeAnnotations);

        if (classInput.getNode().recordComponents != null) {
            for (RecordComponentNode component : classInput.getNode().recordComponents) {
                addAnnotations(constants, component.visibleAnnotations);
                addAnnotations(constants, component.invisibleAnnotations);
                addAnnotations(constants, component.visibleTypeAnnotations);
                addAnnotations(constants, component.invisibleTypeAnnotations);
            }
        }
        for (FieldNode field : classInput.getNode().fields) {
            addConstant(constants, field.value);
            addAnnotations(constants, field.visibleAnnotations);
            addAnnotations(constants, field.invisibleAnnotations);
            addAnnotations(constants, field.visibleTypeAnnotations);
            addAnnotations(constants, field.invisibleTypeAnnotations);
        }
        for (MethodNode method : classInput.getNode().methods) {
            addMethod(constants, method);
        }
    }

    private static void addMethod(Map<ConstantKey, Integer> constants, MethodNode method) {
        addConstant(constants, method.annotationDefault);
        addAnnotations(constants, method.visibleAnnotations);
        addAnnotations(constants, method.invisibleAnnotations);
        addAnnotations(constants, method.visibleTypeAnnotations);
        addAnnotations(constants, method.invisibleTypeAnnotations);
        addParameterAnnotations(constants, method.visibleParameterAnnotations);
        addParameterAnnotations(constants, method.invisibleParameterAnnotations);
        addAnnotations(constants, method.visibleLocalVariableAnnotations);
        addAnnotations(constants, method.invisibleLocalVariableAnnotations);
        if (method.tryCatchBlocks != null) {
            for (TryCatchBlockNode block : method.tryCatchBlocks) {
                addAnnotations(constants, block.visibleTypeAnnotations);
                addAnnotations(constants, block.invisibleTypeAnnotations);
            }
        }

        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof IincInsnNode increment) {
                addConstant(constants, increment.incr);
            } else if (instruction instanceof LdcInsnNode ldc) {
                addConstant(constants, ldc.cst);
            } else if (instruction instanceof InsnNode) {
                int opcode = instruction.getOpcode();
                if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.DCONST_1) {
                    addConstant(constants, InstructionUtil.decodeConstLoad(opcode));
                }
            } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                for (Object argument : dynamic.bsmArgs) {
                    addConstant(constants, argument);
                }
            } else if (instruction instanceof IntInsnNode operand) {
                addConstant(constants, operand.operand);
            } else if (instruction instanceof MultiANewArrayInsnNode array) {
                addConstant(constants, array.dims);
            } else if (instruction instanceof LookupSwitchInsnNode lookup) {
                lookup.keys.forEach(key -> addConstant(constants, key));
            } else if (instruction instanceof TableSwitchInsnNode table) {
                int value = table.min;
                for (int i = 0; i < table.labels.size(); i++, value++) {
                    addConstant(constants, value);
                }
            }
        }
    }

    private static void addParameterAnnotations(Map<ConstantKey, Integer> constants,
                                                List<AnnotationNode>[] annotations) {
        if (annotations == null) {
            return;
        }
        for (List<AnnotationNode> parameter : annotations) {
            addAnnotations(constants, parameter);
        }
    }

    private static void addAnnotations(Map<ConstantKey, Integer> constants,
                                       List<? extends AnnotationNode> annotations) {
        if (annotations == null) {
            return;
        }
        for (AnnotationNode annotation : annotations) {
            if (annotation != null && annotation.values != null) {
                for (int i = 1; i < annotation.values.size(); i += 2) {
                    addConstant(constants, annotation.values.get(i));
                }
            }
        }
    }

    private static void addConstant(Map<ConstantKey, Integer> constants, Object value) {
        if (value == null) {
            return;
        }
        ConstantKey key = ConstantKey.of(value);
        if (key != null) {
            constants.merge(key, 1, Integer::sum);
        } else if (value instanceof AnnotationNode annotation) {
            addAnnotations(constants, List.of(annotation));
        } else if (value instanceof ConstantDynamic dynamic) {
            for (int i = 0; i < dynamic.getBootstrapMethodArgumentCount(); i++) {
                addConstant(constants, dynamic.getBootstrapMethodArgument(i));
            }
        } else if (value instanceof List<?> list) {
            list.forEach(item -> addConstant(constants, item));
        } else if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                addConstant(constants, Array.get(value, i));
            }
        }
    }

    private void invalidate() {
        dirty = true;
    }

    @Subscribe
    public void onClassesLoaded(EventClassesLoaded event) {
        invalidate();
    }

    @Subscribe
    public void onClassModified(EventClassModified event) {
        invalidate();
    }

    @Subscribe
    public void onMemberModified(EventMemberModified event) {
        invalidate();
    }

    private enum ConstantKind {
        STRING,
        INTEGER,
        LONG,
        FLOAT,
        DOUBLE
    }

    private record ConstantKey(ConstantKind kind, Object value) {
        private static ConstantKey of(Object value) {
            if (value instanceof String string) {
                return new ConstantKey(ConstantKind.STRING, string);
            }
            if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
                return new ConstantKey(ConstantKind.INTEGER, ((Number) value).intValue());
            }
            if (value instanceof Character character) {
                return new ConstantKey(ConstantKind.INTEGER, (int) character);
            }
            if (value instanceof Long number) {
                return new ConstantKey(ConstantKind.LONG, number);
            }
            if (value instanceof Float number) {
                return new ConstantKey(ConstantKind.FLOAT, number);
            }
            if (value instanceof Double number) {
                return new ConstantKey(ConstantKind.DOUBLE, number);
            }
            return null;
        }
    }
}
