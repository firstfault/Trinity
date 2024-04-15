package me.f1nal.trinity.refactor.globalrename.api;

import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.remap.Remapper;

public final class Rename {
    private final Input<?> input;
    private final String newName;

    public Rename(Input<?> input, String newName) {
        this.input = input;
        this.newName = newName;
    }

    public Input<?> getInput() {
        return input;
    }

    public String getNewName() {
        return newName;
    }

    public void rename(Remapper remapper) {
        input.rename(remapper, this.getNewName());
    }

    public String getCurrentName() {
        return input.getDisplayName().getName();
    }
}
