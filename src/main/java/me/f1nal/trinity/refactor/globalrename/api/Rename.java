package me.f1nal.trinity.refactor.globalrename.api;

import me.f1nal.trinity.remap.Remapper;

public abstract class Rename<I> {
    private final I input;
    private final String newName;

    public Rename(I input, String newName) {
        this.input = input;
        this.newName = newName;
    }

    public I getInput() {
        return input;
    }

    public String getNewName() {
        return newName;
    }

    public abstract void rename(Remapper remapper);
    public abstract String getCurrentName();
}
