package me.f1nal.trinity.gui.frames.impl.assembler.popup.edit;

import java.util.function.Consumer;

public class EditFieldString extends EditFieldText<String> {
    EditFieldString(int length, String label, String hint, Consumer<String> setter) {
        super(length, label, hint, setter);
    }

    @Override
    protected String parse(String input) {
        return input;
    }
}
