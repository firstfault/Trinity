package me.f1nal.trinity.gui.frames.impl.ldc;

import me.f1nal.trinity.gui.frames.impl.assembler.popup.edit.EditField;

import java.util.ArrayList;
import java.util.List;

public abstract class CstType<T> {
    private final String label;
    private final List<EditField<?>> editFields = new ArrayList<>();
    private T value;
    private boolean valid;

    public CstType(String label) {
        this.label = label;
    }

    public List<EditField<?>> getEditFields() {
        return editFields;
    }

    public void draw() {
        for (EditField<?> editField : editFields) {
            editField.draw();
        }
    }

    protected final void setValue(T value) {
        this.value = value;
    }

    public final T getValue() {
        return value;
    }

    public final boolean isValid() {
        return valid;
    }

    public final String getLabel() {
        return label;
    }

    protected void addField(EditField<?> editField) {
        this.editFields.add(editField);
        editField.setUpdateEvent(this::updateValidity);
    }

    private void updateValidity() {
        for (EditField<?> editField : editFields) {
            if (!editField.isValidInput()) {
                this.valid = false;
                return;
            }
        }
        this.valid = true;
    }
}
