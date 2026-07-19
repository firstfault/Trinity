package me.f1nal.trinity.gui.windows.impl.assembler.popup;

import imgui.ImGui;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.windows.api.PopupWindow;

public final class AssemblerUnsavedChangesPopup extends PopupWindow {
    private final Runnable save;
    private final Runnable discard;
    private final Runnable cancel;

    public AssemblerUnsavedChangesPopup(Trinity trinity, Runnable save, Runnable discard, Runnable cancel) {
        super("Unsaved assembler changes", trinity);
        this.save = save;
        this.discard = discard;
        this.cancel = cancel;
    }

    @Override
    protected void renderFrame() {
        ImGui.text("This method has unsaved bytecode changes.");
        if (ImGui.button("Save")) {
            save.run();
            close();
        }
        ImGui.sameLine();
        if (ImGui.button("Discard")) {
            discard.run();
            close();
        }
        ImGui.sameLine();
        if (ImGui.button("Cancel")) {
            cancel.run();
            close();
        }
    }

    @Override
    public boolean canCloseOnEscapeNow() {
        cancel.run();
        return true;
    }
}
