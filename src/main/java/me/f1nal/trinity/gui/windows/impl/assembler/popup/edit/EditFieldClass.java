package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.ClassSelectComponent;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class EditFieldClass extends EditField<String> {
    private final ClassSelectComponent classSelectComponent;
    private final Trinity trinity;
    private final String label;

    public EditFieldClass(Trinity trinity, String editFieldName, Supplier<String> getter, Consumer<String> setter) {
        super(getter, setter);
        this.trinity = trinity;
        this.label = editFieldName;
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

    @Override
    public String getInlineLabel() {
        return label;
    }

    @Override
    protected String formatInlineValue() {
        return classSelectComponent.getClassName();
    }

    @Override
    public boolean applyInlineValue(String value) {
        this.classSelectComponent.setClassName(value);
        if (value == null || value.isBlank()) {
            this.update();
            return false;
        }
        this.set(value);
        return true;
    }

    @Override
    public List<String> getInlineSuggestions() {
        return this.trinity.getExecution().getClassTargetMap().keySet().stream().sorted().toList();
    }
}
