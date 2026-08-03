package me.f1nal.trinity.execution.xref;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class XrefKindTest {
    @Test
    void everyKindHasTooltipDetails() {
        for (XrefKind kind : XrefKind.values()) {
            assertFalse(kind.getDescription().isBlank(), kind.getName());
            assertFalse(kind.getTypeNames().isEmpty(), kind.getName());
            kind.getTypeNames().forEach(type -> assertFalse(type.isBlank(), kind.getName()));
        }
    }
}
