package me.f1nal.trinity.gui.frames.impl.project.settings;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.tabs.ListBoxTabsComponent;
import me.f1nal.trinity.gui.frames.StaticWindow;
import me.f1nal.trinity.gui.frames.impl.project.settings.tabs.ProjectSettingsGeneral;
import me.f1nal.trinity.gui.frames.impl.project.settings.tabs.ProjectSettingsStatistics;

import java.util.List;

public class ProjectSettingsWindow extends StaticWindow {
    private final ListBoxTabsComponent<AbstractProjectSettingsTab> tabs;

    public ProjectSettingsWindow(Trinity trinity) {
        super("Project Settings", 300, 200, trinity);

        this.tabs = new ListBoxTabsComponent<>(List.of(
                new ProjectSettingsGeneral(trinity),
                new ProjectSettingsStatistics(trinity)
        ));
        this.windowFlags |= ImGuiWindowFlags.AlwaysAutoResize;
        this.windowFlags |= ImGuiWindowFlags.NoDocking;
    }

    @Override
    protected void renderFrame() {
        tabs.draw(150.F, 300.F);
        ImGui.sameLine();
        if (ImGui.beginChild("ProjectSettingsTab", 410.F, 300.F)) {
            tabs.getSelection().drawTabContent();
        }
        ImGui.endChild();
    }
}
