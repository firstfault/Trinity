package me.f1nal.trinity.database.inputs;

import me.f1nal.trinity.database.ClassPath;
import me.f1nal.trinity.gui.components.filelist.ListedFile;
import me.f1nal.trinity.util.IDescribable;

import java.io.File;

public abstract class AbstractProjectInputFile extends ListedFile implements IDescribable {
    private final ClassPath classPath = new ClassPath();

    public AbstractProjectInputFile(File file) {
        super(file);
    }

    public final ClassPath getClassPath() {
        return classPath;
    }

    public final void addInputs(ClassPath classPath) {
        classPath.addClassPath(classPath);
    }

    @Override
    public String getDescription() {
        return classPath.getClasses().size() + " classes, " + classPath.getResources().size() + " resources";
    }
}
