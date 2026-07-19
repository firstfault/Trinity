package me.f1nal.trinity.gui.windows.impl.assembler;

import me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.AssemblerValueCodec;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssemblerValueCodecTest {
    @Test
    void roundTripsEveryLegalConstantKind() {
        Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, "sample/Owner", "bootstrap", "()Ljava/lang/Object;", false);
        ConstantDynamic dynamic = new ConstantDynamic("constant", "Ljava/lang/String;", bootstrap,
                7, "text", Type.getType("Ljava/lang/String;"), bootstrap);
        Object[] values = {
                42, -7L, Float.NaN, Double.NEGATIVE_INFINITY, "a\n\"b\\c",
                Type.getMethodType("(I)Ljava/lang/String;"), bootstrap, dynamic
        };
        for (Object value : values) {
            assertEquals(value, AssemblerValueCodec.parse(AssemblerValueCodec.format(value)));
        }
        assertArrayEquals(values, AssemblerValueCodec.parseList(AssemblerValueCodec.formatList(values)));
    }

    @Test
    void reportsTheInvalidCharacterPosition() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AssemblerValueCodec.parse("handle(H_UNKNOWN, \"x\", \"y\", \"()V\", false)"));
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("character"));
    }

    @Test
    void reportsAnEmptyTypeDescriptorWithoutLeakingAsmExceptions() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AssemblerValueCodec.parse("type(\"\")"));
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("Invalid type descriptor"));
    }
}
