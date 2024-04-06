package me.f1nal.trinity.gui.components.filelist;

import me.f1nal.trinity.util.IDescribable;
import me.f1nal.trinity.util.INameable;

import java.io.File;

public class ListedFile implements INameable, IDescribable {
    private final File file;
    private final String name;
    private final String path;

    protected ListedFile(File file) {
        this.file = file;
        this.name = file.getName();
        this.path = file.getAbsolutePath();
    }

    public File getFile() {
        return file;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDescription() {
        return this.path;
    }
}