package me.f1nal.trinity.gui.frames.impl.assembler.popup.edit;

import imgui.ImGui;
import imgui.type.ImInt;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class EditFieldInteger extends EditField<Integer> {
    private final String label;
    private final int dataType;
    private final ImInt value = new ImInt();

    public EditFieldInteger(String label, Supplier<Integer> getter, Consumer<Integer> setter, int dataType) {
        super(getter, setter);
        this.label = label;
        this.dataType = dataType;
    }

    @Override
    public void draw() {
        if (ImGui.inputScalar(this.label, this.dataType, value, 1, 3)) {
            this.set(this.value.get());
        }
    }

    @Override
    public void updateField() {
        value.set(this.get());
    }

    @Override
    public boolean isValidInput() {
        return true;
    }
}
