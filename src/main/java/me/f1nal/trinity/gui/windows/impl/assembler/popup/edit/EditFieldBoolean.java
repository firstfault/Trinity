package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import imgui.ImGui;
import imgui.type.ImBoolean;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class EditFieldBoolean extends EditField<Boolean> {
    private final String label;
    private final ImBoolean value = new ImBoolean();
    private boolean inlineValid = true;

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
        return inlineValid;
    }

    @Override
    public String getInlineLabel() {
        return label;
    }

    @Override
    public boolean applyInlineValue(String input) {
        if (!input.equalsIgnoreCase("true") && !input.equalsIgnoreCase("false")) {
            this.inlineValid = false;
            this.update();
            return false;
        }
        boolean parsed = Boolean.parseBoolean(input);
        this.value.set(parsed);
        this.inlineValid = true;
        this.set(parsed);
        return true;
    }

    @Override
    public List<String> getInlineSuggestions() {
        return List.of("true", "false");
    }

    @Override
    public String getInlineError() {
        return inlineValid ? null : "Expected true or false";
    }
}
