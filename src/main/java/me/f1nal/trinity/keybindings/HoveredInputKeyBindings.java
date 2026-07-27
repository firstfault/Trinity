package me.f1nal.trinity.keybindings;

import imgui.ImGui;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.navigation.NavigationAction;
import me.f1nal.trinity.gui.windows.impl.bytecode.BytecodeEditorLauncher;

/** Dispatches decompiler-member bindings for an input represented by a hovered UI row. */
public final class HoveredInputKeyBindings {
    private HoveredInputKeyBindings() {
    }

    public static void handle(Input<?> input, Runnable renameAction) {
        if (input == null || ImGui.isAnyItemActive()) {
            return;
        }

        KeyBindManager bindings = Main.getKeyBindManager();
        if (bindings.DECOMPILER_ASSEMBLE.isPressed()) {
            if (input instanceof MethodInput method) method.openAssembler();
        } else if (bindings.DECOMPILER_RENAME.isPressed()) {
            if (renameAction != null) renameAction.run();
        } else if (bindings.DECOMPILER_EDIT.isPressed()) {
            BytecodeEditorLauncher.edit(input);
        } else if (bindings.DECOMPILER_VIEW_XREFS.isPressed()) {
            input.viewXrefs(Main.getTrinity());
        } else if (bindings.DECOMPILER_VIEW_MEMBER.isPressed()) {
            Main.getDisplayManager().followDecompilerView(input, NavigationAction.FOLLOW_MEMBER);
        }
    }
}
