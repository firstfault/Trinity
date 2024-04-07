package me.f1nal.trinity.gui.frames.impl.assembler.popup.edit;

import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class EditFieldText<T> extends EditField<T> {
    private final String label, hint;
    protected final ImString text;
    protected int inputTextFlags = ImGuiInputTextFlags.None;
    private Boolean valid;

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
            T parse;
            try {
                parse = this.parse(this.text.get());
                this.valid = true;
            } catch (InvalidEditInputException e) {
                this.valid = false;
                this.update();
                return;
            }
            this.set(parse);
        }
    }

    protected abstract T parse(String input) throws InvalidEditInputException;

    @Override
    public boolean isValidInput() {
        return !this.text.get().isEmpty() && (valid == null || valid);
    }
}
