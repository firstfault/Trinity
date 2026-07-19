package me.f1nal.trinity.gui.components;

import imgui.ImFont;
import imgui.ImGui;
import me.f1nal.trinity.Main;

public enum IconFamily {
    DEFAULT,
    CODICON;

    public void pushFont() {
        if (this != CODICON) return;
        ImFont font = Main.getDisplayManager().getFontManager().getCodiconFont();
        if (font != null) ImGui.pushFont(font, Main.getPreferences().getDefaultFont().getSize());
    }

    public void popFont() {
        if (this == CODICON && Main.getDisplayManager().getFontManager().getCodiconFont() != null) {
            ImGui.popFont();
        }
    }
}
