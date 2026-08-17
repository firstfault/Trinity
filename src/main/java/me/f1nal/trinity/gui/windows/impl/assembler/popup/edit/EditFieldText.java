package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class EditFieldText<T> extends EditField<T> {
    private final String label, hint;
    protected final ImString text;
    protected int inputTextFlags = ImGuiInputTextFlags.None;
    private Boolean valid;
    private String error;

    EditFieldText(int length, String label, String hint, Supplier<T> getter, Consumer<T> setter) {
        super(getter, setter);
        this.text = new ImString(length);
        this.label = label;
        this.hint = hint;
    }

    protected ImString getText() {
        return text;
    }

    @Override
    public void draw() {
        if (ImGui.inputTextWithHint(this.label, this.hint, this.text, inputTextFlags)) {
            this.applyText(this.text.get());
        }
        if (error != null && !error.isBlank()) ImGui.textColored(CodeColorScheme.NOTIFY_ERROR, error);
    }

    private boolean applyText(String input) {
        try {
            T parsed = this.parse(input);
            this.valid = true;
            this.error = null;
            this.set(parsed);
            return true;
        } catch (InvalidEditInputException exception) {
            this.valid = false;
            this.error = exception.getMessage();
            this.update();
            return false;
        }
    }

    @Override
    public String getInlineLabel() {
        return label;
    }

    @Override
    protected String formatInlineValue() {
        return text.get();
    }

    @Override
    public boolean applyInlineValue(String value) {
        this.text.set(value);
        return this.applyText(value);
    }

    @Override
    public String getInlineError() {
        return error;
    }

    protected abstract T parse(String input) throws InvalidEditInputException;

    @Override
    public boolean isValidInput() {
        return !this.text.get().isEmpty() && (valid == null || valid);
    }
}
