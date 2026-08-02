package me.f1nal.trinity.keybindings;

import imgui.ImGui;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.navigation.NavigationAction;
import me.f1nal.trinity.gui.windows.impl.bytecode.BytecodeEditorLauncher;

/** Resolves member shortcuts once per frame, preferring hovered rows over focused-window fallbacks. */
public final class HoveredInputKeyBindings {
    private static Runnable hoveredHandler;
    private static Runnable focusedHandler;

    private HoveredInputKeyBindings() {
    }

    public static void beginFrame() {
        hoveredHandler = null;
        focusedHandler = null;
    }

    public static void offerHovered(Input<?> input, Runnable renameAction) {
        if (input == null) return;
        hoveredHandler = () -> handle(input, renameAction);
    }

    public static void offerFocused(Runnable handler) {
        if (focusedHandler == null) focusedHandler = handler;
    }

    public static void dispatch() {
        Runnable handler = hoveredHandler != null ? hoveredHandler : focusedHandler;
        hoveredHandler = null;
        focusedHandler = null;
        if (handler != null && !ImGui.isAnyItemActive()) handler.run();
    }

    private static void handle(Input<?> input, Runnable renameAction) {
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
