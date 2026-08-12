package me.f1nal.trinity.gui.windows.impl.refactor.identity;

import imgui.ImGui;
import imgui.flag.ImGuiKey;
import imgui.type.ImString;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.gui.windows.api.PopupWindow;
import me.f1nal.trinity.theme.CodeColorScheme;

final class IdentityRenameInputPopup extends PopupWindow {
    private final Input<?> input;
    private final String initialName;
    private final ImString name = new ImString(512);
    private boolean focus = true;
    private boolean submitted;

    IdentityRenameInputPopup(Trinity trinity, Input<?> input, String initialName) {
        super("Rename Bytecode Identity", trinity);
        this.input = input;
        this.initialName = initialName == null ? "" : initialName;
        this.name.set(this.initialName);
    }

    @Override
    protected void renderFrame() {
        ImGui.textUnformatted("Current identity");
        ImGui.textColored(CodeColorScheme.DISABLED, input.toString());
        ImGui.spacing();
        ImGui.textUnformatted(input.getType() == me.f1nal.trinity.execution.InputType.CLASS
                ? "New internal name" : "New member name");
        ImGui.setNextItemWidth(440.F);
        if (focus) {
            ImGui.setKeyboardFocusHere();
            focus = false;
        }
        boolean enterSubmitted = ImGui.inputText("###IdentityName", name,
                imgui.flag.ImGuiInputTextFlags.EnterReturnsTrue);
        ImGui.spacing();
        if (!submitted && (ImGui.button("Analyze Rename") || enterSubmitted
                || isKeyboardInputReady() && !ImGui.isAnyItemActive()
                && (ImGui.isKeyPressed(ImGuiKey.Enter, false)
                || ImGui.isKeyPressed(ImGuiKey.KeypadEnter, false)))) {
            submitted = true;
            close();
            IdentityRefactorController.submit(input, name.get(), false, initialName);
        }
        ImGui.sameLine();
        if (ImGui.button("Cancel")) close();
    }

    boolean targets(Input<?> candidate) {
        return input == candidate;
    }
}
