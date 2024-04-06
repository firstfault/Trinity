package me.f1nal.trinity.appdata;

import java.io.File;
import java.util.*;

public class RecentDatabasesFile extends AppDataFile {
    private final Set<RecentDatabaseEntry> databases = new HashSet<>();

    protected RecentDatabasesFile(AppDataManager manager) {
        super("recent", manager);
        this.addAlias(RecentDatabaseEntry.class, "database");
    }

    @Override
    public void handleLoad() {
        databases.removeIf(entry -> !new File(entry.getPath()).isFile());
    }

    public Set<RecentDatabaseEntry> getDatabases() {
        return databases;
    }

    public List<RecentDatabaseEntry> getSortedDatabases() {
        List<RecentDatabaseEntry> entries = new ArrayList<>(this.databases);
        entries.sort(Comparator.comparingLong(recentDatabaseEntry -> -recentDatabaseEntry.getLastOpened()));
        return entries;
    }

    public String getMostRecentDatabasePath() {
        List<RecentDatabaseEntry> sortedDatabases = getSortedDatabases();
        if (sortedDatabases.isEmpty()) {
            return null;
        }
        RecentDatabaseEntry recentDatabaseEntry = sortedDatabases.get(0);
        return recentDatabaseEntry.getPath();
    }

    public void addDatabase(RecentDatabaseEntry entry) {
        getDatabases().remove(entry);
        getDatabases().add(entry);
    }
}
