package me.f1nal.trinity.execution.packages;

import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.object.DatabaseExportJarSettings;

import java.util.UUID;

public final class ExportJarSettings implements IDatabaseSavable<DatabaseExportJarSettings> {
    private final UUID containerId;
    private boolean removeSignatures = true;
    private boolean ignoreUnresolvedDependencies;
    private boolean overwriteExisting;

    public ExportJarSettings(UUID containerId) {
        this.containerId = containerId;
    }

    public boolean isRemoveSignatures() {
        return removeSignatures;
    }

    public boolean isIgnoreUnresolvedDependencies() {
        return ignoreUnresolvedDependencies;
    }

    public boolean isOverwriteExisting() {
        return overwriteExisting;
    }

    public void set(boolean removeSignatures, boolean ignoreUnresolvedDependencies,
                    boolean overwriteExisting) {
        this.removeSignatures = removeSignatures;
        this.ignoreUnresolvedDependencies = ignoreUnresolvedDependencies;
        this.overwriteExisting = overwriteExisting;
    }

    @Override
    public DatabaseExportJarSettings createDatabaseObject() {
        return new DatabaseExportJarSettings(containerId.toString(), removeSignatures,
                ignoreUnresolvedDependencies, overwriteExisting);
    }
}
