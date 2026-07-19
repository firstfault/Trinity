package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import java.util.function.Consumer;
import java.util.function.Supplier;

final class EditFieldAsmConstant extends EditFieldText<Object> {
    EditFieldAsmConstant(String label, Supplier<Object> getter, Consumer<Object> setter) {
        super(4096, label, "string(), type(), handle(), condy()", getter, setter);
    }

    @Override
    protected Object parse(String input) throws InvalidEditInputException {
        try {
            return AssemblerValueCodec.parse(input);
        } catch (IllegalArgumentException exception) {
            throw new InvalidEditInputException(exception.getMessage());
        }
    }

    @Override
    public void updateField() {
        text.set(AssemblerValueCodec.format(get()));
    }
}
