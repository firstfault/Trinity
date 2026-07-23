package me.f1nal.trinity.database.object;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.logging.Logging;

import java.util.Objects;

public class DatabasePackage extends AbstractDatabaseObject {
    private final String containerId;
    private final String path;
    private final boolean open;

    public DatabasePackage(String containerId, String path, boolean open) {
        this.containerId = containerId;
        this.path = path;
        this.open = open;
    }

    @Override
    public boolean load(Trinity trinity) {
        me.f1nal.trinity.execution.packages.ProjectContainer container;
        try {
            container = trinity.getExecution().getContainer(java.util.UUID.fromString(containerId));
        } catch (IllegalArgumentException exception) {
            return false;
        }
        if (container == null) return false;
        Package pkg = container.getRootPackage().getPackageHierarchy().getPathToPackage().get(path);
        if (pkg == null) {
            Logging.warn("Cannot find package {} from database object.", path);
            return false;
        }
        pkg.setOpenForced(open);
        return true;
    }

    @Override
    protected int databaseHashCode() {
        return Objects.hash("package", containerId, path);
    }
}
