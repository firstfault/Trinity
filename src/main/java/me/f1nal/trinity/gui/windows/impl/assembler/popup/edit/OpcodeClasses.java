package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class OpcodeClasses {
    private static final Map<Class<? extends AbstractInsnNode>, String[]> classToOpcodeNames = new HashMap<>();
    private static final Map<Integer, Class<? extends AbstractInsnNode>> opcodesToClass = new HashMap<>();
    private static final Map<String, Class<? extends AbstractInsnNode>> namesToClasses = new HashMap<>();
    private static Field opcodeField;

    public static int getOpcodeIndex(String name) {
        for (int i = 0; i < Printer.OPCODES.length; i++) {
            String opcode = Printer.OPCODES[i];
            if (opcode.equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    public static Map<String, Class<? extends AbstractInsnNode>> getNamesToClasses() {
        return namesToClasses;
    }

    public static Class<? extends AbstractInsnNode> getOpcodeClass(int opcode) {
        return Objects.requireNonNull(opcodesToClass.get(opcode));
    }

    public static Class<?> getOpcodeClass(String opcode) {
        return namesToClasses.get(opcode);
    }

    public static void setInstructionOpcode(AbstractInsnNode insnNode, int opcode) {
        try {
            opcodeField.setInt(insnNode, opcode);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static {
        classToOpcodeNames.put(InsnNode.class,
                new String[] { "nop", "aconst_null", "iconst_m1", "iconst_0", "iconst_1", "iconst_2", "iconst_3", "iconst_4", "iconst_5", "lconst_0",
                        "lconst_1", "fconst_0", "fconst_1", "fconst_2", "dconst_0", "dconst_1", "iaload", "laload", "faload", "daload", "aaload", "baload",
                        "caload", "saload", "iastore", "lastore", "fastore", "dastore", "aastore", "bastore", "castore", "sastore", "pop", "pop2", "dup",
                        "dup_x1", "dup_x2", "dup2", "dup2_x1", "dup2_x2", "swap", "iadd", "ladd", "fadd", "dadd", "isub", "lsub", "fsub", "dsub", "imul", "lmul",
                        "fmul", "dmul", "idiv", "ldiv", "fdiv", "ddiv", "irem", "lrem", "frem", "drem", "ineg", "lneg", "fneg", "dneg", "ishl", "lshl", "ishr",
                        "lshr", "iushr", "lushr", "iand", "land", "ior", "lor", "ixor", "lxor", "i2l", "i2f", "i2d", "l2i", "l2f", "l2d", "f2i", "f2l", "f2d",
                        "d2i", "d2l", "d2f", "i2b", "i2c", "i2s", "lcmp", "fcmpl", "fcmpg", "dcmpl", "dcmpg", "ireturn", "lreturn", "freturn", "dreturn",
                        "areturn", "return", "arraylength", "athrow", "monitorenter", "monitorexit" });
        classToOpcodeNames.put(MethodInsnNode.class, new String[] { "invokestatic", "invokevirtual", "invokespecial", "invokeinterface" });
        classToOpcodeNames.put(FieldInsnNode.class, new String[] { "getstatic", "putstatic", "getfield", "putfield" });
        classToOpcodeNames.put(VarInsnNode.class,
                new String[] { "iload", "lload", "fload", "dload", "aload", "istore", "lstore", "fstore", "dstore", "astore", "ret" });
        classToOpcodeNames.put(TypeInsnNode.class, new String[] { "new", "anewarray", "checkcast", "instanceof" });
        classToOpcodeNames.put(MultiANewArrayInsnNode.class, new String[] { "multianewarray" });
        classToOpcodeNames.put(LdcInsnNode.class, new String[] { "ldc" });
        classToOpcodeNames.put(IincInsnNode.class, new String[] { "iinc" });
        classToOpcodeNames.put(JumpInsnNode.class, new String[] { "ifeq", "ifne", "iflt", "ifge", "ifgt", "ifle", "if_icmpeq", "if_icmpne", "if_icmplt",
                "if_icmpge", "if_icmpgt", "if_icmple", "if_acmpeq", "if_acmpne", "goto", "jsr", "ifnull", "ifnonnull" });
        classToOpcodeNames.put(IntInsnNode.class, new String[] { "bipush", "sipush", "newarray" });
        classToOpcodeNames.put(InvokeDynamicInsnNode.class, new String[] { "invokedynamic" });
        classToOpcodeNames.put(TableSwitchInsnNode.class, new String[] { "tableswitch" });
        classToOpcodeNames.put(LookupSwitchInsnNode.class, new String[] { "lookupswitch" });
        classToOpcodeNames.put(LabelNode.class, new String[] {"label"});
        classToOpcodeNames.put(LineNumberNode.class, new String[] {"line"});
        classToOpcodeNames.put(FrameNode.class, new String[] {"frame"});
        for (Map.Entry<Class<? extends AbstractInsnNode>, String[]> entry : classToOpcodeNames.entrySet()) {
            for (String opcodeName : entry.getValue()) {
                namesToClasses.put(opcodeName, entry.getKey());
                int opcodeIndex = getOpcodeIndex(opcodeName);
                if (opcodeIndex == -1 && !(entry.getKey() == LabelNode.class || entry.getKey() == LineNumberNode.class || entry.getKey() == FrameNode.class)) {
                    throw new RuntimeException("no opcode index " + opcodeName);
                }
                opcodesToClass.put(opcodeIndex, entry.getKey());
            }
        }
        Field opcodeField;
        try {
            opcodeField = AbstractInsnNode.class.getDeclaredField("opcode");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        opcodeField.setAccessible(true);
        OpcodeClasses.opcodeField = opcodeField;
    }
}
