package me.f1nal.trinity.refactor.globalrename.api;

import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.remap.Remapper;

public class FieldRename extends Rename<FieldInput> {
    public FieldRename(FieldInput input, String newName) {
        super(input, newName);
    }

    @Override
    public void rename(Remapper remapper) {
        remapper.renameField(this.getInput(), this.getNewName());
    }

    @Override
    public String getCurrentName() {
        return getInput().getDisplayName();
    }
}
