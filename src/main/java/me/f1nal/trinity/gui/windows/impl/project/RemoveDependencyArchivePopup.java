package me.f1nal.trinity.gui.windows.impl.project;

import imgui.ImGui;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.dependency.DependencyArchive;
import me.f1nal.trinity.gui.windows.api.PopupWindow;

public final class RemoveDependencyArchivePopup extends PopupWindow {
    private final DependencyArchive archive;

    public RemoveDependencyArchivePopup(Trinity trinity, DependencyArchive archive) {
        super("Remove Dependency", trinity);
        this.archive = archive;
    }

    @Override
    protected void renderFrame() {
        ImGui.textWrapped("Remove " + archive.getName()
                + " from this project's dependency classpath?");
        if (ImGui.button("Remove Dependency")) {
            trinity.getExecution().removeDependency(archive);
            close();
        }
        ImGui.sameLine();
        if (ImGui.button("Cancel")) close();
    }
}
