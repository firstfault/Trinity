package me.f1nal.trinity.gui.windows.impl;

import imgui.ImGui;
import imgui.type.ImBoolean;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.compile.ClassWriterTask;
import me.f1nal.trinity.execution.compile.Console;
import me.f1nal.trinity.execution.packages.ProjectContainer;
import me.f1nal.trinity.gui.components.general.FileSelectorComponent;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.util.NameUtil;

import java.awt.*;
import java.io.File;

public class ExportJarWindow extends ClosableWindow {
    private final ProjectContainer container;
    private final FileSelectorComponent outputFile;
    private final ImBoolean removeSignatures = new ImBoolean(true);
    private ClassWriterTask classWriterTask;
    private final Console console = new Console();
    private volatile float progress;

    public ExportJarWindow(Trinity trinity, ProjectContainer container) {
        super("Export " + container.getName(), 520, 400, trinity);
        if (!container.isJar()) throw new IllegalArgumentException("Cannot export a loose container as a JAR");
        this.container = container;
        this.setDialog(true);
        File databasePath = trinity.getDatabase().getPath();
        File parent = databasePath == null ? new File("").getAbsoluteFile() : databasePath.getAbsoluteFile().getParentFile();
        String archiveName = container.getName().replace('\\', '/');
        archiveName = archiveName.substring(archiveName.lastIndexOf('/') + 1);
        String baseName = NameUtil.removeExtensions(archiveName);
        if (baseName.isBlank()) baseName = "archive";
        this.outputFile = new FileSelectorComponent("Output File", new File(parent,
                baseName + "-out.jar").getAbsolutePath(),
                (dir, name) -> name.toLowerCase().endsWith(".jar"), FileDialog.SAVE);
    }

    @Override
    protected void renderFrame() {
        this.outputFile.draw();
        ImGui.checkbox("Remove invalid signatures", removeSignatures);
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip("Removes JAR signature files that no longer verify after bytecode changes.");
        }

        boolean disabled = classWriterTask != null;
        if (disabled) ImGui.beginDisabled();
        if (ImGui.button("Export JAR")) {
            console.clear();
            progress = 0.F;
            classWriterTask = new ClassWriterTask(container, trinity, console,
                    outputFile.getFile(), removeSignatures.get());
            classWriterTask.build(value -> progress = value, () -> classWriterTask = null);
        }
        if (disabled) ImGui.endDisabled();
        ImGui.sameLine();
        ImGui.progressBar(progress);

        ImGui.beginChild("ExportJarConsoleChild", 0.F, 0.F);
        console.draw();
        ImGui.endChild();
    }

    @Override
    public boolean isAlreadyOpen(ClosableWindow otherWindow) {
        return otherWindow instanceof ExportJarWindow other
                && other.container.getId().equals(container.getId());
    }
}
