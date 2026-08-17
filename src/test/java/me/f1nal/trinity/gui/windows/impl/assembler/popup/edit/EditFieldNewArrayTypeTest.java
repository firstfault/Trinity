package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditFieldNewArrayTypeTest {
    @Test
    void acceptsPrimitiveNamesAndRejectsUnknownTypes() {
        AtomicInteger value = new AtomicInteger(Opcodes.T_INT);
        EditFieldNewArrayType field = new EditFieldNewArrayType(value::get, value::set);
        field.updateField();
        field.prepareInlineValue();

        assertEquals("int", field.getInlineText().get());
        assertTrue(field.applyInlineValue("boolean"));
        assertEquals(Opcodes.T_BOOLEAN, value.get());
        assertFalse(field.applyInlineValue("java/lang/String"));
        assertEquals(Opcodes.T_BOOLEAN, value.get());
    }
}
