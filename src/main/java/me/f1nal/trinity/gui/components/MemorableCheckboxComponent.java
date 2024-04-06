package me.f1nal.trinity.gui.components;

import imgui.ImGui;
import imgui.type.ImBoolean;
import me.f1nal.trinity.Main;

import java.util.Map;

public class MemorableCheckboxComponent {
    private final ImBoolean state;
    private final String identifier;

    public MemorableCheckboxComponent(String identifier, boolean defaultValue) {
        this.state = new ImBoolean(defaultValue);
        this.identifier = identifier;

        Boolean memorized = getMemorizedCheckboxes().get(identifier);
        if (memorized != null) {
            this.state.set(memorized);
        }
    }

    private static Map<String, Boolean> getMemorizedCheckboxes() {
        return Main.getPreferences().getMemorizedCheckboxes();
    }

    public void drawCheckbox(String label) {
        boolean state = getState();
        boolean newState = ImGui.checkbox(label, state);

        if (state != newState) {
            setState(newState);
        }
    }

    public void setState(boolean state) {
        this.state.set(state);
        getMemorizedCheckboxes().put(this.identifier, state);
    }

    public boolean getState() {
        return state.get();
    }

    public void toggleState() {
        this.setState(!this.getState());
    }
}
