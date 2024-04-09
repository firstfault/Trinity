package me.f1nal.trinity.gui.windows.impl.project.create.tabs;

import imgui.type.ImString;
import me.f1nal.trinity.database.compression.DatabaseCompressionType;
import me.f1nal.trinity.database.compression.DatabaseCompressionTypeManager;
import me.f1nal.trinity.gui.components.DescribableEnumComboBox;
import me.f1nal.trinity.gui.components.general.FileSelectorComponent;
import me.f1nal.trinity.gui.components.general.TextFieldComponent;
import me.f1nal.trinity.gui.windows.impl.CreateDatabasePopup;
import me.f1nal.trinity.gui.windows.impl.project.create.AbstractProjectCreationTab;
import me.f1nal.trinity.util.PatternUtil;
import me.f1nal.trinity.util.TextFieldPatternMatchCallback;

import java.awt.*;
import java.io.File;

public class ProjectCreationGeneral extends AbstractProjectCreationTab {
    private TextFieldComponent projectName;
    private DescribableEnumComboBox<DatabaseCompressionType> compressionTypeCombo;
    private FileSelectorComponent databasePath;

    public ProjectCreationGeneral() {
        this.projectName = new TextFieldComponent("Project Name", new ImString("", 0x50));
        this.compressionTypeCombo = new DescribableEnumComboBox<>("Database Compression", DatabaseCompressionTypeManager.getTypes().toArray(new DatabaseCompressionType[0]));
        this.databasePath = new FileSelectorComponent("Database Path", new File("").getAbsolutePath(), CreateDatabasePopup.TDB_FILE_FILTER, FileDialog.SAVE);

        this.projectName.setCallback(new TextFieldPatternMatchCallback(PatternUtil.DATABASE_NAME));
    }

    @Override
    public void drawTabContent() {
        this.projectName.draw();
        this.compressionTypeCombo.draw();
        this.databasePath.draw();
    }

    public TextFieldComponent getProjectName() {
        return projectName;
    }

    public DescribableEnumComboBox<DatabaseCompressionType> getCompressionTypeCombo() {
        return compressionTypeCombo;
    }

    public FileSelectorComponent getDatabasePath() {
        return databasePath;
    }

    @Override
    public String getName() {
        return "Project";
    }

    @Override
    public String getDescription() {
        return "Database and general project settings";
    }

    @Override
    public boolean isInputValid() {
        return !getProjectName().getText().isEmpty();
    }
}
