package me.f1nal.trinity.theme;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ThemeManagerTest {
    @Test
    void registersFawnAsTheBuiltInDefault() {
        ThemeManager manager = new ThemeManager();

        assertEquals("Fawn", manager.getDefaultTheme().getName());
        assertEquals("Fawn", manager.getCurrentTheme().getName());
        assertNotNull(manager.getTheme("Gerry Dark"));
    }

    @Test
    void accentPaletteHasUniqueNamesAndColors() {
        assertEquals(AccentColor.values().length,
                Arrays.stream(AccentColor.values()).map(AccentColor::getName).distinct().count());
        assertEquals(AccentColor.values().length,
                Arrays.stream(AccentColor.values()).map(AccentColor::getColor).distinct().count());
    }
}
