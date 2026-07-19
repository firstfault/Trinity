package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import imgui.ImGui;
import imgui.type.ImBoolean;

import java.util.function.Consumer;
import java.util.function.Supplier;

final class EditFieldBoolean extends EditField<Boolean> {
    private final String label;
    private final ImBoolean value = new ImBoolean();

    EditFieldBoolean(String label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        super(getter, setter);
        this.label = label;
    }

    @Override
    public void draw() {
        if (ImGui.checkbox(label, value)) set(value.get());
    }

    @Override
    public void updateField() {
        value.set(get());
    }

    @Override
    public boolean isValidInput() {
        return true;
    }
}
