package me.f1nal.trinity.keybindings;

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
    void exposesStableDefaultInstructionMappings() {
        KeyBindManager manager = new KeyBindManager();

        assertEquals(GLFW.GLFW_KEY_A, manager.ASSEMBLER_INSERT.getKeyCode());
        assertEquals(GLFW.GLFW_KEY_E, manager.ASSEMBLER_EDIT.getKeyCode());
        assertEquals("E", manager.ASSEMBLER_EDIT.getKeyName());
        assertTrue(manager.ASSEMBLER_EDIT.isDefault());
        assertEquals(GLFW.GLFW_KEY_A, manager.DECOMPILER_ASSEMBLE.getKeyCode());
        assertEquals(GLFW.GLFW_KEY_R, manager.DECOMPILER_RENAME.getKeyCode());
        assertEquals(GLFW.GLFW_KEY_E, manager.DECOMPILER_EDIT.getKeyCode());
        assertEquals(GLFW.GLFW_KEY_X, manager.DECOMPILER_VIEW_XREFS.getKeyCode());
        assertEquals(GLFW.GLFW_KEY_V, manager.DECOMPILER_VIEW_MEMBER.getKeyCode());
    }

    @Test
    void assigningAChordClearsConflictsInTheSameScope() {
        KeyBindManager manager = new KeyBindManager();

        Bindable conflict = manager.bind(manager.ASSEMBLER_EDIT, GLFW.GLFW_KEY_A,
                false, false, false, false);

        assertSame(manager.ASSEMBLER_INSERT, conflict);
        assertFalse(manager.ASSEMBLER_INSERT.isBound());
        assertEquals(GLFW.GLFW_KEY_A, manager.ASSEMBLER_EDIT.getKeyCode());
    }

    @Test
    void loadsPersistedModifierChordsByStableIdentifier() {
        KeyBindManager manager = new KeyBindManager();
        manager.load(Set.of(new KeyBindingData(GLFW.GLFW_KEY_K, "assembler.instruction.edit",
                true, true, false, false)));

        assertEquals(GLFW.GLFW_KEY_K, manager.ASSEMBLER_EDIT.getKeyCode());
        assertTrue(manager.ASSEMBLER_EDIT.isControl());
        assertTrue(manager.ASSEMBLER_EDIT.isShift());
        assertEquals("Ctrl+Shift+K", manager.ASSEMBLER_EDIT.getKeyName());
    }
}
