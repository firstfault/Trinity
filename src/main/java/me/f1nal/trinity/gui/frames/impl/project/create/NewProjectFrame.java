package me.f1nal.trinity.gui.frames.impl.project.create;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.database.compression.DatabaseCompressionType;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.execution.exception.MissingEntryPointException;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.NextButtonEnum;
import me.f1nal.trinity.gui.components.tabs.ListBoxTabsComponent;
import me.f1nal.trinity.gui.frames.StaticWindow;
import me.f1nal.trinity.gui.frames.impl.project.create.tabs.ProjectCreationGeneral;
import me.f1nal.trinity.gui.frames.impl.project.create.tabs.ProjectCreationInput;
import me.f1nal.trinity.gui.viewport.notifications.ICaption;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.util.GuiUtil;
import me.f1nal.trinity.util.NameUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class NewProjectFrame extends StaticWindow implements ICaption {
    private final ListBoxTabsComponent<AbstractProjectCreationTab> tabs;
    private final ProjectCreationInput inputTab;
    private final ProjectCreationGeneral generalTab;

    public NewProjectFrame(Trinity trinity) {
        super("New Project", 0, 0, trinity);

        this.inputTab = new ProjectCreationInput(this);
        this.generalTab = new ProjectCreationGeneral();

        this.tabs = new ListBoxTabsComponent<>(List.of(
                inputTab,
                generalTab
        ));

        this.inputTab.getFileListComponent().setElementAddEvent(input -> {
            if (this.generalTab.isInputValid()) {
                return;
            }

            final File file = input.getFile();
            final String name = file.getName();

            final String databaseName = NameUtil.removeExtensions(name);

            this.generalTab.getProjectName().setText(databaseName);
            this.generalTab.getDatabasePath().setFile(new File(file.getParentFile(), String.format("%s.tdb", databaseName)));
        });

        this.windowFlags |= ImGuiWindowFlags.AlwaysAutoResize;
    }

    @Override
    protected void renderFrame() {
        this.tabs.draw(150.F, 300.F);
        ImGui.sameLine();
        ImGui.beginGroup();
        if (ImGui.beginChild("ProjectCreationTab", 410.F, 270.F)) {
            ImGui.textWrapped(this.tabs.getSelection().getDescription());
            ImGui.separator();
            this.tabs.getSelection().drawTabContent();
        }
        ImGui.endChild();
        if (ImGui.beginChild("ProjectCreationControlsChild", 0.F, 0.F)) {
            ImGui.separator();
            if (GuiUtil.disabledWidget(!this.isInputsValid(), () -> ImGui.button(FontAwesomeIcons.Plus + " Create Project"))) {
                this.createProject();
            }
            Arrays.stream(NextButtonEnum.values()).forEach(nextButton -> {
                ImGui.sameLine();
                this.tabs.setSelection(nextButton.draw(this.tabs.getSelection(), this.tabs.getElementList()));
            });
        }
        ImGui.endChild();
        ImGui.endGroup();
    }

    private void createProject() {
        final String projectName = this.generalTab.getProjectName().getText();
        final File databaseFile = this.generalTab.getDatabasePath().getFile();
        final DatabaseCompressionType compressionType = this.generalTab.getCompressionTypeCombo().getSelected();
        final Database database = new Database(projectName, databaseFile, compressionType);
        final Trinity trinity;
        try {
            trinity = new Trinity(database, this.inputTab.createClassPath());
        } catch (IOException | MissingEntryPointException e) {
            Main.getDisplayManager().addNotification(new Notification(NotificationType.ERROR, this, ColoredStringBuilder.create()
                    .fmt("Failed to load new Trinity: {}", e).get()));
            return;
        }
        Main.getDisplayManager().closeDatabase(() -> Main.getDisplayManager().setDatabase(trinity));
    }

    private boolean isInputsValid() {
        for (AbstractProjectCreationTab tab : tabs.getElementList()) {
            if (!tab.isInputValid()) {
                return false;
            }
        }
        return !tabs.getElementList().isEmpty();
    }

    @Override
    public String getCaption() {
        return "New Project";
    }
}
