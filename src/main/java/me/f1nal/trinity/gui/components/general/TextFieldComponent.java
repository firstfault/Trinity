package me.f1nal.trinity.gui.components.general;

import imgui.ImGui;
import imgui.callback.ImGuiInputTextCallback;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;
import me.f1nal.trinity.gui.components.ComponentId;

public class TextFieldComponent {
    private final ImString text;
    private final String id = ComponentId.getId(getClass());
    private final String label;
    private ImGuiInputTextCallback callback;

    public TextFieldComponent(String label, ImString text) {
        this.text = text;
        this.label = label;
    }

    public void setCallback(ImGuiInputTextCallback callback) {
        this.callback = callback;
    }

    public boolean draw() {
        ImGui.text(this.label);
        return ImGui.inputText("###" + id, this.text, callback != null ? ImGuiInputTextFlags.CallbackCharFilter : ImGuiInputTextFlags.None, this.callback);
    }

    public String getText() {
        return text.get();
    }

    public void setText(String text) {
        this.text.set(text);
    }
}
