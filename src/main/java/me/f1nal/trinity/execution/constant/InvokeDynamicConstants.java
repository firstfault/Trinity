package me.f1nal.trinity.execution.constant;

import org.objectweb.asm.tree.InvokeDynamicInsnNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Resolves user-visible constants encoded inside {@code invokedynamic} instructions. */
public final class InvokeDynamicConstants {
    private static final String CONCAT_FACTORY = "java/lang/invoke/StringConcatFactory";
    private static final String CONCAT_WITH_CONSTANTS = "makeConcatWithConstants";
    private static final char ARGUMENT_TAG = '\u0001';
    private static final char CONSTANT_TAG = '\u0002';

    private InvokeDynamicConstants() {
    }

    public static List<Object> resolve(InvokeDynamicInsnNode instruction) {
        if (!isStringConcat(instruction) || instruction.bsmArgs.length == 0
                || !(instruction.bsmArgs[0] instanceof String recipe)) {
            return Arrays.asList(instruction.bsmArgs);
        }

        List<Object> constants = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int bootstrapArgument = 1;
        for (int i = 0; i < recipe.length(); i++) {
            char character = recipe.charAt(i);
            if (character != ARGUMENT_TAG && character != CONSTANT_TAG) {
                literal.append(character);
                continue;
            }

            flushLiteral(constants, literal);
            if (character == CONSTANT_TAG) {
                if (bootstrapArgument >= instruction.bsmArgs.length) {
                    return Arrays.asList(instruction.bsmArgs);
                }
                constants.add(instruction.bsmArgs[bootstrapArgument++]);
            }
        }
        flushLiteral(constants, literal);

        // Retain unusual, unreferenced bootstrap constants rather than silently
        // dropping data from malformed or custom factory invocations.
        while (bootstrapArgument < instruction.bsmArgs.length) {
            constants.add(instruction.bsmArgs[bootstrapArgument++]);
        }
        return List.copyOf(constants);
    }

    private static boolean isStringConcat(InvokeDynamicInsnNode instruction) {
        return instruction.bsm != null
                && CONCAT_WITH_CONSTANTS.equals(instruction.name)
                && CONCAT_FACTORY.equals(instruction.bsm.getOwner())
                && CONCAT_WITH_CONSTANTS.equals(instruction.bsm.getName());
    }

    private static void flushLiteral(List<Object> constants, StringBuilder literal) {
        if (literal.isEmpty()) return;
        constants.add(literal.toString());
        literal.setLength(0);
    }
}
