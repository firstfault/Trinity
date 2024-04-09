package me.f1nal.trinity.gui.windows.impl;

import imgui.ImGui;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.windows.api.StaticWindow;

public class AboutWindow extends StaticWindow {
    public AboutWindow(Trinity trinity) {
        super("About Trinity", 370, 100, trinity);
    }

    @Override
    protected void renderFrame() {
        ImGui.textWrapped(String.format("Software reverse engineering suite for Java applications developed by %s. You are running version %s.", "final", Main.VERSION));
    }
}
