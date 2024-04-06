package me.f1nal.trinity.database.object;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.logging.Logging;

import java.util.Objects;

public class DatabasePackage extends AbstractDatabaseObject {
    private final String path;
    private final boolean open;

    public DatabasePackage(String path, boolean open) {
        this.path = path;
        this.open = open;
    }

    @Override
    public boolean load(Trinity trinity) {
        Package pkg = trinity.getExecution().getRootPackage().getPackageHierarchy().getPathToPackage().get(path);
        if (pkg == null) {
            Logging.warn("Cannot find package {} from database object.", path);
            return false;
        }
        pkg.setOpenForced(open);
        return true;
    }

    @Override
    protected int databaseHashCode() {
        return Objects.hash("package", path);
    }
}
