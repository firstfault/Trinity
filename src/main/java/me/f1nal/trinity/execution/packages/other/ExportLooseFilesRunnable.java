package me.f1nal.trinity.execution.packages.other;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.database.datapool.DataPool;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.execution.packages.ProjectContainer;
import me.f1nal.trinity.execution.packages.ResourceArchiveEntry;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.gui.viewport.notifications.SimpleCaption;

import javax.swing.JFileChooser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/** Exports the synthetic loose container as an ordinary package directory. */
public final class ExportLooseFilesRunnable implements Runnable {
    private final ProjectContainer container;

    public ExportLooseFilesRunnable(ProjectContainer container) {
        this.container = container;
    }

    @Override
    public void run() {
        File databaseFile = Main.getTrinity().getDatabase().getPath();
        File initial = databaseFile == null ? new File("").getAbsoluteFile() : databaseFile.getAbsoluteFile().getParentFile();
        JFileChooser chooser = new JFileChooser(initial);
        chooser.setDialogTitle("Export Loose Files");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return;

        Path root = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        Thread worker = new Thread(() -> export(root), "Trinity loose file exporter");
        worker.setDaemon(true);
        worker.start();
    }

    private void export(Path root) {
        try {
            Files.createDirectories(root);
            int written = 0;
            for (var target : container.getClasses()) {
                if (target.getInput() == null) continue;
                write(root, target.getRealName() + ".class", DataPool.writeClassNode(target.getInput().getNode()));
                written++;
            }
            for (ResourceArchiveEntry resource : container.getResources()) {
                write(root, resource.getRealName(), resource.getBytes());
                written++;
            }
            notify(NotificationType.SUCCESS, "Exported " + written + " loose entries.");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            notify(NotificationType.ERROR, "Loose export failed: " + throwable.getMessage());
        }
    }

    private static void write(Path root, String entryName, byte[] bytes) throws Exception {
        Path output = root.resolve(entryName.replace('/', File.separatorChar)).normalize();
        if (!output.startsWith(root)) throw new IllegalArgumentException("Entry escapes export directory: " + entryName);
        if (output.getParent() != null) Files.createDirectories(output.getParent());
        Files.write(output, bytes);
    }

    private static void notify(NotificationType type, String message) {
        Main.runLater(() -> Main.getDisplayManager().addNotification(new Notification(type,
                new SimpleCaption("Loose Files"), ColoredStringBuilder.create().fmt("{}", message).get())));
    }
}
