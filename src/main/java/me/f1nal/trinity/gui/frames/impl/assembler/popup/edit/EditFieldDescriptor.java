package me.f1nal.trinity.gui.frames.impl.assembler.popup.edit;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class EditFieldDescriptor extends EditFieldString {
    EditFieldDescriptor(Supplier<String> getter, Consumer<String> setter) {
        super(256, "Descriptor", "java.lang.Object (Internal Type)", getter, setter);
    }
}
