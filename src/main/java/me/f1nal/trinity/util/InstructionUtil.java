package me.f1nal.trinity.util;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public final class InstructionUtil {
    /**
     * Checks if this instruction list may modify program flow.
     * <p>
     *     This method checks the instruction types. If it contains more than just "return", variable instructions, arithmetics, and so on, then it's marked as modifying the program flow.
     * </p>
     * @param insnList Instruction list to check.
     * @return If this instruction list may modify program flow. {@code false} means it will, with 100% certainty, not change the state of the program after being run.
     */
    public static boolean mayModifyProgramFlow(InsnList insnList) {
        for (AbstractInsnNode insnNode : insnList) {
            final int opcode = insnNode.getOpcode();
            if (insnNode instanceof InsnNode) {
                if (opcode == Opcodes.ATHROW || opcode == Opcodes.MONITORENTER || opcode == Opcodes.MONITOREXIT) {
                    return true;
                }
            } else if (insnNode instanceof TypeInsnNode) {
                if (opcode == Opcodes.NEW) {
                    return true;
                }
            } else if (insnNode instanceof InvokeDynamicInsnNode) {
                return true;
            } else if (insnNode instanceof FieldInsnNode) {
                return true;
            } else if (insnNode instanceof MethodInsnNode) {
                return true;
            }
        }

        return false;
    }

    public static boolean isStoreInstruction(int opcode) {
        return opcode >= Opcodes.ISTORE && opcode <= Opcodes.ASTORE;
    }

    public static boolean isLoadInstruction(int opcode) {
        return opcode >= Opcodes.ILOAD && opcode <= Opcodes.ALOAD;
    }

    public static AbstractInsnNode generateIntegerPush(int value) {
        if (value <= 5 && value >= -1) {
            return new InsnNode(value + 3);
        }
        if (value >= -128 && value <= 127) {
            return new IntInsnNode(BIPUSH, value);
        }
        if (value >= -32768 && value <= 32767) {
            return new IntInsnNode(SIPUSH, value);
        }
        return new LdcInsnNode(value);
    }

    public static @Nullable Integer translateIntegerPush(AbstractInsnNode instruction) {
        if (instruction instanceof InsnNode) {
            int opcode = instruction.getOpcode();
            if (opcode >= ICONST_M1 && opcode <= ICONST_5) {
                return opcode - 3;
            }
        } else if (instruction instanceof IntInsnNode) {
            int opcode = instruction.getOpcode();
            if (opcode == SIPUSH || opcode == BIPUSH) {
                return ((IntInsnNode) instruction).operand;
            }
        } else if (instruction instanceof LdcInsnNode) {
            Object cst = ((LdcInsnNode) instruction).cst;
            if (cst instanceof Integer) {
                return (Integer) cst;
            }
        }
        return null;
    }

    public static InsnList cloneList(InsnList insnList) {
        InsnList clone = new InsnList();
        for (AbstractInsnNode insnNode : insnList) {
            clone.add(insnNode.clone(null));
        }
        return clone;
    }

    public static String printInstruction(AbstractInsnNode instruction) {
        final String opcode = Printer.OPCODES[instruction.getOpcode()];
        final Map<String, String> data = new LinkedHashMap<>();
        if (instruction instanceof VarInsnNode) {
            data.put("var", String.valueOf(((VarInsnNode) instruction).var));
        }
        StringBuilder result = new StringBuilder(opcode);
        if (!data.isEmpty()) {
            boolean start = false;
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (start) {
                    result.append(", ");
                } else {
                    result.append(": ");
                }
                String key = entry.getKey();
                String value = entry.getValue();
                result.append(key).append("=").append(value);
                start = true;
            }
        }
        return result.toString();
    }

    public static Number decodeConstLoad(int opcode) {
        switch (opcode) {
            case ICONST_M1: return -1;
            case ICONST_0: return 0;
            case ICONST_1: return 1;
            case ICONST_2: return 2;
            case ICONST_3: return 3;
            case ICONST_4: return 4;
            case ICONST_5: return 5;
            case LCONST_0: return 0L;
            case LCONST_1: return 1L;
            case FCONST_0: return 0.F;
            case FCONST_1: return 1.F;
            case FCONST_2: return 2.F;
            case DCONST_0: return 0.D;
            case DCONST_1: return 1.D;
            default: throw new RuntimeException("Not XCONST_X: " + opcode);
        }
    }

    public static boolean isIntegerInstruction(AbstractInsnNode insnNode) {
        int opcode = insnNode.getOpcode();
        if (insnNode instanceof LdcInsnNode) {
            Object cst = ((LdcInsnNode) insnNode).cst;
            return cst instanceof Integer;
        }
        if (opcode >= ICONST_M1 && opcode <= ICONST_5) return true;
        if (opcode == BIPUSH || opcode == SIPUSH) return true;
        return false;
    }
}
