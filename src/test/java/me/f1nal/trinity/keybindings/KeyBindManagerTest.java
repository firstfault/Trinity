package me.f1nal.trinity.keybindings;

import imgui.flag.ImGuiKey;
import me.f1nal.trinity.appdata.keybindings.KeyBindingData;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyBindManagerTest {
    @Test
    void assigningAChordClearsConflictsInTheSameScope() {
        KeyBindManager manager = new KeyBindManager();

        Bindable conflict = manager.bind(manager.ASSEMBLER_EDIT, ImGuiKey.A,
                false, false, false, false);

        assertSame(manager.ASSEMBLER_INSERT, conflict);
        assertFalse(manager.ASSEMBLER_INSERT.isBound());
        assertEquals(ImGuiKey.A, manager.ASSEMBLER_EDIT.getKeyCode());
    }

    @Test
    void migratesPersistedGlfwModifierChordsByStableIdentifier() {
        KeyBindManager manager = new KeyBindManager();
        manager.load(Set.of(new KeyBindingData(GLFW.GLFW_KEY_K, "assembler.instruction.edit",
                true, true, false, false)));

        assertEquals(ImGuiKey.K, manager.ASSEMBLER_EDIT.getKeyCode());
        assertTrue(manager.ASSEMBLER_EDIT.isControl());
        assertTrue(manager.ASSEMBLER_EDIT.isShift());
        assertEquals("Ctrl+Shift+K", manager.ASSEMBLER_EDIT.getKeyName());
    }

    @Test
    void persistsMouseButtonMappingsByStableIdentifier() {
        KeyBindManager manager = new KeyBindManager();
        manager.load(Set.of(new KeyBindingData(
                Bindable.mouseButtonCode(4), "decompiler.navigation.back")));

        assertEquals(Bindable.mouseButtonCode(4), manager.DECOMPILER_NAVIGATE_BACK.getKeyCode());
        assertEquals("Mouse 5", manager.DECOMPILER_NAVIGATE_BACK.getKeyName());
        assertFalse(manager.DECOMPILER_NAVIGATE_FORWARD.isBound());
    }
}
