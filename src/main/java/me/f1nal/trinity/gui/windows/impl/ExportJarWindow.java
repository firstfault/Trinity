package me.f1nal.trinity.gui.windows.impl;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.compile.ClassWriterTask;
import me.f1nal.trinity.execution.compile.Console;
import me.f1nal.trinity.execution.dependency.DependencyArchive;
import me.f1nal.trinity.execution.packages.ProjectContainer;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.general.FileSelectorComponent;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.ByteUtil;
import me.f1nal.trinity.util.GuiUtil;
import me.f1nal.trinity.util.NameUtil;
import me.f1nal.trinity.util.SystemUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExportJarWindow extends ClosableWindow {
    private final ProjectContainer container;
    private final FileSelectorComponent outputFile;
    private final ImBoolean removeSignatures = new ImBoolean(true);
    private final ImBoolean ignoreUnresolvedDependencies = new ImBoolean(false);
    private final Console console = new Console();
    private ClassWriterTask classWriterTask;
    private ClassWriterTask.ExportResult lastResult;
    private ExportState state = ExportState.READY;
    private volatile float progress;

    public ExportJarWindow(Trinity trinity, ProjectContainer container) {
        super("Export " + container.getName(), 680, 590, trinity);
        if (!container.isJar()) throw new IllegalArgumentException("Cannot export a loose container as a JAR");
        this.container = container;
        this.setDialog(true);
        this.windowFlags |= ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize;

        File databasePath = trinity.getDatabase().getPath();
        File parent = databasePath == null ? new File("").getAbsoluteFile()
                : databasePath.getAbsoluteFile().getParentFile();
        String archiveName = container.getName().replace('\\', '/');
        archiveName = archiveName.substring(archiveName.lastIndexOf('/') + 1);
        String baseName = NameUtil.removeExtensions(archiveName);
        if (baseName.isBlank()) baseName = "archive";
        this.outputFile = new FileSelectorComponent("Output JAR", new File(parent,
                baseName + "-out.jar").getAbsolutePath(),
                (dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".jar"),
                FileSelectorComponent.Mode.SAVE, "jar");
    }

    @Override
    protected void renderFrame() {
        drawArchiveSummary();

        boolean running = state == ExportState.EXPORTING;
        if (running) ImGui.beginDisabled();

        ImGui.separatorText("Destination");
        outputFile.drawInline(FontAwesomeIcons.FolderOpen + " Browse");
        OutputValidation validation = validateOutputPath(outputFile.getPath());
        drawOutputValidation(validation);

        ImGui.separatorText("Export options");
        drawExportOptions();

        if (running) ImGui.endDisabled();

        drawDependencyHealth();

        ImGui.separatorText("Export");
        drawExportStatus(validation);
        drawExportActions(validation);

        ImGui.separatorText("Export activity");
        drawActivityLog(running);
    }

    private void drawArchiveSummary() {
        ImGui.textColored(CodeColorScheme.ARCHIVE_REF, FontAwesomeIcons.FileArchive);
        ImGui.sameLine();
        ImGui.textWrapped(NameUtil.cleanNewlines(container.getName()));

        int classCount = container.getClasses().size();
        int resourceCount = container.getResources().size();
        long modifiedCount = container.getClasses().stream()
                .filter(target -> target.getInput() != null && target.getInput().isRebuildRequired())
                .count();
        List<String> summary = new ArrayList<>();
        summary.add(countLabel(classCount, "class", "classes"));
        summary.add(countLabel(resourceCount, "resource", "resources"));
        if (modifiedCount != 0) {
            summary.add(countLabel(modifiedCount, "modified class", "modified classes"));
        }

        List<DependencyArchive> dependencies =
                trinity.getExecution().getDependencies().getArchives();
        summary.add(countLabel(dependencies.size(), "dependency", "dependencies"));
        ImGui.textColored(CodeColorScheme.DISABLED, String.join("  |  ", summary));
    }

    private void drawOutputValidation(OutputValidation validation) {
        int color;
        String icon;
        if (!validation.valid()) {
            color = CodeColorScheme.NOTIFY_ERROR;
            icon = FontAwesomeIcons.TimesCircle;
        } else if (validation.overwriteRequired()) {
            color = CodeColorScheme.NOTIFY_WARN;
            icon = FontAwesomeIcons.ExclamationTriangle;
        } else {
            color = CodeColorScheme.NOTIFY_SUCCESS;
            icon = FontAwesomeIcons.CheckCircle;
        }
        ImGui.textColored(color, icon + " " + validation.message());
    }

    private void drawExportOptions() {
        ImGui.checkbox("Remove invalid signatures", removeSignatures);
        GuiUtil.tooltip("Recommended whenever classes or resources have changed.");
        drawOptionDescription("Removes META-INF signature files that no longer verify after the JAR changes.");

        ImGui.checkbox("Ignore Unresolved Dependencies", ignoreUnresolvedDependencies);
        GuiUtil.tooltip("Controls reporting only; export is always allowed to continue.");
        String description = ignoreUnresolvedDependencies.get()
                ? "Suppress missing-class warnings and use safe java/lang/Object frame fallbacks."
                : "List every missing class in Export activity. The JAR still exports with warnings.";
        drawOptionDescription(description);
    }

    private static void drawOptionDescription(String description) {
        ImGui.pushStyleColor(ImGuiCol.Text, CodeColorScheme.DISABLED);
        ImGui.textWrapped(description);
        ImGui.popStyleColor();
    }

    private void drawDependencyHealth() {
        List<DependencyArchive> dependencies =
                trinity.getExecution().getDependencies().getArchives();
        long unavailable = dependencies.stream().filter(dependency -> !dependency.isResolved()).count();
        if (unavailable == 0) return;

        ImGui.spacing();
        String message = countLabel(unavailable, "dependency archive is",
                "dependency archives are") + " unavailable. "
                + (ignoreUnresolvedDependencies.get()
                ? "Missing-class warnings are disabled for this export."
                : "Missing referenced classes will be reported during export.");
        ImGui.pushStyleColor(ImGuiCol.Text, CodeColorScheme.NOTIFY_WARN);
        ImGui.textWrapped(FontAwesomeIcons.ExclamationTriangle + " " + message);
        ImGui.popStyleColor();
    }

    private void drawExportStatus(OutputValidation validation) {
        switch (state) {
            case READY -> {
                String message = validation.valid()
                        ? FontAwesomeIcons.FileExport + " Ready to export"
                        : FontAwesomeIcons.ExclamationCircle
                        + " Resolve the destination issue before exporting";
                ImGui.textColored(CodeColorScheme.DISABLED, message);
            }
            case EXPORTING -> {
                ImGui.textColored(CodeColorScheme.NOTIFY_INFORMATION,
                        FontAwesomeIcons.Cog + " Building " + outputName() + "...");
                int percent = Math.round(Math.max(0.F, Math.min(1.F, progress)) * 100.F);
                ImGui.progressBar(progress, -1.F, 0.F, percent + "%");
            }
            case SUCCEEDED -> ImGui.textColored(CodeColorScheme.NOTIFY_SUCCESS,
                    FontAwesomeIcons.CheckCircle + " " + successSummary());
            case SUCCEEDED_WITH_WARNINGS -> ImGui.textColored(CodeColorScheme.NOTIFY_WARN,
                    FontAwesomeIcons.ExclamationTriangle + " " + successSummary()
                            + " Output may contain verification errors.");
            case FAILED -> {
                String failure = lastResult == null ? "Unknown export failure"
                        : describeFailure(lastResult.failure());
                ImGui.pushStyleColor(ImGuiCol.Text, CodeColorScheme.NOTIFY_ERROR);
                ImGui.textWrapped(FontAwesomeIcons.TimesCircle + " Export failed: " + failure);
                ImGui.popStyleColor();
            }
        }
    }

    private String successSummary() {
        if (lastResult == null) return "Export complete.";
        String summary = "Exported " + countLabel(lastResult.entryCount(), "entry", "entries")
                + " (" + ByteUtil.getHumanReadableByteCountSI(lastResult.outputSize()) + ").";
        if (state == ExportState.SUCCEEDED_WITH_WARNINGS) {
            summary += " " + countLabel(lastResult.unresolvedDependencyCount(),
                    "unresolved dependency", "unresolved dependencies") + ".";
        }
        return summary;
    }

    private void drawExportActions(OutputValidation validation) {
        boolean running = state == ExportState.EXPORTING;
        if (running || !validation.valid()) ImGui.beginDisabled();
        String exportLabel = switch (state) {
            case SUCCEEDED, SUCCEEDED_WITH_WARNINGS -> FontAwesomeIcons.RedoAlt + " Export Again";
            case FAILED -> FontAwesomeIcons.RedoAlt + " Retry Export";
            default -> FontAwesomeIcons.FileExport + " Export JAR";
        };
        if (ImGui.button(exportLabel)) requestExport(validation);
        if (running || !validation.valid()) ImGui.endDisabled();

        ImGui.sameLine();
        File actionFile = lastResult != null && lastResult.isSuccessful()
                ? lastResult.outputFile() : validation.file();
        File actionDirectory = actionFile == null ? null : actionFile.getAbsoluteFile().getParentFile();
        boolean canOpenDirectory = actionDirectory != null && actionDirectory.isDirectory();
        if (!canOpenDirectory) ImGui.beginDisabled();
        if (ImGui.button(FontAwesomeIcons.FolderOpen + " Open Folder")) {
            if (!SystemUtil.openDirectory(actionDirectory)) {
                console.warn("Unable to open output folder: {}",
                        actionDirectory == null ? "(unknown)" : actionDirectory.getAbsolutePath());
            }
        }
        if (!canOpenDirectory) ImGui.endDisabled();

        ImGui.sameLine();
        if (validation.file() == null) ImGui.beginDisabled();
        if (ImGui.button(FontAwesomeIcons.Copy + " Copy Path")) {
            SystemUtil.copyToClipboard(validation.file().getAbsolutePath());
        }
        if (validation.file() == null) ImGui.endDisabled();
    }

    private void drawActivityLog(boolean running) {
        boolean empty = console.isEmpty();
        if (empty) ImGui.beginDisabled();
        if (ImGui.button(FontAwesomeIcons.Copy + " Copy Log")) {
            SystemUtil.copyToClipboard(console.getPlainText());
        }
        if (empty) ImGui.endDisabled();

        ImGui.sameLine();
        if (empty || running) ImGui.beginDisabled();
        if (ImGui.button(FontAwesomeIcons.TrashAlt + " Clear")) console.clear();
        if (empty || running) ImGui.endDisabled();

        if (ImGui.beginChild(getId("ExportJarConsole"), 0.F, 0.F, true,
                ImGuiWindowFlags.HorizontalScrollbar)) {
            if (console.isEmpty()) {
                ImGui.textColored(CodeColorScheme.DISABLED,
                        "Warnings, errors, and completion details will appear here.");
            } else {
                console.draw();
            }
        }
        ImGui.endChild();
    }

    private void requestExport(OutputValidation validation) {
        if (!validation.valid() || state == ExportState.EXPORTING) return;
        if (validation.overwriteRequired()) {
            Main.getWindowManager().addPopup(new ExportJarOverwritePopup(
                    trinity, validation.file(), this::startExport));
            return;
        }
        startExport(validation.file());
    }

    private void startExport(File destination) {
        if (classWriterTask != null) return;

        console.clear();
        progress = 0.F;
        state = ExportState.EXPORTING;
        lastResult = null;
        boolean ignoredUnresolved = ignoreUnresolvedDependencies.get();
        classWriterTask = new ClassWriterTask(container, trinity, console,
                destination, removeSignatures.get(), ignoredUnresolved);
        classWriterTask.build(value -> progress = value, result -> {
            lastResult = result;
            progress = result.isSuccessful() ? 1.F : progress;
            if (!result.isSuccessful()) {
                state = ExportState.FAILED;
            } else if (result.unresolvedDependencyCount() != 0 && !ignoredUnresolved) {
                state = ExportState.SUCCEEDED_WITH_WARNINGS;
            } else {
                state = ExportState.SUCCEEDED;
            }
            classWriterTask = null;
        });
    }

    private String outputName() {
        File selected = outputFile.getFile();
        return selected.getName().isBlank() ? "JAR" : selected.getName();
    }

    static OutputValidation validateOutputPath(String path) {
        if (path == null || path.isBlank()) {
            return OutputValidation.invalid(null, "Choose an output JAR.");
        }

        File output = new File(path).getAbsoluteFile();
        if (!output.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) {
            return OutputValidation.invalid(output, "Destination must end in .jar.");
        }

        try {
            if (output.exists()) {
                if (!output.isFile()) {
                    return OutputValidation.invalid(output,
                            "Choose a file path, not a directory.");
                }
                if (!output.canWrite()) {
                    return OutputValidation.invalid(output,
                            "The existing output file is not writable.");
                }
                return OutputValidation.overwrite(output,
                        "This file already exists. You will be asked before it is replaced.");
            }

            File ancestor = output.getParentFile();
            while (ancestor != null && !ancestor.exists()) ancestor = ancestor.getParentFile();
            if (ancestor == null || !ancestor.isDirectory()) {
                return OutputValidation.invalid(output,
                        "The destination parent is not a directory.");
            }
            if (!ancestor.canWrite()) {
                return OutputValidation.invalid(output,
                        "The destination directory is not writable.");
            }
            return OutputValidation.valid(output, "Ready to create " + output.getName() + ".");
        } catch (SecurityException exception) {
            return OutputValidation.invalid(output,
                    "Trinity cannot access this destination.");
        }
    }

    private static String countLabel(long count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }

    private static String describeFailure(Throwable throwable) {
        if (throwable == null) return "Unknown export failure";
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName() : message;
    }

    @Override
    public boolean isAlreadyOpen(ClosableWindow otherWindow) {
        return otherWindow instanceof ExportJarWindow other
                && other.container.getId().equals(container.getId());
    }

    enum ExportState {
        READY,
        EXPORTING,
        SUCCEEDED,
        SUCCEEDED_WITH_WARNINGS,
        FAILED
    }

    record OutputValidation(File file, boolean valid, boolean overwriteRequired, String message) {
        private static OutputValidation invalid(File file, String message) {
            return new OutputValidation(file, false, false, message);
        }

        private static OutputValidation valid(File file, String message) {
            return new OutputValidation(file, true, false, message);
        }

        private static OutputValidation overwrite(File file, String message) {
            return new OutputValidation(file, true, true, message);
        }
    }
}
