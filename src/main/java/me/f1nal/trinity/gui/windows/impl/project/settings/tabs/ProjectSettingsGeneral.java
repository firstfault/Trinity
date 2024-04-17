package me.f1nal.trinity.gui.windows.impl.project.settings.tabs;

import imgui.type.ImString;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.compression.DatabaseCompressionType;
import me.f1nal.trinity.database.compression.DatabaseCompressionTypeManager;
import me.f1nal.trinity.gui.components.DescribableEnumComboBox;
import me.f1nal.trinity.gui.components.general.FileSelectorComponent;
import me.f1nal.trinity.gui.components.general.TextFieldComponent;
import me.f1nal.trinity.gui.windows.impl.project.settings.AbstractProjectSettingsTab;

import java.awt.*;

public class ProjectSettingsGeneral extends AbstractProjectSettingsTab {
    private TextFieldComponent projectName;
    private DescribableEnumComboBox<DatabaseCompressionType> compressionTypeCombo;
    private FileSelectorComponent databasePath;

    public ProjectSettingsGeneral(Trinity trinity) {
        super(trinity);
        this.projectName = new TextFieldComponent("Project Name", new ImString(trinity.getDatabase().getName(), 0x50));
        this.compressionTypeCombo = new DescribableEnumComboBox<>("Database Compression", DatabaseCompressionTypeManager.getTypes().toArray(new DatabaseCompressionType[0]), getDatabase().getCompressionType());
        this.databasePath = new FileSelectorComponent("Database Path", getDatabase().getPath().getAbsolutePath(), FileSelectorComponent.TDB_FILE_FILTER, FileDialog.SAVE);
    }

    @Override
    public void drawTabContent() {
        if (this.projectName.draw()) {
            getDatabase().setName(this.projectName.getText());
        }

        DatabaseCompressionType databaseCompressionType = compressionTypeCombo.draw();
        getDatabase().setCompressionType(databaseCompressionType);

        this.databasePath.draw();
        getDatabase().setPath(this.databasePath.getFile());
    }

    @Override
    public String getName() {
        return "General";
    }
}
