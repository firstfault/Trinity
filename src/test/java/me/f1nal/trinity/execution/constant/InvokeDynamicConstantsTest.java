package me.f1nal.trinity.execution.constant;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvokeDynamicConstantsTest {
    private static final Handle CONCAT_BOOTSTRAP = new Handle(Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/StringConcatFactory", "makeConcatWithConstants",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                    + "Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;", false);

    @Test
    void extractsLiteralAroundRuntimeArgumentTags() {
        InvokeDynamicInsnNode instruction = concat("E1ygaqdb\u0001");

        assertEquals(List.of("E1ygaqdb"), InvokeDynamicConstants.resolve(instruction));
    }

    @Test
    void retainsRepeatedLiteralOccurrences() {
        InvokeDynamicInsnNode instruction = concat("\u0001same\u0001same");

        assertEquals(List.of("same", "same"), InvokeDynamicConstants.resolve(instruction));
    }

    @Test
    void resolvesStaticConstantsAndRetainsUnreferencedArguments() {
        InvokeDynamicInsnNode instruction = concat("left\u0002right", "fixed", 42);

        assertEquals(List.of("left", "fixed", "right", 42),
                InvokeDynamicConstants.resolve(instruction));
    }

    @Test
    void fallsBackToRawArgumentsForMalformedRecipe() {
        InvokeDynamicInsnNode instruction = concat("missing\u0002");

        assertEquals(List.of("missing\u0002"), InvokeDynamicConstants.resolve(instruction));
    }

    @Test
    void leavesOtherBootstrapMethodsUnchanged() {
        Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, "example/Bootstrap", "link",
                CONCAT_BOOTSTRAP.getDesc(), false);
        InvokeDynamicInsnNode instruction = new InvokeDynamicInsnNode("dynamic", "()Ljava/lang/String;",
                bootstrap, "raw\u0001", 7);

        assertEquals(List.of("raw\u0001", 7), InvokeDynamicConstants.resolve(instruction));
    }

    private static InvokeDynamicInsnNode concat(String recipe, Object... constants) {
        Object[] arguments = new Object[constants.length + 1];
        arguments[0] = recipe;
        System.arraycopy(constants, 0, arguments, 1, constants.length);
        return new InvokeDynamicInsnNode("makeConcatWithConstants", "(I)Ljava/lang/String;",
                CONCAT_BOOTSTRAP, arguments);
    }
}
