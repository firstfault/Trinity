package me.f1nal.trinity.execution.xref;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XrefKindTest {
    @Test
    void everyKindHasTooltipDetails() {
        for (XrefKind kind : XrefKind.values()) {
            assertFalse(kind.getDescription().isBlank(), kind.getName());
            assertFalse(kind.getTypeNames().isEmpty(), kind.getName());
            kind.getTypeNames().forEach(type -> assertFalse(type.isBlank(), kind.getName()));
        }
    }

    @Test
    void contextLabelsMatchTheirCanonicalTooltipType() {
        assertTrue(XrefKind.INVOKE.matchesTypeName(
                "Invoke (Static)", "Invoke (Dynamic) bootstrap handle Invoke (Static)"));
        assertTrue(XrefKind.TYPE.matchesTypeName(
                "Handle descriptor", "LDC handle descriptor"));
        assertTrue(XrefKind.ANNOTATION.matchesTypeName("Enum constant", "ENTRY"));
    }
}
