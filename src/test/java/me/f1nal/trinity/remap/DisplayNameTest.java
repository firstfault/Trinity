package me.f1nal.trinity.remap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayNameTest {
    @Test
    void refusesEmptyAndWhitespaceOnlyNames() {
        DisplayName displayName = new DisplayName("original");

        displayName.setName("");
        assertEquals("original", displayName.getName());

        displayName.setName("   ");
        assertEquals("original", displayName.getName());
    }
}
