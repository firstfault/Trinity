package me.f1nal.trinity.refactor.globalrename.api;

import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.remap.Remapper;

public class ClassRename extends Rename<ClassTarget> {
    public ClassRename(ClassTarget input, String newName) {
        super(input, newName);
    }

    @Override
    public void rename(Remapper remapper) {
        remapper.renameClass(this.getInput(), this.getNewName());
    }

    @Override
    public String getCurrentName() {
        return getInput().getDisplayOrRealName();
    }
}
