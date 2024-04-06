package me.f1nal.trinity.database.inputs;

import com.google.common.io.Files;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.database.inputs.impl.ProjectInputClassFile;
import me.f1nal.trinity.database.inputs.impl.ProjectInputJARFile;
import me.f1nal.trinity.gui.components.filelist.ListedFileFactory;
import me.f1nal.trinity.gui.frames.impl.project.create.misc.ClassPathViewerWindow;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ProjectInputFileFactory implements ListedFileFactory<AbstractProjectInputFile> {
    @Override
    public @Nullable AbstractProjectInputFile create(File file) {
        byte[] bytes;
        try {
            bytes = Files.toByteArray(file);
        } catch (IOException e) {
            return null;
        }

        final ByteBuffer headerBytes = ByteBuffer.wrap(bytes, 0, 4);

        if (headerBytes.getInt(0) == 0xcafebabe) {
            return new ProjectInputClassFile(file, bytes);
        }

        try {
            return new ProjectInputJARFile(file, bytes);
        } catch (IOException e) {
            // Not a JAR file
        }

        return null;
    }

    @Override
    public void view(AbstractProjectInputFile file) {
        Main.getDisplayManager().addClosableWindow(new ClassPathViewerWindow(file.getName(), file.getClassPath()));
    }
}
