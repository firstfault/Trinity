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
            T parse = null;
            boolean parsed = false;
            try {
                parse = this.parse(this.text.get());
                this.valid = true;
                this.error = null;
                parsed = true;
            } catch (InvalidEditInputException e) {
                this.valid = false;
                this.error = e.getMessage();
                this.update();
            }
            if (parsed) {
                this.set(parse);
            }
        }
        if (error != null && !error.isBlank()) ImGui.textColored(CodeColorScheme.NOTIFY_ERROR, error);
    }

    protected abstract T parse(String input) throws InvalidEditInputException;

    @Override
    public boolean isValidInput() {
        return !this.text.get().isEmpty() && (valid == null || valid);
    }
}
