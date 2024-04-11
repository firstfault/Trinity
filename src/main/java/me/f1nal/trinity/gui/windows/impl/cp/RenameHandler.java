package me.f1nal.trinity.gui.windows.impl.cp;

import me.f1nal.trinity.remap.Remapper;

public interface RenameHandler {
    /**
     * Full name of this entry, to set the renaming field to when beginning rename.
     * @return The full name or {@code null} to automatically get the name.
     */
    default String getFullName() {
        return null;
    }

    void rename(Remapper remapper, String newName);
}
