package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import java.util.function.Consumer;
import java.util.function.Supplier;

final class EditFieldAsmConstantList extends EditFieldText<Object[]> {
    EditFieldAsmConstantList(String label, Supplier<Object[]> getter, Consumer<Object[]> setter) {
        super(8192, label, "[int(), string(), handle()]", getter, setter);
    }

    @Override
    protected Object[] parse(String input) throws InvalidEditInputException {
        try {
            return AssemblerValueCodec.parseList(input);
        } catch (IllegalArgumentException exception) {
            throw new InvalidEditInputException(exception.getMessage());
        }
    }

    @Override
    public void updateField() {
        text.set(AssemblerValueCodec.formatList(get()));
    }
}
