package me.f1nal.trinity.gui.components.general;

import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.type.ImString;
import me.f1nal.trinity.gui.components.ComponentId;
import me.f1nal.trinity.util.GuiUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

public class FileSelectorComponent {
    private static final int PATH_CAPACITY = 2048;
    public static final FilenameFilter TDB_FILE_FILTER = (f, n) -> n.toLowerCase().endsWith(".tdb");

    public enum Mode {
        OPEN,
        SAVE
    }

    private final String label;
    private final ImString path = new ImString(PATH_CAPACITY);
    private String lastDirectory;
    private final FilenameFilter filenameFilter;
    private final Mode mode;
    private final NativeFilePicker.Filter nativeFilter;
    private final String componentId = ComponentId.getId(this.getClass());

    public FileSelectorComponent(
            String label,
            String path,
            FilenameFilter filenameFilter,
            Mode mode,
            String... extensions
    ) {
        this.label = label;
        this.path.set(path);
        this.filenameFilter = filenameFilter;
        this.mode = mode;
        this.nativeFilter = extensions.length == 0
                ? null
                : new NativeFilePicker.Filter("Supported files", extensions);
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

    /**
     * Draws the path editor and file-picker action on one row.
     *
     * @return whether the selected path changed
     */
    public boolean drawInline(String browseLabel) {
        ImGui.text(this.label);

        ImGuiStyle style = ImGui.getStyle();
        float buttonWidth = ImGui.calcTextSize(browseLabel).x
                + style.getFramePadding().x * 2.F;
        float inputWidth = Math.max(80.F, ImGui.getContentRegionAvailX()
                - buttonWidth - style.getItemSpacing().x);
        ImGui.setNextItemWidth(inputWidth);
        boolean changed = ImGui.inputText("###" + this.componentId, this.path);
        ImGui.sameLine();
        if (ImGui.button(browseLabel + "###Browse" + this.componentId)) {
            File result = this.openFileChooser();
            if (result != null) {
                this.path.set(result);
                changed = true;
            }
        }
        GuiUtil.tooltip("Choose a file");
        return changed;
    }

    public String getPath() {
        return this.path.get();
    }

    public File getFile() {
        return new File(this.path.get());
    }

    public void setFile(File file) {
        this.path.set(file.getAbsolutePath());
    }

    public File openFileChooser() {
        File[] files = this.openFileChooserMultiple(false);
        return files.length == 0 ? null : files[0];
    }

    public File[] openFileChooserMultiple() {
        return this.openFileChooserMultiple(true);
    }

    private File[] openFileChooserMultiple(boolean multiple) {
        String initialDirectory = lastDirectory != null ? lastDirectory : getParentFromPath();
        File[] files;
        if (mode == Mode.SAVE) {
            File selected = NativeFilePicker.saveFile(initialDirectory, getFileNameFromPath(), nativeFilter);
            files = selected == null ? new File[0] : new File[]{selected};
        } else if (multiple) {
            files = NativeFilePicker.openFiles(initialDirectory, nativeFilter);
        } else {
            File selected = NativeFilePicker.openFile(initialDirectory, nativeFilter);
            files = selected == null ? new File[0] : new File[]{selected};
        }

        files = Arrays.stream(files)
                .filter(file -> mode == Mode.SAVE || file.exists())
                .filter(file -> mode == Mode.SAVE || filenameFilter == null
                        || filenameFilter.accept(file.getParentFile(), file.getName()))
                .toArray(File[]::new);
        if (files.length != 0) {
            File directory = files[0].getAbsoluteFile().getParentFile();
            if (directory != null && directory.isDirectory()) lastDirectory = directory.getAbsolutePath();
        }
        return files;
    }

    private String getParentFromPath() {
        File file = new File(path.get()).getAbsoluteFile();
        File directory = file.isDirectory() ? file : file.getParentFile();
        return directory == null ? null : directory.getAbsolutePath();
    }

    private String getFileNameFromPath() {
        File file = new File(path.get()).getAbsoluteFile();
        return file.isDirectory() ? null : file.getName();
    }
}
