package me.f1nal.trinity.remap;

public enum RenameType {
    /**
     * Original name, it is not renamed.
     */
    NONE,
    /**
     * Rename issued by a human.
     */
    MANUAL,
    /**
     * Rename issued with automatic tools.
     */
    AUTOMATED;
}
