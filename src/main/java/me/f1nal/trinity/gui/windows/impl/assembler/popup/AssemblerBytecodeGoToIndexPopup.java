package me.f1nal.trinity.gui.windows.impl.assembler.popup;

import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.windows.api.PopupWindow;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerFrame;

public class AssemblerBytecodeGoToIndexPopup extends PopupWindow {
    private final AssemblerFrame parent;
    private final ImString input = new ImString(12);

    public AssemblerBytecodeGoToIndexPopup(AssemblerFrame parent, Trinity trinity) {
        super("Bytecode Go To", trinity);
        this.parent = parent;
    }

    @Override
    protected void renderFrame() {
        ImGui.text("Enter a bytecode index to jump to:");
        ImGui.inputText("Bytecode Index", this.input, ImGuiInputTextFlags.CharsDecimal | ImGuiInputTextFlags.AlwaysOverwrite);
        final int index = this.getIndex();
        final boolean disabled = index < 0 || index >= parent.getInstructions().size();
        if (disabled) ImGui.beginDisabled();
        if (ImGui.button("Go To")) {
            if (!disabled) this.gotoIndex(index);
        }
        if (disabled) ImGui.endDisabled();
        ImGui.sameLine();
        if (ImGui.button("Cancel")) {
            this.close();
        }
    }

    private int getIndex() {
        try {
            return Integer.parseInt(this.input.get());
        } catch (Throwable throwable) {
            return -1;
        }
    }

    private void gotoIndex(int index) {
        parent.setInstructionView(parent.getInstructions().get(index));
        this.close();
    }
}
