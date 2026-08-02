package me.f1nal.trinity.execution.constant;

import me.f1nal.trinity.execution.asm.AsmValueWalker;
import me.f1nal.trinity.util.InstructionUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Produces one canonical stream of actual constant occurrences from an ASM class tree.
 */
public final class AsmConstantScanner {
    private AsmConstantScanner() {
    }

    public static List<Occurrence> scan(ClassNode node) {
        Builder builder = new Builder();
        Site classSite = Site.classSite();
        builder.addAnnotations(classSite, node.visibleAnnotations);
        builder.addAnnotations(classSite, node.invisibleAnnotations);
        builder.addAnnotations(classSite, node.visibleTypeAnnotations);
        builder.addAnnotations(classSite, node.invisibleTypeAnnotations);

        if (node.recordComponents != null) {
            for (RecordComponentNode component : node.recordComponents) {
                builder.addAnnotations(classSite, component.visibleAnnotations);
                builder.addAnnotations(classSite, component.invisibleAnnotations);
                builder.addAnnotations(classSite, component.visibleTypeAnnotations);
                builder.addAnnotations(classSite, component.invisibleTypeAnnotations);
            }
        }
        if (node.fields != null) {
            for (FieldNode field : node.fields) builder.addField(field);
        }
        if (node.methods != null) {
            for (MethodNode method : node.methods) builder.addMethod(method);
        }
        return List.copyOf(builder.occurrences);
    }

    public enum Kind {
        LITERAL,
        ANNOTATION
    }

    public record Site(FieldNode field, MethodNode method, AbstractInsnNode instruction) {
        static Site classSite() {
            return new Site(null, null, null);
        }

        static Site fieldSite(FieldNode field) {
            return new Site(field, null, null);
        }

        static Site methodSite(MethodNode method) {
            return new Site(null, method, null);
        }

        static Site instructionSite(MethodNode method, AbstractInsnNode instruction) {
            return new Site(null, method, instruction);
        }
    }

    public record Occurrence(Object value, Site site, Kind kind, int instructionOccurrence) {
    }

    private static final class Builder {
        private final List<Occurrence> occurrences = new ArrayList<>();

        private void addField(FieldNode field) {
            Site site = Site.fieldSite(field);
            addValue(field.value, site, Kind.LITERAL, null);
            addAnnotations(site, field.visibleAnnotations);
            addAnnotations(site, field.invisibleAnnotations);
            addAnnotations(site, field.visibleTypeAnnotations);
            addAnnotations(site, field.invisibleTypeAnnotations);
        }

        private void addMethod(MethodNode method) {
            Site methodSite = Site.methodSite(method);
            addValue(method.annotationDefault, methodSite, Kind.ANNOTATION, null);
            addAnnotations(methodSite, method.visibleAnnotations);
            addAnnotations(methodSite, method.invisibleAnnotations);
            addAnnotations(methodSite, method.visibleTypeAnnotations);
            addAnnotations(methodSite, method.invisibleTypeAnnotations);
            addParameterAnnotations(methodSite, method.visibleParameterAnnotations);
            addParameterAnnotations(methodSite, method.invisibleParameterAnnotations);
            addAnnotations(methodSite, method.visibleLocalVariableAnnotations);
            addAnnotations(methodSite, method.invisibleLocalVariableAnnotations);
            if (method.tryCatchBlocks != null) {
                for (TryCatchBlockNode block : method.tryCatchBlocks) {
                    addAnnotations(methodSite, block.visibleTypeAnnotations);
                    addAnnotations(methodSite, block.invisibleTypeAnnotations);
                }
            }

            if (method.instructions == null) return;
            for (AbstractInsnNode instruction : method.instructions) {
                Site instructionSite = Site.instructionSite(method, instruction);
                addAnnotations(instructionSite, instruction.visibleTypeAnnotations);
                addAnnotations(instructionSite, instruction.invisibleTypeAnnotations);
                addInstructionConstants(instructionSite, instruction);
            }
        }

        private void addInstructionConstants(Site site, AbstractInsnNode instruction) {
            List<Object> seen = new ArrayList<>();
            if (instruction instanceof IincInsnNode increment) {
                addValue(increment.incr, site, Kind.LITERAL, seen);
            } else if (instruction instanceof LdcInsnNode ldc) {
                addValue(ldc.cst, site, Kind.LITERAL, seen);
            } else if (instruction instanceof InsnNode) {
                int opcode = instruction.getOpcode();
                if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.DCONST_1) {
                    addValue(InstructionUtil.decodeConstLoad(opcode), site, Kind.LITERAL, seen);
                }
            } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                addValue(dynamic.bsm, site, Kind.LITERAL, seen);
                for (Object constant : InvokeDynamicConstants.resolve(dynamic)) {
                    addValue(constant, site, Kind.LITERAL, seen);
                }
            } else if (instruction instanceof IntInsnNode operand) {
                addValue(operand.operand, site, Kind.LITERAL, seen);
            } else if (instruction instanceof MultiANewArrayInsnNode array) {
                addValue(array.dims, site, Kind.LITERAL, seen);
            } else if (instruction instanceof LookupSwitchInsnNode lookup) {
                for (Integer key : lookup.keys) addValue(key, site, Kind.LITERAL, seen);
            } else if (instruction instanceof TableSwitchInsnNode table) {
                int value = table.min;
                for (int index = 0; index < table.labels.size(); index++, value++) {
                    addValue(value, site, Kind.LITERAL, seen);
                }
            }
        }

        private void addParameterAnnotations(Site site, List<AnnotationNode>[] annotations) {
            if (annotations == null) return;
            for (List<AnnotationNode> parameter : annotations) addAnnotations(site, parameter);
        }

        private void addAnnotations(Site site, List<? extends AnnotationNode> annotations) {
            if (annotations == null) return;
            for (AnnotationNode annotation : annotations) {
                if (annotation == null || annotation.values == null) continue;
                for (int index = 1; index < annotation.values.size(); index += 2) {
                    addValue(annotation.values.get(index), site, Kind.ANNOTATION, null);
                }
            }
        }

        private void addValue(Object value, Site site, Kind kind, List<Object> instructionValues) {
            AsmValueWalker.walk(value, nested -> {
                int occurrence = 0;
                if (instructionValues != null) {
                    for (Object previous : instructionValues) {
                        if (Objects.equals(previous, nested)) occurrence++;
                    }
                    instructionValues.add(nested);
                }
                occurrences.add(new Occurrence(nested, site, kind, occurrence));
            });
        }
    }
}
