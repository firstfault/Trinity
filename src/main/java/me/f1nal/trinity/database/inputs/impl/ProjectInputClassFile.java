package me.f1nal.trinity.database.inputs.impl;

import me.f1nal.trinity.database.inputs.AbstractProjectInputFile;
import me.f1nal.trinity.database.inputs.UnreadClassBytes;

import java.io.File;

public class ProjectInputClassFile extends AbstractProjectInputFile {
    public ProjectInputClassFile(File file, byte[] bytes) {
        super(file);

        this.getClassPath().addClass(new UnreadClassBytes(file.getName(), bytes));
    }
}
