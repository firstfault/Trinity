package me.f1nal.trinity.database.inputs;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.execution.loading.tasks.ClassInputReaderLoadTask;
import me.f1nal.trinity.execution.packages.ProjectContainerKind;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.gui.viewport.notifications.SimpleCaption;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Shared active-project import path used by the File menu and OS drops. */
public final class ProjectInputImporter {
    private ProjectInputImporter() {
    }

    public static void chooseAndImport(Trinity trinity) {
        FileDialog dialog = new FileDialog((Frame) null, "Add project inputs", FileDialog.LOAD);
        dialog.setMultipleMode(true);
        dialog.setFilenameFilter((dir, name) -> isSupported(new File(dir, name)));
        dialog.setVisible(true);
        if (dialog.getFiles().length != 0) importFiles(trinity, Arrays.asList(dialog.getFiles()));
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

        Thread worker = new Thread(() -> {
            ProjectInputFileFactory factory = new ProjectInputFileFactory();
            List<AbstractProjectInputFile> parsed = new ArrayList<>();
            for (File file : accepted) {
                AbstractProjectInputFile input = factory.create(file);
                if (input != null) parsed.add(input);
                else notify(NotificationType.ERROR, "Could not read " + file.getName() + ".");
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

            ClassInputReaderLoadTask task = new ClassInputReaderLoadTask(inputSet);
            task.setTrinity(trinity);
            task.runImpl();
            if (task.getInstalledContainerCount() == 0) return;
            trinity.getExecution().refreshStructuralIndexes();
            Main.runLater(() -> {
                trinity.getEventManager().postEvent(new EventClassesLoaded());
                notify(NotificationType.SUCCESS, "Added " + task.getInstalledContainerCount() + " project input"
                        + (task.getInstalledContainerCount() == 1 ? "." : "s."));
            });
        }, "Trinity project input importer");
        worker.setDaemon(true);
        worker.start();
    }

    private static boolean isSupported(File file) {
        String name = file.getName().toLowerCase();
        return file.isFile() && (name.endsWith(".jar") || name.endsWith(".zip") || name.endsWith(".class"));
    }

    private static void notify(NotificationType type, String message) {
        Main.getDisplayManager().addNotification(new Notification(type, new SimpleCaption("Project Input"),
                ColoredStringBuilder.create().fmt("{}", message).get()));
    }
}
