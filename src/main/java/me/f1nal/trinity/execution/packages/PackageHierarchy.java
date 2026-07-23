package me.f1nal.trinity.execution.packages;

import me.f1nal.trinity.database.Database;

import java.util.HashMap;
import java.util.Map;

public class PackageHierarchy {
    private final Package root;
    private final Database database;
    private final ProjectContainer container;
    private final Map<String, Package> pathToPackage = new HashMap<>();

    public PackageHierarchy(Package root, Database database, ProjectContainer container) {
        this.root = root;
        this.database = database;
        this.container = container;
    }

    public Map<String, Package> getPathToPackage() {
        return pathToPackage;
    }

    public Database getDatabase() {
        return database;
    }

    public ProjectContainer getContainer() {
        return container;
    }
}
