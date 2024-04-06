package me.f1nal.trinity.gui.frames.impl;

import imgui.ImGui;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.ClassPath;
import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.database.compression.DatabaseCompressionType;
import me.f1nal.trinity.database.compression.DatabaseCompressionTypeManager;
import me.f1nal.trinity.execution.exception.MissingEntryPointException;
import me.f1nal.trinity.gui.components.DescribableEnumComboBox;
import me.f1nal.trinity.gui.components.general.FileSelectorComponent;
import me.f1nal.trinity.gui.frames.Popup;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class CreateDatabasePopup extends Popup {
    public static final FilenameFilter TDB_FILE_FILTER = (f, n) -> n.toLowerCase().endsWith(".tdb");
    private File targetFile;
    private int resourceCount, classCount;
    private final FileSelectorComponent targetFileSelector = new FileSelectorComponent("Target File", new File("").getAbsolutePath(), (f, n) -> n.toLowerCase().endsWith(".jar"), FileDialog.LOAD);
    private FileSelectorComponent databaseFileSelector;
    private String loadError;
    private DescribableEnumComboBox<DatabaseCompressionType> compressionTypeCombo = new DescribableEnumComboBox<>("Database Compression", DatabaseCompressionTypeManager.getTypes().toArray(new DatabaseCompressionType[0]));

    public CreateDatabasePopup() {
        super("Create Database", null);
    }

    private String lastCheckedPath;

    private void setTargetFile(File file) throws IOException {
        if (file.getPath().equals(lastCheckedPath)) {
            return;
        }
        File targetFilePath = this.targetFileSelector.getFile();
        this.databaseFileSelector = new FileSelectorComponent("Database File", new File(targetFilePath.getParentFile(), targetFilePath.getName() + ".tdb").getAbsolutePath(), TDB_FILE_FILTER, FileDialog.SAVE);

        this.lastCheckedPath = file.getPath();

        int resourceCount = 0, classCount = 0;
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file));
        while (true) {
            ZipEntry nextEntry = zipInputStream.getNextEntry();
            if (nextEntry == null) break;
            if (nextEntry.getName().endsWith(".class")) {
                ++classCount;
            } else {
                ++resourceCount;
            }
            zipInputStream.closeEntry();
        }
        zipInputStream.close();
        this.resourceCount = resourceCount;
        this.classCount = classCount;
        this.targetFile = file;
    }

    @Override
    protected void renderFrame() {
        boolean tryLoad = false;
        this.targetFileSelector.draw();
        if (ImGui.button("Load")) {
            tryLoad = true;
        }
        File targetFile = this.targetFileSelector.getFile();
        if (tryLoad) {
            try {
                this.setTargetFile(targetFile);
            } catch (IOException exception) {
                loadError = exception.toString();
            }
        }
        if (loadError != null) {
            ImGui.text("Failed to load input");
            ImGui.text(loadError);
        }
        if (this.targetFile != null) {
            ImGui.separator();
            ImGui.text(this.targetFile.getName());
            ImGui.text(classCount + " classes, " + resourceCount + " resources");
            ImGui.separator();
            this.databaseFileSelector.draw();
            DatabaseCompressionType databaseCompressionType = this.compressionTypeCombo.draw();

            boolean disabled = (this.databaseFileSelector.getFile().isDirectory());
            if (disabled) ImGui.beginDisabled();
            if (ImGui.button("Create")) {
                this.close();
                try {
                    ClassPath classPath = new ClassPath(new JarInputStream(new FileInputStream(this.targetFile)));
                    Database database = new Database(this.targetFile.getName(), this.databaseFileSelector.getFile(), databaseCompressionType);
                    Trinity trinity = new Trinity(database, classPath);
                    Main.getDisplayManager().setDatabase(trinity);
                } catch (IOException | MissingEntryPointException e) {
                    throw new RuntimeException(e);
                }
            }
            if (disabled) ImGui.endDisabled();
            ImGui.sameLine();
            if (ImGui.button("Back")) {
                this.targetFile = null;
                this.lastCheckedPath = null;
            }
        }
    }
}
