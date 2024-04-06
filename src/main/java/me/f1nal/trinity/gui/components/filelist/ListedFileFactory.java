package me.f1nal.trinity.gui.components.filelist;

import org.jetbrains.annotations.Nullable;

import java.io.File;

public interface ListedFileFactory<T extends ListedFile> {
    @Nullable T create(File file);
    void view(T file);
}
