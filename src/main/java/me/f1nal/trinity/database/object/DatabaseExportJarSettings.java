package me.f1nal.trinity.database.object;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.packages.ProjectContainer;

import java.util.Objects;
import java.util.UUID;

public final class DatabaseExportJarSettings extends AbstractDatabaseObject {
    private final String containerId;
    private final boolean removeSignatures;
    private final boolean ignoreUnresolvedDependencies;
    private final boolean overwriteExisting;

    public DatabaseExportJarSettings(String containerId, boolean removeSignatures,
                                     boolean ignoreUnresolvedDependencies,
                                     boolean overwriteExisting) {
        this.containerId = containerId;
        this.removeSignatures = removeSignatures;
        this.ignoreUnresolvedDependencies = ignoreUnresolvedDependencies;
        this.overwriteExisting = overwriteExisting;
    }

    @Override
    public boolean load(Trinity trinity) {
        UUID id;
        try {
            id = UUID.fromString(containerId);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        ProjectContainer container = trinity.getExecution().getContainer(id);
        if (container == null || !container.isJar()) return false;
        container.getExportJarSettings().set(removeSignatures,
                ignoreUnresolvedDependencies, overwriteExisting);
        return true;
    }

    @Override
    protected int databaseHashCode() {
        return Objects.hash("exportJarSettings", containerId);
    }
}
