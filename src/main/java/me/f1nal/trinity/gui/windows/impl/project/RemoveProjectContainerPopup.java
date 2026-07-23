package me.f1nal.trinity.gui.windows.impl.project;

import imgui.ImGui;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.packages.ProjectContainer;
import me.f1nal.trinity.gui.windows.api.PopupWindow;
import me.f1nal.trinity.gui.windows.impl.entryviewer.ArchiveEntryViewerWindow;

public final class RemoveProjectContainerPopup extends PopupWindow {
    private final ProjectContainer container;

    public RemoveProjectContainerPopup(Trinity trinity, ProjectContainer container) {
        super("Remove Archive", trinity);
        this.container = container;
    }

    @Override
    protected void renderFrame() {
        ImGui.textWrapped("Remove " + container.getName() + " and all of its classes and resources from this project?");
        if (ImGui.button("Remove Archive")) {
            Main.getWindowManager().closeAll(window -> window instanceof ArchiveEntryViewerWindow<?> viewer
                    && viewer.getArchiveEntry().getContainer() == container);
            trinity.getExecution().removeContainer(container);
            trinity.getExecution().refreshStructuralIndexes();
            close();
        }
        ImGui.sameLine();
        if (ImGui.button("Cancel")) close();
    }
}
