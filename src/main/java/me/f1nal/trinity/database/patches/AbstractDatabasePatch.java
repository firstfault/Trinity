package me.f1nal.trinity.database.patches;

import me.f1nal.trinity.Trinity;

public abstract class AbstractDatabasePatch {
    public abstract boolean isForVersion(int version);
    public abstract void apply(Trinity trinity);
}
