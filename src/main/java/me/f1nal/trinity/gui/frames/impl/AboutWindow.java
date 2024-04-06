package me.f1nal.trinity.gui.frames.impl;

import imgui.ImGui;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.gui.frames.StaticWindow;

import java.util.List;

public class AboutWindow extends StaticWindow {
    private final List<ColoredString> text;

    public AboutWindow(Trinity trinity) {
        super("About Trinity", 370, 100, trinity);
        text = null;
    }

    @Override
    protected void renderFrame() {
        ImGui.textWrapped(String.format("Software reverse engineering suite for Java applications developed by %s. You are running version %s.", "final", Main.VERSION));
    }
}
