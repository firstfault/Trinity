package me.f1nal.trinity.gui.navigation;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.InputType;
import me.f1nal.trinity.execution.MethodInput;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.Objects;

/** A session-stable decompiler destination that can survive member reindexing. */
public final class NavigationTarget {
    private final ClassTarget classTarget;
    private final InputType inputType;
    private final Object memberNode;
    private final String memberName;
    private final String memberDescriptor;
    private final AbstractInsnNode instruction;
    private final int instructionIndex;

    private NavigationTarget(ClassTarget classTarget, InputType inputType, Object memberNode,
                             String memberName, String memberDescriptor,
                             @Nullable AbstractInsnNode instruction, int instructionIndex) {
        this.classTarget = classTarget;
        this.inputType = inputType;
        this.memberNode = memberNode;
        this.memberName = memberName;
        this.memberDescriptor = memberDescriptor;
        this.instruction = instruction;
        this.instructionIndex = instructionIndex;
    }

    public static NavigationTarget capture(Input<?> input, @Nullable AbstractInsnNode instruction) {
        ClassInput owner = input.getOwningClass();
        Object memberNode = null;
        String memberName = null;
        String memberDescriptor = null;
        int instructionIndex = -1;
        if (input instanceof MethodInput method) {
            memberNode = method.getNode();
            memberName = method.getName();
            memberDescriptor = method.getDescriptor();
            if (instruction != null) instructionIndex = method.getInstructions().indexOf(instruction);
        } else if (input instanceof FieldInput field) {
            memberNode = field.getNode();
            memberName = field.getDetails().getName();
            memberDescriptor = field.getDescriptor();
        }
        return new NavigationTarget(owner.getClassTarget(), input.getType(), memberNode,
                memberName, memberDescriptor, instruction, instructionIndex);
    }

    public @Nullable ResolvedNavigation resolve(Trinity trinity) {
        if (trinity == null || classTarget.getInput() == null) {
            return null;
        }
        ClassInput owner = classTarget.getInput();
        Input<?> input = switch (inputType) {
            case CLASS -> owner;
            case METHOD -> resolveMethod(owner);
            case FIELD -> resolveField(owner);
            default -> null;
        };
        if (input == null) {
            return null;
        }
        return new ResolvedNavigation(input, resolveInstruction(input));
    }

    public String describe(Trinity trinity) {
        ResolvedNavigation resolved = resolve(trinity);
        Input<?> input = resolved == null ? null : resolved.input();
        String ownerName = classTarget.getDisplaySimpleName();
        if (input instanceof MethodInput method) {
            return "method " + ownerName + "." + method.getDisplayName().getName()
                    + (resolved.instruction() == null ? "" : " usage");
        }
        if (input instanceof FieldInput field) {
            return "field " + ownerName + "." + field.getDisplayName().getName();
        }
        if (input instanceof ClassInput) {
            return "class " + ownerName;
        }
        if (memberName != null) {
            String type = inputType == InputType.METHOD ? "method " : "field ";
            return type + ownerName + "." + memberName;
        }
        return "class " + ownerName;
    }

    private @Nullable MethodInput resolveMethod(ClassInput owner) {
        for (MethodInput method : owner.getMethodMap().values()) {
            if (method.getNode() == memberNode) return method;
        }
        return owner.getMethod(memberName, memberDescriptor);
    }

    private @Nullable FieldInput resolveField(ClassInput owner) {
        for (FieldInput field : owner.getFieldMap().values()) {
            if (field.getNode() == memberNode) return field;
        }
        return owner.getField(memberName, memberDescriptor);
    }

    private @Nullable AbstractInsnNode resolveInstruction(Input<?> input) {
        if (!(input instanceof MethodInput method) || instruction == null) {
            return null;
        }
        if (method.getInstructions().indexOf(instruction) >= 0) {
            return instruction;
        }
        return instructionIndex >= 0 && instructionIndex < method.getInstructions().size()
                ? method.getInstructions().get(instructionIndex) : null;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof NavigationTarget target)) return false;
        return instructionIndex == target.instructionIndex
                && classTarget == target.classTarget
                && inputType == target.inputType
                && memberNode == target.memberNode
                && instruction == target.instruction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(System.identityHashCode(classTarget), inputType,
                System.identityHashCode(memberNode), System.identityHashCode(instruction), instructionIndex);
    }

    public record ResolvedNavigation(Input<?> input, @Nullable AbstractInsnNode instruction) {
    }
}
