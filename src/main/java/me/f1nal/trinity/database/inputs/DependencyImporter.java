package me.f1nal.trinity.database.inputs;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.events.EventDependenciesChanged;
import me.f1nal.trinity.execution.dependency.DependencyArchive;
import me.f1nal.trinity.execution.dependency.DependencyArchiveReader;
import me.f1nal.trinity.gui.components.general.NativeFilePicker;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.gui.viewport.notifications.SimpleCaption;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Imports classpath archives into the active project database. */
public final class DependencyImporter {
    private static final AtomicBoolean importing = new AtomicBoolean();

    private DependencyImporter() {
    }

    public static void chooseAndImport(Trinity trinity) {
        File[] files = NativeFilePicker.openFiles(null,
                new NativeFilePicker.Filter("Dependency archives", "jar", "zip", "jmod"));
        if (files.length != 0) importFiles(trinity, Arrays.asList(files));
    }

    public static void importFiles(Trinity trinity, List<File> files) {
        if (!trinity.getExecution().getAsynchronousLoad().isFinished()) {
            notify(NotificationType.INFO, "Wait for the current project to finish loading before adding dependencies.");
            return;
        }
        List<File> accepted = files.stream().filter(DependencyImporter::isSupported).toList();
        if (accepted.isEmpty()) {
            notify(NotificationType.WARNING, "No supported JAR, ZIP, or JMOD archives were selected.");
            return;
        }
        if (!importing.compareAndSet(false, true)) {
            notify(NotificationType.INFO, "Another dependency import is already running.");
            return;
        }

        Thread worker = new Thread(() -> {
            try {
                List<DependencyArchive> archives = new ArrayList<>();
                for (File file : accepted) {
                    try {
                        archives.add(DependencyArchiveReader.read(
                                file, trinity.getDatabase().getPath()));
                    } catch (Throwable throwable) {
                        notify(NotificationType.ERROR, "Could not add " + file.getName() + ": "
                                + message(throwable));
                    }
                }
                if (archives.isEmpty()) return;
                Main.runLater(() -> {
                    if (Main.getTrinity() != trinity) return;
                    archives.forEach(trinity.getExecution().getDependencies()::addArchive);
                    trinity.getEventManager().postEvent(new EventDependenciesChanged());
                    notify(NotificationType.SUCCESS, "Added " + archives.size() + " dependency archive"
                            + (archives.size() == 1 ? "." : "s."));
                });
            } finally {
                importing.set(false);
            }
        }, "Trinity dependency importer");
        worker.setDaemon(true);
        worker.start();
    }

    public static void restoreJavaBase(Trinity trinity) {
        if (!importing.compareAndSet(false, true)) {
            notify(NotificationType.INFO, "Another dependency import is already running.");
            return;
        }
        Thread worker = new Thread(() -> {
            try {
                DependencyArchive archive = DependencyArchiveReader.readRuntimeJavaBase();
                Main.runLater(() -> {
                    if (Main.getTrinity() != trinity) return;
                    trinity.getExecution().getDependencies().addArchive(archive);
                    trinity.getEventManager().postEvent(new EventDependenciesChanged());
                    notify(NotificationType.SUCCESS, "Restored java.base from the running JDK.");
                });
            } catch (Throwable throwable) {
                notify(NotificationType.ERROR, "Could not restore java.base: " + message(throwable));
            } finally {
                importing.set(false);
            }
        }, "Trinity java.base importer");
        worker.setDaemon(true);
        worker.start();
    }

    public static void rebind(Trinity trinity, DependencyArchive previous) {
        File file = NativeFilePicker.openFile(null,
                new NativeFilePicker.Filter("Dependency archives", "jar", "zip", "jmod"));
        if (file == null) return;
        if (!importing.compareAndSet(false, true)) {
            notify(NotificationType.INFO, "Another dependency import is already running.");
            return;
        }
        Thread worker = new Thread(() -> {
            try {
                DependencyArchive replacement = DependencyArchiveReader.read(
                        file, trinity.getDatabase().getPath(), previous.getId());
                Main.runLater(() -> {
                    if (Main.getTrinity() != trinity) return;
                    trinity.getExecution().getDependencies().replaceArchive(previous, replacement);
                    trinity.getEventManager().postEvent(new EventDependenciesChanged());
                    notify(NotificationType.SUCCESS, "Updated dependency location to " + file.getAbsolutePath() + ".");
                });
            } catch (Throwable throwable) {
                notify(NotificationType.ERROR, "Could not use " + file.getName() + ": " + message(throwable));
            } finally {
                importing.set(false);
            }
        }, "Trinity dependency rebinder");
        worker.setDaemon(true);
        worker.start();
    }

    private static boolean isSupported(File file) {
        if (file == null || !file.isFile()) return false;
        String name = file.getName().toLowerCase();
        return name.endsWith(".jar") || name.endsWith(".zip") || name.endsWith(".jmod");
    }

    private static String message(Throwable throwable) {
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    private static void notify(NotificationType type, String message) {
        Main.getDisplayManager().addNotification(new Notification(type, new SimpleCaption("Dependencies"),
                ColoredStringBuilder.create().fmt("{}", message).get()));
    }
}
