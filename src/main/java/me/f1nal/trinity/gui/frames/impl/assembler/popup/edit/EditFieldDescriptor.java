package me.f1nal.trinity.gui.frames.impl.assembler.popup.edit;

import java.util.function.Consumer;

public class EditFieldDescriptor extends EditFieldString {
    EditFieldDescriptor(Consumer<String> setter) {
        super(256, "Descriptor", "java.lang.Object (Internal Type)", setter);
    }
}
