package me.f1nal.trinity.gui.components.general;

import imgui.ImGui;
import me.f1nal.trinity.gui.components.ComponentId;
import me.f1nal.trinity.util.SystemUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class LabeledTextList {
    private final Map<String, String> map;
    private final boolean copyButton;
    private final String id = ComponentId.getId(this.getClass());

    public LabeledTextList(boolean copyButton) {
        this.copyButton = copyButton;
        this.map = new LinkedHashMap<>();
    }

    public boolean isCopyButton() {
        return copyButton;
    }

    public void add(String key, String value) {
        this.map.put(key, value);
    }

    public void draw() {
        var ref = new Object() {
            int index = 0;
        };
        map.forEach((key, value) -> {
            ImGui.textDisabled(key);
            ImGui.sameLine();
            ImGui.text(value);

            if (isCopyButton()) {
                ImGui.sameLine();
                if (ImGui.smallButton("Copy###" + id + "Cpy" + ++ref.index)) {
                    SystemUtil.copyToClipboard(value);
                }
            }
        });
    }
}
