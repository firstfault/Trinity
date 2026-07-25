package me.f1nal.trinity.database.inputs;

import me.f1nal.trinity.util.FileUtil;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.database.inputs.impl.ProjectInputClassFile;
import me.f1nal.trinity.database.inputs.impl.ProjectInputAPKMFile;
import me.f1nal.trinity.database.inputs.impl.ProjectInputDEXFile;
import me.f1nal.trinity.database.inputs.impl.ProjectInputJARFile;
import me.f1nal.trinity.gui.components.filelist.ListedFileFactory;
import me.f1nal.trinity.gui.windows.impl.project.create.misc.ClassPathViewerWindow;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;

public class ProjectInputFileFactory implements ListedFileFactory<AbstractProjectInputFile> {
    static final int MAX_PROJECT_INPUT_BYTES = 512 * 1024 * 1024;

    @Override
    public @Nullable AbstractProjectInputFile create(File file) {
        byte[] bytes;
        try {
            bytes = FileUtil.readAllBytes(file, MAX_PROJECT_INPUT_BYTES, "Project input");
        } catch (IOException e) {
            return null;
        }
        if (bytes.length < Integer.BYTES) {
            return null;
        }

        final ByteBuffer headerBytes = ByteBuffer.wrap(bytes, 0, Integer.BYTES);
        if (headerBytes.getInt(0) == 0xcafebabe) {
            return new ProjectInputClassFile(file, bytes);
        }

        if (bytes[0] == 'd' && bytes[1] == 'e' && bytes[2] == 'x' && bytes[3] == '\n') {
            return new ProjectInputDEXFile(file, bytes);
        }

        if (file.getName().toLowerCase(Locale.ROOT).endsWith(".apkm")) {
            try {
                return new ProjectInputAPKMFile(file, bytes);
            } catch (IOException exception) {
                return null;
            }
        }

        try {
            return new ProjectInputJARFile(file, bytes);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void view(AbstractProjectInputFile file) {
        Main.getWindowManager().addClosableWindow(new ClassPathViewerWindow(file.getName(), file.getClassPath()));
    }
}
