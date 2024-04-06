package me.f1nal.trinity.gui.frames.impl.annotation;

import imgui.type.ImString;

public class AnnotationKeyValue {
    private String key;
    private Object value;
    /**
     * Name editing text
     */
    private ImString editField;

    public AnnotationKeyValue(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public boolean isEditing() {
        return this.editField != null;
    }

    public ImString getEditField() {
        return editField;
    }

    public void setEditing() {
        this.editField = new ImString(this.getKey(), 256);
    }

    public void stopEditing() {
        this.setKey(this.editField.get());
        this.editField = null;
    }
}
