package me.f1nal.trinity.database.object;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.xref.XrefKind;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Serialized project-level xref viewer settings. */
public final class DatabaseXrefViewerSettings extends AbstractDatabaseObject {
    private static final int CURRENT_VERSION = 1;

    private final int version;
    private final Set<String> disabledKinds;

    public DatabaseXrefViewerSettings(Collection<XrefKind> disabledKinds) {
        this.version = CURRENT_VERSION;
        this.disabledKinds = new LinkedHashSet<>();
        for (XrefKind kind : disabledKinds) {
            this.disabledKinds.add(kind.name());
        }
    }

    @Override
    public boolean load(Trinity trinity) {
        trinity.getXrefViewerSettings().restoreDisabledKindNames(disabledKinds);
        if (version < CURRENT_VERSION) {
            trinity.getXrefViewerSettings().setKindEnabled(XrefKind.METADATA, false);
        }
        return true;
    }

    @Override
    protected int databaseHashCode() {
        return Objects.hash("xrefViewerSettings");
    }
}
