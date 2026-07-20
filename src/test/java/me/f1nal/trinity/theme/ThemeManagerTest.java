package me.f1nal.trinity.theme;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeManagerTest {
    @Test
    void accentPaletteHasUniqueNamesAndColors() {
        assertEquals(AccentColor.values().length,
                Arrays.stream(AccentColor.values()).map(AccentColor::getName).distinct().count());
        assertEquals(AccentColor.values().length,
                Arrays.stream(AccentColor.values()).map(AccentColor::getColor).distinct().count());
    }
}
