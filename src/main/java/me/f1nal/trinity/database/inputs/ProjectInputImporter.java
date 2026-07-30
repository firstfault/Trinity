package me.f1nal.trinity.database.inputs;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.execution.loading.tasks.ClassInputReaderLoadTask;
import me.f1nal.trinity.execution.packages.ProjectContainerKind;
import me.f1nal.trinity.gui.components.general.NativeFilePicker;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.gui.viewport.notifications.SimpleCaption;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Shared active-project import path used by the File menu and OS drops. */
public final class ProjectInputImporter {
    private static final ProjectInputImportQueue IMPORT_QUEUE =
            new ProjectInputImportQueue("Trinity project input importer");

    private ProjectInputImporter() {
    }

    public static void chooseAndImport(Trinity trinity) {
        if (isBusy()) {
            notify(NotificationType.INFO, "Wait for the current project input import to finish.");
            return;
        }
        File[] files = NativeFilePicker.openFiles(null,
                new NativeFilePicker.Filter("Java archives and classes", "jar", "zip", "class"));
        if (files.length != 0) importFiles(trinity, Arrays.asList(files));
    }

    public static void importFiles(Trinity trinity, List<File> files) {
        if (!trinity.getExecution().getAsynchronousLoad().isFinished()) {
            notify(NotificationType.INFO, "Wait for the current project to finish loading before adding inputs.");
            return;
        }
        List<File> accepted = files.stream().filter(ProjectInputImporter::isSupported).toList();
        if (accepted.isEmpty()) {
            notify(NotificationType.WARNING, "No supported JAR, ZIP, or class files were selected.");
            return;
        }

        boolean queued = IMPORT_QUEUE.submit(() -> importFilesImpl(trinity, accepted));
        if (queued) {
            notify(NotificationType.INFO, "Queued project input import.");
        }
    }

    public static boolean isBusy() {
        return IMPORT_QUEUE.isBusy();
    }

    private static void importFilesImpl(Trinity trinity, List<File> accepted) {
        try {
            ProjectInputFileFactory factory = new ProjectInputFileFactory();
            List<AbstractProjectInputFile> parsed = new ArrayList<>();
            for (File file : accepted) {
                AbstractProjectInputFile input = factory.create(file);
                if (input != null) parsed.add(input);
                else notifyLater(NotificationType.ERROR, "Could not read " + file.getName() + ".");
            }
            if (parsed.isEmpty()) return;

            ProjectInputSet inputSet = new ProjectInputSet();
            for (AbstractProjectInputFile input : parsed) {
                if (input.getContainerKind() == ProjectContainerKind.JAR) {
                    inputSet.addJar(input.getName(), input.getClassPath());
                } else {
                    inputSet.addLoose(input.getClassPath());
                }
            }

            ClassInputReaderLoadTask task = ClassInputReaderLoadTask.forActiveImport(inputSet,
                    () -> Main.getTrinity() == trinity);
            task.setTrinity(trinity);
            task.runImpl();
            if (task.isInstallationCancelled()) {
                notifyLater(NotificationType.INFO,
                        "Project input import was cancelled because the active project changed.");
                return;
            }
            if (task.getInstalledContainerCount() == 0) return;
            notifyLater(NotificationType.SUCCESS, "Added " + task.getInstalledContainerCount() + " project input"
                    + (task.getInstalledContainerCount() == 1 ? "." : "s."));
        } catch (RuntimeException exception) {
            String message = exception.getMessage();
            notifyLater(NotificationType.ERROR, "Could not import project inputs"
                    + (message == null || message.isBlank() ? "." : ": " + message));
        }
    }

    private static boolean isSupported(File file) {
        String name = file.getName().toLowerCase();
        return file.isFile() && (name.endsWith(".jar") || name.endsWith(".zip") || name.endsWith(".class"));
    }

    private static void notify(NotificationType type, String message) {
        Main.getDisplayManager().addNotification(new Notification(type, new SimpleCaption("Project Input"),
                ColoredStringBuilder.create().fmt("{}", message).get()));
    }

    private static void notifyLater(NotificationType type, String message) {
        Main.runLater(() -> notify(type, message));
    }
}
