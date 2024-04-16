package me.f1nal.trinity.database.patches.impl;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.patches.AbstractDatabasePatch;

public class DatabasePatchMethodHierarchyNames extends AbstractDatabasePatch {
    @Override
    public boolean isForVersion(int version) {
        return version == 1;
    }

    @Override
    public void apply(Trinity trinity) {

    }
}
