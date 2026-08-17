package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import imgui.ImGui;
import imgui.type.ImInt;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class EditFieldInteger extends EditField<Integer> {
    private final String label;
    private final int dataType;
    private final ImInt value = new ImInt();
    private boolean inlineValid = true;
    private String inlineError;

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
        return inlineValid;
    }

    @Override
    public String getInlineLabel() {
        return label;
    }

    @Override
    public boolean applyInlineValue(String input) {
        try {
            int parsed = Integer.parseInt(input.trim());
            if (dataType == imgui.flag.ImGuiDataType.U8 && (parsed < 0 || parsed > 255)) {
                throw new NumberFormatException("outside unsigned byte range");
            }
            this.value.set(parsed);
            this.inlineValid = true;
            this.inlineError = null;
            this.set(parsed);
            return true;
        } catch (NumberFormatException exception) {
            this.inlineValid = false;
            this.inlineError = "Expected an integer";
            this.update();
            return false;
        }
    }

    @Override
    public String getInlineError() {
        return inlineError;
    }
}
