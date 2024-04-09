package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class EditFieldString extends EditFieldText<String> {
    EditFieldString(int length, String label, String hint, Supplier<String> getter, Consumer<String> setter) {
        super(length, label, hint, getter, setter);
    }

    @Override
    protected String parse(String input) {
        return input;
    }

    @Override
    public void updateField() {
        text.set(get());
    }
}
