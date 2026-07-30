package me.f1nal.trinity.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayManagerSaveDecisionTest {
    @Test
    void closesOnlyAfterAnExplicitlySuccessfulSave() {
        assertTrue(DisplayManager.shouldCloseAfterSave(Boolean.TRUE));
        assertFalse(DisplayManager.shouldCloseAfterSave(Boolean.FALSE));
        assertFalse(DisplayManager.shouldCloseAfterSave(null));
    }
}
