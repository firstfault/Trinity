package me.f1nal.trinity.execution.xref;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XrefInvocationFormatterTest {
    @Test
    void formatsInvocationInstructions() {
        assertEquals("Invoke (Virtual)",
                XrefInvocationFormatter.instruction(Opcodes.INVOKEVIRTUAL));
        assertEquals("Invoke (Special)",
                XrefInvocationFormatter.instruction(Opcodes.INVOKESPECIAL));
        assertEquals("Invoke (Static)",
                XrefInvocationFormatter.instruction(Opcodes.INVOKESTATIC));
        assertEquals("Invoke (Interface)",
                XrefInvocationFormatter.instruction(Opcodes.INVOKEINTERFACE));
        assertEquals("Invoke (Dynamic)",
                XrefInvocationFormatter.instruction(Opcodes.INVOKEDYNAMIC));
    }

    @Test
    void formatsFieldInstructions() {
        assertEquals("Field (Get)",
                XrefInvocationFormatter.instruction(Opcodes.GETFIELD));
        assertEquals("Field (Put)",
                XrefInvocationFormatter.instruction(Opcodes.PUTFIELD));
        assertEquals("Static (Get)",
                XrefInvocationFormatter.instruction(Opcodes.GETSTATIC));
        assertEquals("Static (Put)",
                XrefInvocationFormatter.instruction(Opcodes.PUTSTATIC));
    }

    @Test
    void formatsCastAndHandleOperations() {
        assertEquals("New", XrefInvocationFormatter.instruction(Opcodes.NEW));
        assertEquals("New (Array)",
                XrefInvocationFormatter.instruction(Opcodes.ANEWARRAY));
        assertEquals("New (Multi Array)",
                XrefInvocationFormatter.instruction(Opcodes.MULTIANEWARRAY));
        assertEquals("Cast", XrefInvocationFormatter.instruction(Opcodes.CHECKCAST));
        assertEquals("Instance Of",
                XrefInvocationFormatter.instruction(Opcodes.INSTANCEOF));
        assertEquals("Invoke (Constructor)",
                XrefInvocationFormatter.handle(Opcodes.H_NEWINVOKESPECIAL));
        assertEquals("Static (Get)",
                XrefInvocationFormatter.handle(Opcodes.H_GETSTATIC));
    }
}
