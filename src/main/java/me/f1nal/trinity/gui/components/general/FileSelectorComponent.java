package me.f1nal.trinity.gui.components.general;

import imgui.ImGui;
import imgui.type.ImString;
import me.f1nal.trinity.gui.components.ComponentId;
import me.f1nal.trinity.util.GuiUtil;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;

public class FileSelectorComponent {
    public static final FilenameFilter TDB_FILE_FILTER = (f, n) -> n.toLowerCase().endsWith(".tdb");

    private final String label;
    private final ImString path = new ImString(256);
    private String lastDirectory;
    private final FilenameFilter filenameFilter;
    private final int mode;
    private final String componentId = ComponentId.getId(this.getClass());

    public FileSelectorComponent(String label, String path, FilenameFilter filenameFilter, int mode) {
        this.label = label;
        this.path.set(path);
        this.filenameFilter = filenameFilter;
        this.mode = mode;
    }

    public void draw() {
        ImGui.text(this.label);
        ImGui.sameLine();
        if (ImGui.smallButton("...")) {
            File result = this.openFileChooser();
            if (result != null) this.path.set(result);
        }
        GuiUtil.tooltip("Open File Chooser");
        ImGui.inputText("###" + this.componentId, this.path);
    }

    public File getFile() {
        return new File(this.path.get());
    }

    public void setFile(File file) {
        this.path.set(file.getAbsolutePath());
    }

    public File openFileChooser() {
        FileDialog fd = new FileDialog((java.awt.Frame) null, "Choose a file", mode);
        fd.setDirectory(lastDirectory != null ? lastDirectory : getParentFromPath());
        fd.setFilenameFilter(this.filenameFilter);
        fd.setFile(path.get());
        fd.setVisible(true);
        if (fd.getDirectory() != null) this.lastDirectory = fd.getDirectory();
        if (fd.getFiles().length == 0) {
            return null;
        }
        File file = fd.getFiles()[0];
        if (mode != FileDialog.SAVE && !file.exists()) {
            return null;
        }
        File directory = file.getParentFile();
        if (directory.exists() && directory.isDirectory()) {
            lastDirectory = directory.getAbsolutePath();
        }
        return file;
    }

    private String getParentFromPath() {
        File file = new File(path.get());
        if (!file.isDirectory()) return file.getParentFile().getAbsolutePath();
        return file.getAbsolutePath();
    }
}
