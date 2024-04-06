package me.f1nal.trinity.execution.xref;

import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.xref.where.XrefWhereMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.util.Printer;

/**
 * The {@link MemberXref} class represents a reference between a class member and code within a method.
 * It encapsulates information about the method and instruction involved in the reference.
 */
public class MemberXref extends AbstractXref {
    /**
     * The method in which the reference is found.
     */
    private final MethodInput methodInput;

    /**
     * The specific instruction that creates the reference.
     */
    private final AbstractInsnNode instruction;
    private final XrefAccessType access;

    /**
     * Constructs a new {@link MemberXref} instance with the provided method input and instruction.
     *
     * @param methodInput The method in which the reference occurs.
     * @param instruction The instruction responsible for the reference.
     */
    public MemberXref(MethodInput methodInput, AbstractInsnNode instruction) {
        super(new XrefWhereMethod(methodInput), instruction instanceof FieldInsnNode ? XrefKind.FIELD : XrefKind.INVOKE);
        this.methodInput = methodInput;
        this.instruction = instruction;
        this.access = getAccess(instruction);
    }

    /**
     * Retrieves the method input associated with this reference.
     *
     * @return The method input.
     */
    public MethodInput getMethodInput() {
        return methodInput;
    }

    /**
     * Retrieves the instruction responsible for this reference.
     *
     * @return The instruction creating the reference.
     */
    public AbstractInsnNode getInstruction() {
        return instruction;
    }

    private XrefAccessType getAccess(AbstractInsnNode instruction) {
        if (instruction instanceof MethodInsnNode) {
            return XrefAccessType.EXECUTE;
        }
        switch (instruction.getOpcode()) {
            case Opcodes.PUTFIELD:
            case Opcodes.PUTSTATIC:
                return XrefAccessType.WRITE;
            case Opcodes.GETFIELD:
            case Opcodes.GETSTATIC:
                return XrefAccessType.READ;
        }
        throw new RuntimeException("Edge case");
    }

    @Override
    public XrefAccessType getAccess() {
        return this.access;
    }

    @Override
    public String getInvocation() {
        return Printer.OPCODES[this.instruction.getOpcode()];
    }
}

