package me.f1nal.trinity.execution.xref;

import me.f1nal.trinity.util.NameUtil;
import org.objectweb.asm.Opcodes;

/** Supplies concise, consistent labels for bytecode operations shown in xref results. */
final class XrefInvocationFormatter {
    private XrefInvocationFormatter() {
    }

    static String instruction(int opcode) {
        return switch (opcode) {
            case Opcodes.INVOKEVIRTUAL -> "Invoke (Virtual)";
            case Opcodes.INVOKESPECIAL -> "Invoke (Special)";
            case Opcodes.INVOKESTATIC -> "Invoke (Static)";
            case Opcodes.INVOKEINTERFACE -> "Invoke (Interface)";
            case Opcodes.INVOKEDYNAMIC -> "Invoke (Dynamic)";
            case Opcodes.GETFIELD -> "Field (Get)";
            case Opcodes.PUTFIELD -> "Field (Put)";
            case Opcodes.GETSTATIC -> "Static (Get)";
            case Opcodes.PUTSTATIC -> "Static (Put)";
            case Opcodes.NEW -> "New";
            case Opcodes.ANEWARRAY -> "New (Array)";
            case Opcodes.MULTIANEWARRAY -> "New (Multi Array)";
            case Opcodes.CHECKCAST -> "Cast";
            case Opcodes.INSTANCEOF -> "Instance Of";
            default -> NameUtil.getOpcodeName(opcode);
        };
    }

    static String handle(int tag) {
        return switch (tag) {
            case Opcodes.H_GETFIELD -> instruction(Opcodes.GETFIELD);
            case Opcodes.H_GETSTATIC -> instruction(Opcodes.GETSTATIC);
            case Opcodes.H_PUTFIELD -> instruction(Opcodes.PUTFIELD);
            case Opcodes.H_PUTSTATIC -> instruction(Opcodes.PUTSTATIC);
            case Opcodes.H_INVOKEVIRTUAL -> instruction(Opcodes.INVOKEVIRTUAL);
            case Opcodes.H_INVOKESTATIC -> instruction(Opcodes.INVOKESTATIC);
            case Opcodes.H_INVOKESPECIAL -> instruction(Opcodes.INVOKESPECIAL);
            case Opcodes.H_NEWINVOKESPECIAL -> "Invoke (Constructor)";
            case Opcodes.H_INVOKEINTERFACE -> instruction(Opcodes.INVOKEINTERFACE);
            default -> "Handle (" + tag + ')';
        };
    }
}
