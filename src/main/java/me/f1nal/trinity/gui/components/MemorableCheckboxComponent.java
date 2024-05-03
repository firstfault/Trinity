package me.f1nal.trinity.gui.components;

import imgui.ImGui;
import imgui.type.ImBoolean;
import me.f1nal.trinity.Main;

import java.util.Map;

public class MemorableCheckboxComponent extends CheckboxComponent {
    private final String identifier;

    public MemorableCheckboxComponent(String identifier, String label, boolean defaultValue) {
        super(label, defaultValue);
        this.identifier = identifier;

        Boolean memorized = getMemorizedCheckboxes().get(identifier);
        if (memorized != null) {
            this.setChecked(memorized);
        }
    }

    private static Map<String, Boolean> getMemorizedCheckboxes() {
        return Main.getPreferences().getMemorizedCheckboxes();
    }

    @Override
    public void draw() {
        super.draw();
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        getMemorizedCheckboxes().put(this.identifier, checked);
    }
}
