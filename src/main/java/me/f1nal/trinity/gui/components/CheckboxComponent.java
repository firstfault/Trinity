package me.f1nal.trinity.gui.components;

import imgui.ImGui;
import imgui.type.ImBoolean;

import java.awt.font.ImageGraphicAttribute;

public class CheckboxComponent {
    private final String id = ComponentId.getId(this.getClass());
    protected final ImBoolean state = new ImBoolean();
    private String label;

    public CheckboxComponent(String label) {
        this(label, false);
    }

    public CheckboxComponent(String label, boolean defaultValue) {
        this.label = label;
        this.state.set(defaultValue);
    }

    public void draw() {
        boolean checked = this.isChecked();
        ImGui.checkbox(this.label + "###" + this.id, state);
        if (this.isChecked() != checked) {
            this.setChecked(this.isChecked());
        }
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isChecked() {
        return state.get();
    }

    public void setChecked(boolean checked) {
        state.set(checked);
    }

    public void toggleChecked() {
        this.setChecked(!this.isChecked());
    }
}
