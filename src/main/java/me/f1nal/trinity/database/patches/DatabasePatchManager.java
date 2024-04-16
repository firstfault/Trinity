package me.f1nal.trinity.database.patches;

import me.f1nal.trinity.database.patches.impl.DatabasePatchMethodHierarchyNames;

import java.util.List;

public class DatabasePatchManager {
    private static final List<AbstractDatabasePatch> PATCH_LIST = List.of(
            new DatabasePatchMethodHierarchyNames()
    );
}
