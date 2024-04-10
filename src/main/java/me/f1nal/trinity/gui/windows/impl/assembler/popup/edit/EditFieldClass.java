package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.ClassSelectComponent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class EditFieldClass extends EditField<String> {
    private final ClassSelectComponent classSelectComponent;

    public EditFieldClass(Trinity trinity, String editFieldName, Supplier<String> getter, Consumer<String> setter) {
        super(getter, setter);
        this.classSelectComponent = new ClassSelectComponent(trinity, editFieldName, target -> target.getInput() != null);
    }

    @Override
    public void draw() {
        if(this.classSelectComponent.draw()) {
            set(classSelectComponent.getClassName());
        }
    }

    @Override
    public void updateField() {
        this.classSelectComponent.setClassName(get());
    }

    @Override
    public boolean isValidInput() {
        final var className = classSelectComponent.getClassName();
        return className != null && !className.isEmpty();
    }
}
