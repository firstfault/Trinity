package me.f1nal.trinity.gui.windows.impl.cp;

public interface RenameHandler {
    /**
     * Full name of this entry, to set the renaming field to when beginning rename.
     * @return The full name or {@code null} to automatically get the name.
     */
    default String getFullName() {
        return null;
    }

    void rename(String newName);
}
