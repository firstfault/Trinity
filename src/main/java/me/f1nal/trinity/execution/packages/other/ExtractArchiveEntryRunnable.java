package me.f1nal.trinity.execution.packages.other;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.packages.ArchiveEntry;
import me.f1nal.trinity.gui.components.general.FileSelectorComponent;
import me.f1nal.trinity.gui.viewport.notifications.ICaption;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.util.NameUtil;
import com.google.common.io.Files;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class ExtractArchiveEntryRunnable implements Runnable, ICaption {
    private final String simpleName;
    private final byte[] extract;

    public ExtractArchiveEntryRunnable(ArchiveEntry archiveEntry) {
        // FIXME: Manual .class append
        this(archiveEntry.getDisplaySimpleName() + (archiveEntry instanceof ClassTarget ? ".class" : ""), archiveEntry.extract());
    }

    public ExtractArchiveEntryRunnable(String simpleName, byte[] extract) {
        this.simpleName = simpleName;
        this.extract = extract;
    }

    @Override
    public String getCaption() {
        return "Extracting Archive File";
    }

    @Override
    public void run() {
        if (extract == null) {
            Main.getDisplayManager().addNotification(new Notification(NotificationType.ERROR, this, ColoredStringBuilder.create().
                    fmt("Failed to extract file {}", simpleName).get()));
            return;
        }

        File parentFile = Main.getTrinity().getDatabase().getPath().getParentFile();

        File targetFile = new File(parentFile, simpleName);
        String extension = !simpleName.contains(".") ? null : NameUtil.getExtension(simpleName);
        FilenameFilter filter = extension == null ? null : (dir, name) -> name.endsWith("." + extension);
        FileSelectorComponent selector = new FileSelectorComponent("Extract To", targetFile.getAbsolutePath(), filter, FileDialog.SAVE);
        File file = selector.openFileChooser();

        if (file == null) {
            Main.getDisplayManager().addNotification(new Notification(NotificationType.INFO, this, ColoredStringBuilder.create().
                    fmt("Operation cancelled").get()));
            return;
        }

        try {
            Files.write(extract, file);
        } catch (IOException e) {
            Main.getDisplayManager().addNotification(new Notification(NotificationType.ERROR, this, ColoredStringBuilder.create().
                    fmt("Failed to save file {}", file.getAbsolutePath()).get()));
            return;
        }

        Main.getDisplayManager().addNotification(new Notification(NotificationType.SUCCESS, this, ColoredStringBuilder.create().
                fmt("Saved file {} to {}", simpleName, file.getAbsolutePath()).get()));
    }
}
