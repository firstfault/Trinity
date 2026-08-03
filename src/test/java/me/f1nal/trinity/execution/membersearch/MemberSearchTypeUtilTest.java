package me.f1nal.trinity.execution.membersearch;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberSearchTypeUtilTest {
    @Test
    void parsesJavaNamesDescriptorsArraysAndParameterLists() {
        assertEquals("[[Ljava/lang/String;",
                MemberSearchTypeUtil.parseType("java.lang.String[][]", false).getDescriptor());
        assertEquals("[I", MemberSearchTypeUtil.parseType("int[]", false).getDescriptor());
        assertEquals("Ljava/util/List;",
                MemberSearchTypeUtil.parseType("Ljava/util/List;", false).getDescriptor());

        assertEquals(List.of(Type.INT_TYPE, Type.getType("[Ljava/lang/String;")),
                MemberSearchTypeUtil.parseParameterList("(I[Ljava/lang/String;)V"));
        assertEquals(List.of(Type.getType(String.class), Type.getType("[I")),
                MemberSearchTypeUtil.parseParameterList("java.lang.String, int[]"));
    }

    @Test
    void rejectsVoidArraysAndMethodDescriptorsAsTypes() {
        assertThrows(IllegalArgumentException.class,
                () -> MemberSearchTypeUtil.parseType("void[]", true));
        assertThrows(IllegalArgumentException.class,
                () -> MemberSearchTypeUtil.parseType("(I)V", false));
        assertThrows(IllegalArgumentException.class,
                () -> MemberSearchTypeUtil.parseType("void", false));
    }

    @Test
    void exactTypeMatchingDoesNotCollapseArrayDimensionsOrPrimitiveKinds() {
        assertTrue(MemberSearchTypeUtil.matches(Type.getType("[[Ljava/lang/String;"),
                Type.getType("[[Ljava/lang/String;"), MemberSearchQuery.TypeMode.EXACT, null));
        assertFalse(MemberSearchTypeUtil.matches(Type.getType("[[Ljava/lang/String;"),
                Type.getType("[Ljava/lang/String;"), MemberSearchQuery.TypeMode.EXACT, null));
        assertFalse(MemberSearchTypeUtil.matches(Type.LONG_TYPE, Type.INT_TYPE,
                MemberSearchQuery.TypeMode.EXACT, null));
    }

    @Test
    void genericSignatureSearchWalksNestedTypeArguments() {
        String signature = "Ljava/util/Map<Ljava/lang/String;Ljava/util/List<Lsample/Target;>;>;";
        assertTrue(MemberSearchTypeUtil.signatureContains(signature, "sample/Target"));
        assertTrue(MemberSearchTypeUtil.signatureContains(signature, "java/util/List"));
        assertFalse(MemberSearchTypeUtil.signatureContains(signature, "sample/Missing"));
    }
}
