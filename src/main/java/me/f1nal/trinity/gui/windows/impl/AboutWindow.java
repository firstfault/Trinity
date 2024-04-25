package me.f1nal.trinity.gui.windows.impl;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.util.GuiUtil;
import me.f1nal.trinity.util.SystemUtil;

import java.awt.*;

public class AboutWindow extends StaticWindow {
    public AboutWindow(Trinity trinity) {
        super("About Trinity", 370, 100, trinity);
        this.windowFlags |= ImGuiWindowFlags.AlwaysAutoResize;
    }

    @Override
    protected void renderFrame() {
        ImGui.textWrapped(String.format("Software reverse engineering suite for Java applications developed by %s.\nYou are running version %s.", "final", Main.VERSION));
        if (ImGui.button(FontAwesomeIcons.Link + " GitHub Page")) {
            SystemUtil.browseURL("https://github.com/firstfault/Trinity");
        }
        GuiUtil.tooltip("Clicking will open a URL with your default browser");
    }
}
