package me.f1nal.trinity.refactor.globalrename.api;

import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.remap.Remapper;

public class MethodRename extends Rename<MethodInput> {
    public MethodRename(MethodInput input, String newName) {
        super(input, newName);
    }

    @Override
    public void rename(Remapper remapper) {
        remapper.renameMethod(this.getInput(), this.getNewName());
    }

    @Override
    public String getCurrentName() {
        return getInput().getDisplayName();
    }
}
