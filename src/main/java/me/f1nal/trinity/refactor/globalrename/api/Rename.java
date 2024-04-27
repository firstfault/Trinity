package me.f1nal.trinity.refactor.globalrename.api;

import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.gui.windows.impl.cp.RenameHandler;
import me.f1nal.trinity.remap.Remapper;

public final class Rename {
    private final RenameHandler handler;
    private final String newName;

    public Rename(RenameHandler handler, String newName) {
        this.handler = handler;
        this.newName = newName;
    }

    public RenameHandler getHandler() {
        return handler;
    }

    public String getNewName() {
        return newName;
    }

    public void rename(Remapper remapper) {
        this.handler.rename(remapper, newName);
    }
}
