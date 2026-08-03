package me.f1nal.trinity.gui.windows.impl.constant.search;

import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewCache;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstantSearchTypeAsmTest {
    private static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            "bootstrap/Factory",
            "make",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                    + "Ljava/lang/Class;)Ljava/lang/Object;",
            false);

    @Test
    void typeSearchAcceptsSourceInternalDescriptorAndJvmTypeNames() {
        Type string = Type.getType("Ljava/lang/String;");

        assertTrue(new ConstantSearchTypeType(null, "java.lang.String").matches(string));
        assertTrue(new ConstantSearchTypeType(null, "java/lang/String").matches(string));
        assertTrue(new ConstantSearchTypeType(null, "Ljava/lang/String;").matches(string));
        assertTrue(new ConstantSearchTypeType(null, "java.lang.String.class").matches(string));
        assertFalse(new ConstantSearchTypeType(null, "java.lang.Integer").matches(string));
        assertTrue(new ConstantSearchTypeType(null, "int").matches(Type.INT_TYPE));
        assertTrue(new ConstantSearchTypeType(null, "java.lang.String[]")
                .matches(Type.getType("[Ljava/lang/String;")));
        assertTrue(new ConstantSearchTypeType(null, "(I)Ljava/lang/String;")
                .matches(Type.getMethodType("(I)Ljava/lang/String;")));
    }

    @Test
    void typeSearchRejectsMalformedDescriptorsAndFormatsResultsReadably() {
        assertNull(ConstantSearchTypeType.normalizeQuery("(I"));
        ArrayList<ConstantViewCache> unused = new ArrayList<>();
        new ConstantSearchTypeType(null, "(I").populate(unused);
        assertTrue(unused.isEmpty());
        assertEquals("java.lang.String.class",
                ConstantSearchTypeType.format(Type.getType("Ljava/lang/String;")));
        assertEquals("java.lang.String[].class",
                ConstantSearchTypeType.format(Type.getType("[Ljava/lang/String;")));
        assertEquals("method-type (I)V",
                ConstantSearchTypeType.format(Type.getMethodType("(I)V")));
    }

    @Test
    void handleSearchMatchesOwnerMemberDescriptorAndKind() {
        ConstantSearchTypeHandle all = new ConstantSearchTypeHandle(null, "");

        assertTrue(all.matches(BOOTSTRAP));
        assertTrue(new ConstantSearchTypeHandle(null, "bootstrap.Factory").matches(BOOTSTRAP));
        assertTrue(new ConstantSearchTypeHandle(null, "make").matches(BOOTSTRAP));
        assertTrue(new ConstantSearchTypeHandle(null, "MethodHandles$Lookup").matches(BOOTSTRAP));
        assertTrue(new ConstantSearchTypeHandle(null, "invokestatic").matches(BOOTSTRAP));
        assertFalse(new ConstantSearchTypeHandle(null, "invokevirtual").matches(BOOTSTRAP));
    }

    @Test
    void constantDynamicSearchMatchesNameTypeAndBootstrap() {
        ConstantDynamic dynamic = new ConstantDynamic(
                "configuration", "Ljava/lang/String;", BOOTSTRAP, "payload");

        assertTrue(new ConstantSearchTypeConstantDynamic(null, "").matches(dynamic));
        assertTrue(new ConstantSearchTypeConstantDynamic(null, "configuration").matches(dynamic));
        assertTrue(new ConstantSearchTypeConstantDynamic(null, "java/lang/String").matches(dynamic));
        assertTrue(new ConstantSearchTypeConstantDynamic(null, "bootstrap.Factory").matches(dynamic));
        assertFalse(new ConstantSearchTypeConstantDynamic(null, "unrelated").matches(dynamic));
    }
}
