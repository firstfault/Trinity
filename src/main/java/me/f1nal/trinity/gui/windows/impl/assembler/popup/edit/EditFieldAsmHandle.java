package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import org.objectweb.asm.Handle;

import java.util.function.Consumer;
import java.util.function.Supplier;

final class EditFieldAsmHandle extends EditFieldText<Handle> {
    EditFieldAsmHandle(String label, Supplier<Handle> getter, Consumer<Handle> setter) {
        super(2048, label, "handle(H_INVOKESTATIC, \"owner\", \"name\", \"desc\", false)", getter, setter);
    }

    @Override
    protected Handle parse(String input) throws InvalidEditInputException {
        try {
            return AssemblerValueCodec.parseHandle(input);
        } catch (IllegalArgumentException exception) {
            throw new InvalidEditInputException(exception.getMessage());
        }
    }

    @Override
    public void updateField() {
        text.set(AssemblerValueCodec.format(get()));
    }
}
