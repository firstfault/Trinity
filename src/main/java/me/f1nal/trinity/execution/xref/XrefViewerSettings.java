package me.f1nal.trinity.execution.xref;

import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.object.DatabaseXrefViewerSettings;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/** Project-level xref viewer filter preferences. */
public final class XrefViewerSettings implements IDatabaseSavable<DatabaseXrefViewerSettings> {
    private final EnumSet<XrefKind> disabledKinds =
            EnumSet.of(XrefKind.DESCRIPTOR, XrefKind.METADATA, XrefKind.STACK_FRAME);

    public boolean isKindEnabled(XrefKind kind) {
        return !disabledKinds.contains(kind);
    }

    public void setKindEnabled(XrefKind kind, boolean enabled) {
        if (enabled) {
            disabledKinds.remove(kind);
        } else {
            disabledKinds.add(kind);
        }
    }

    public Set<XrefKind> getDisabledKinds() {
        return Set.copyOf(disabledKinds);
    }

    public void restoreDisabledKindNames(Collection<String> names) {
        if (names == null) return;

        disabledKinds.clear();
        for (String name : names) {
            try {
                disabledKinds.add(XrefKind.valueOf(name));
            } catch (IllegalArgumentException | NullPointerException ignored) {
                // Ignore settings written by a version with kinds this build does not know.
            }
        }
    }

    @Override
    public DatabaseXrefViewerSettings createDatabaseObject() {
        return new DatabaseXrefViewerSettings(disabledKinds);
    }
}
