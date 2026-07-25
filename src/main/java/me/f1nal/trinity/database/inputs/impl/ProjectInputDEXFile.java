package me.f1nal.trinity.database.inputs.impl;

import me.f1nal.trinity.database.inputs.AbstractProjectInputFile;
import me.f1nal.trinity.database.inputs.UnreadDexBytes;

import java.io.File;

/** A standalone Android DEX project input. */
public final class ProjectInputDEXFile extends AbstractProjectInputFile {
    public ProjectInputDEXFile(File file, byte[] bytes) {
        super(file);
        getClassPath().getDexFiles().add(new UnreadDexBytes(file.getName(), bytes));
    }
}
