package me.f1nal.trinity.gui.windows.impl.assembler;

import me.f1nal.trinity.execution.ClassInput;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.util.CheckClassAdapter;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class AssemblerValidator {
    private AssemblerValidator() {
    }

    public static AssemblerValidationResult validate(ClassInput owner, MethodNode candidate) {
        AssemblerValidationResult result = new AssemblerValidationResult();
        Set<LabelNode> labels = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean noCodeAllowed = (candidate.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0;
        if (noCodeAllowed && candidate.instructions.size() != 0) {
            result.error("Abstract and native methods cannot contain instructions");
        } else if (!noCodeAllowed && candidate.instructions.size() == 0) {
            result.error("A concrete method must contain a Code body");
        }
        if (candidate.maxStack < 0 || candidate.maxLocals < 0) result.error("Method max values cannot be negative");
        for (AbstractInsnNode instruction : candidate.instructions) {
            if (instruction instanceof LabelNode label && !labels.add(label)) {
                result.error("A label node occurs more than once in the method");
            }
        }

        int index = 0;
        for (AbstractInsnNode instruction : candidate.instructions) {
            validateInstruction(instruction, index++, labels, result);
        }
        validateCodeMetadata(candidate, labels, result);

        if (result.isValid()) {
            try {
                ClassNode classClone = new ClassNode(Opcodes.ASM9);
                owner.getNode().accept(classClone);
                for (int i = 0; i < classClone.methods.size(); i++) {
                    MethodNode method = classClone.methods.get(i);
                    if (method.name.equals(candidate.name) && method.desc.equals(candidate.desc)) {
                        classClone.methods.set(i, AssemblerDocument.cloneMethod(candidate));
                        break;
                    }
                }
                ClassWriter writer = new ClassWriter(0);
                classClone.accept(new CheckClassAdapter(writer, false));
                writer.toByteArray();
            } catch (Throwable throwable) {
                result.error("ASM could not serialize the edited method: " + message(throwable));
            }
        }

        if (result.isValid() && candidate.instructions.size() > 0
                && (candidate.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
            try {
                Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicVerifier());
                analyzer.analyze(owner.getRealName(), candidate);
            } catch (Throwable throwable) {
                result.warning("ASM data-flow analysis: " + message(throwable));
            }
        }
        return result;
    }

    private static void validateInstruction(AbstractInsnNode instruction, int index, Set<LabelNode> labels,
                                            AssemblerValidationResult result) {
        String prefix = "Instruction " + index + " (" + instruction.getClass().getSimpleName() + "): ";
        if (instruction instanceof JumpInsnNode jump) requireLabel(jump.label, labels, prefix + "jump target", result);
        if (instruction instanceof TableSwitchInsnNode table) {
            requireLabel(table.dflt, labels, prefix + "default target", result);
            if (table.max < table.min || table.labels.size() != table.max - table.min + 1) {
                result.error(prefix + "table switch range does not match its label count");
            }
            table.labels.forEach(label -> requireLabel(label, labels, prefix + "case target", result));
        }
        if (instruction instanceof LookupSwitchInsnNode lookup) {
            requireLabel(lookup.dflt, labels, prefix + "default target", result);
            if (lookup.keys.size() != lookup.labels.size()) result.error(prefix + "lookup keys and labels differ in size");
            int previous = Integer.MIN_VALUE;
            for (Integer key : lookup.keys) {
                if (key < previous) result.error(prefix + "lookup keys must be sorted in ascending order");
                previous = key;
            }
            lookup.labels.forEach(label -> requireLabel(label, labels, prefix + "case target", result));
        }
        if (instruction instanceof LineNumberNode line) {
            requireLabel(line.start, labels, prefix + "line start", result);
            if (line.line < 0) result.error(prefix + "line number cannot be negative");
        }
        if (instruction instanceof FrameNode frame) {
            validateFrameValues(frame.local, labels, prefix + "local frame", result);
            validateFrameValues(frame.stack, labels, prefix + "stack frame", result);
        }
        if (instruction instanceof VarInsnNode variable && variable.var < 0) result.error(prefix + "local index cannot be negative");
        if (instruction instanceof IincInsnNode increment && increment.var < 0) result.error(prefix + "local index cannot be negative");
        if (instruction instanceof IntInsnNode integer) {
            if (integer.getOpcode() == Opcodes.BIPUSH && (integer.operand < Byte.MIN_VALUE || integer.operand > Byte.MAX_VALUE)) {
                result.error(prefix + "BIPUSH operand is outside the signed-byte range");
            }
            if (integer.getOpcode() == Opcodes.SIPUSH && (integer.operand < Short.MIN_VALUE || integer.operand > Short.MAX_VALUE)) {
                result.error(prefix + "SIPUSH operand is outside the signed-short range");
            }
            if (integer.getOpcode() == Opcodes.NEWARRAY
                    && (integer.operand < Opcodes.T_BOOLEAN || integer.operand > Opcodes.T_LONG)) {
                result.error(prefix + "NEWARRAY has an invalid primitive type code");
            }
        }
        if (instruction instanceof FieldInsnNode field) {
            requireInternalName(field.owner, prefix + "owner", result);
            requireName(field.name, prefix + "field name", result);
            try {
                if (Type.getType(field.desc).getSort() == Type.METHOD) throw new IllegalArgumentException();
            } catch (Throwable throwable) {
                result.error(prefix + "invalid field descriptor " + field.desc);
            }
        }
        if (instruction instanceof MethodInsnNode method) {
            requireInternalName(method.owner, prefix + "owner", result);
            requireName(method.name, prefix + "method name", result);
            requireMethodDescriptor(method.desc, prefix, result);
            if (method.getOpcode() == Opcodes.INVOKEINTERFACE && !method.itf) {
                result.error(prefix + "INVOKEINTERFACE requires the interface-owner flag");
            }
        }
        if (instruction instanceof InvokeDynamicInsnNode dynamic) {
            requireName(dynamic.name, prefix + "call-site name", result);
            requireMethodDescriptor(dynamic.desc, prefix, result);
            validateHandle(dynamic.bsm, prefix + "bootstrap", result);
            for (Object argument : dynamic.bsmArgs) validateConstant(argument, prefix + "bootstrap argument", result);
        }
        if (instruction instanceof LdcInsnNode ldc) validateConstant(ldc.cst, prefix + "constant", result);
        if (instruction instanceof TypeInsnNode type) {
            if (type.desc == null || type.desc.isBlank() || type.desc.indexOf('.') >= 0) {
                result.error(prefix + "type must be a slash-separated internal name or array descriptor");
            } else if (type.desc.startsWith("[")) {
                try {
                    if (Type.getType(type.desc).getSort() != Type.ARRAY) throw new IllegalArgumentException();
                } catch (Throwable throwable) {
                    result.error(prefix + "invalid array type descriptor " + type.desc);
                }
            }
        }
        if (instruction instanceof MultiANewArrayInsnNode multi) {
            try {
                Type type = Type.getType(multi.desc);
                if (type.getSort() != Type.ARRAY || multi.dims < 1 || multi.dims > type.getDimensions()) {
                    throw new IllegalArgumentException();
                }
            } catch (Throwable throwable) {
                result.error(prefix + "invalid multidimensional-array descriptor or dimensions");
            }
        }
    }

    private static void validateCodeMetadata(MethodNode method, Set<LabelNode> labels, AssemblerValidationResult result) {
        if (method.tryCatchBlocks != null) for (TryCatchBlockNode block : method.tryCatchBlocks) {
            requireLabel(block.start, labels, "Try/catch start", result);
            requireLabel(block.end, labels, "Try/catch end", result);
            if (block.handler != null) requireLabel(block.handler, labels, "Try/catch handler", result);
        }
        if (method.localVariables != null) for (LocalVariableNode local : method.localVariables) {
            requireLabel(local.start, labels, "Local variable start", result);
            requireLabel(local.end, labels, "Local variable end", result);
        }
        validateLocalAnnotations(method.visibleLocalVariableAnnotations, labels, result);
        validateLocalAnnotations(method.invisibleLocalVariableAnnotations, labels, result);
    }

    private static void validateLocalAnnotations(List<LocalVariableAnnotationNode> annotations, Set<LabelNode> labels,
                                                 AssemblerValidationResult result) {
        if (annotations == null) return;
        for (LocalVariableAnnotationNode annotation : annotations) {
            if (annotation.start.size() != annotation.end.size() || annotation.start.size() != annotation.index.size()) {
                result.error("Local-variable annotation ranges differ in size");
                continue;
            }
            annotation.start.forEach(label -> requireLabel(label, labels, "Local annotation start", result));
            annotation.end.forEach(label -> requireLabel(label, labels, "Local annotation end", result));
        }
    }

    private static void validateFrameValues(List<Object> values, Set<LabelNode> labels, String label,
                                            AssemblerValidationResult result) {
        if (values == null) return;
        for (Object value : values) {
            if (value instanceof LabelNode node) requireLabel(node, labels, label + " uninitialized value", result);
            else if (!(value instanceof Integer) && !(value instanceof String)) result.error(label + " has an invalid value");
        }
    }

    private static void validateConstant(Object value, String label, AssemblerValidationResult result) {
        if (value instanceof Integer || value instanceof Float || value instanceof Long || value instanceof Double
                || value instanceof String || value instanceof Type) return;
        if (value instanceof Handle handle) {
            validateHandle(handle, label, result);
            return;
        }
        if (value instanceof ConstantDynamic dynamic) {
            requireName(dynamic.getName(), label + " name", result);
            try {
                Type.getType(dynamic.getDescriptor());
            } catch (Throwable throwable) {
                result.error(label + " has an invalid ConstantDynamic descriptor");
            }
            validateHandle(dynamic.getBootstrapMethod(), label + " bootstrap", result);
            for (int i = 0; i < dynamic.getBootstrapMethodArgumentCount(); i++) {
                validateConstant(dynamic.getBootstrapMethodArgument(i), label + " argument " + i, result);
            }
            return;
        }
        result.error(label + " has unsupported type " + (value == null ? "null" : value.getClass().getName()));
    }

    private static void validateHandle(Handle handle, String label, AssemblerValidationResult result) {
        if (handle == null) {
            result.error(label + " cannot be null");
            return;
        }
        if (handle.getTag() < Opcodes.H_GETFIELD || handle.getTag() > Opcodes.H_INVOKEINTERFACE) {
            result.error(label + " has an invalid handle tag");
        }
        requireInternalName(handle.getOwner(), label + " owner", result);
        requireName(handle.getName(), label + " name", result);
    }

    private static void requireLabel(LabelNode label, Set<LabelNode> labels, String name,
                                     AssemblerValidationResult result) {
        if (label == null || !labels.contains(label)) result.error(name + " does not reference a label in this method");
    }

    private static void requireInternalName(String name, String label, AssemblerValidationResult result) {
        if (name == null || name.isBlank() || name.indexOf('.') >= 0) result.error(label + " is not a JVM internal name");
    }

    private static void requireName(String name, String label, AssemblerValidationResult result) {
        if (name == null || name.isBlank()) result.error(label + " cannot be empty");
    }

    private static void requireMethodDescriptor(String descriptor, String prefix, AssemblerValidationResult result) {
        try {
            Type.getMethodType(descriptor);
        } catch (Throwable throwable) {
            result.error(prefix + "invalid method descriptor " + descriptor);
        }
    }

    private static String message(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
