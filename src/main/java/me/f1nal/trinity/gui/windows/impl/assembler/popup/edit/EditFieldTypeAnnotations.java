package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import me.f1nal.trinity.gui.windows.impl.bytecode.InstructionTypeAnnotationEditor;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class EditFieldTypeAnnotations extends EditField<List<TypeAnnotationNode>> {
    private final InstructionTypeAnnotationEditor editor;

    EditFieldTypeAnnotations(String label, Supplier<List<TypeAnnotationNode>> getter,
                             Consumer<List<TypeAnnotationNode>> setter) {
        super(getter, setter);
        this.editor = new InstructionTypeAnnotationEditor(label, getter.get());
    }

    @Override
    public void draw() {
        editor.draw();
        try {
            set(editor.get());
        } catch (RuntimeException ignored) {
            update();
        }
    }

    @Override
    public void updateField() {
    }

    @Override
    public boolean isValidInput() {
        try {
            editor.get();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
