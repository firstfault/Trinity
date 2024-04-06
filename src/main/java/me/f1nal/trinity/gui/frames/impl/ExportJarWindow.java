package me.f1nal.trinity.gui.frames.impl;

import imgui.ImGui;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.compile.ClassWriterTask;
import me.f1nal.trinity.compile.Console;
import me.f1nal.trinity.gui.components.general.FileSelectorComponent;
import me.f1nal.trinity.gui.frames.StaticWindow;
import me.f1nal.trinity.util.NameUtil;

import java.awt.*;
import java.io.File;

public class ExportJarWindow extends StaticWindow {
    private final FileSelectorComponent outputFile;
    private ClassWriterTask classWriterTask;
    private Console console = new Console();
    private float progress;

    public ExportJarWindow(Trinity trinity) {
        super("Export JAR", 500, 400, trinity);
        File path = trinity.getDatabase().getPath();
        this.outputFile = new FileSelectorComponent("Output File", new File(path.getParent(),
                NameUtil.removeExtensions(trinity.getDatabase().getName()) + "-out.jar").getAbsolutePath(),
                (dir, name) -> name.toLowerCase().endsWith(".jar"), FileDialog.SAVE);
    }

    @Override
    protected void renderFrame() {
        this.outputFile.draw();
        boolean disabled = this.classWriterTask != null;
        if (disabled) ImGui.beginDisabled();
        if (ImGui.button("Export")) {
            this.console.clear();
            this.classWriterTask = new ClassWriterTask(trinity.getExecution().getClassList(), trinity.getExecution().getResourceMap(), trinity, console, this.outputFile.getFile());
            this.classWriterTask.build(progress -> this.progress = progress, () -> classWriterTask = null);
        }
        if (disabled) ImGui.endDisabled();
        ImGui.sameLine();
        ImGui.progressBar(this.progress);

        ImGui.beginChild("ExportJarConsoleChld", 0, 0);
        console.draw();
        ImGui.endChild();
    }
}
