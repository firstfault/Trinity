package me.f1nal.trinity.gui.windows.impl;

import imgui.ImGui;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.windows.api.PopupWindow;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;

final class ExportJarOverwritePopup extends PopupWindow {
    private final File outputFile;
    private final Consumer<File> confirm;

    ExportJarOverwritePopup(Trinity trinity, File outputFile, Consumer<File> confirm) {
        super("Replace existing JAR?", trinity);
        this.outputFile = outputFile.getAbsoluteFile();
        this.confirm = Objects.requireNonNull(confirm, "confirm");
    }

    @Override
    protected void renderFrame() {
        ImGui.textColored(CodeColorScheme.NOTIFY_WARN,
                FontAwesomeIcons.ExclamationTriangle + " The destination already exists.");
        ImGui.spacing();
        ImGui.textWrapped(outputFile.getAbsolutePath());
        ImGui.spacing();
        ImGui.textWrapped("Trinity writes a complete temporary archive before replacing this file.");
        ImGui.spacing();

        if (ImGui.button("Replace File")) {
            confirm.accept(outputFile);
            close();
        }
        ImGui.sameLine();
        if (ImGui.button("Cancel")) close();
    }
}
