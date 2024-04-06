package me.f1nal.trinity.database;

import com.thoughtworks.xstream.annotations.XStreamOmitField;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.compression.DatabaseCompressionType;
import me.f1nal.trinity.database.object.AbstractDatabaseObject;
import me.f1nal.trinity.execution.loading.AsynchronousLoad;
import me.f1nal.trinity.execution.loading.ProgressiveLoadTask;
import me.f1nal.trinity.logging.Logging;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Database {
    private String name;
    private final Set<AbstractDatabaseObject> objects;
    private final long creationTime;
    private long openTime;
    @XStreamOmitField
    private boolean opened;
    @XStreamOmitField
    private long objectsLoadTime;
    /**
     * Size of the last loaded/saved database file, in bytes.
     */
    @XStreamOmitField
    private long databaseSize;
    /**
     * Path to the database <b>file</b>, not folder.
     */
    @XStreamOmitField
    private File path;
    @XStreamOmitField
    private DatabaseCompressionType compressionType;
    @XStreamOmitField
    private long dataPoolLoadTime;
    @XStreamOmitField
    public List<ProgressiveLoadTask> loadTasks;

    public Database(String name, File path, DatabaseCompressionType compressionType) {
        this.name = name;
        this.path = path;
        this.resetLastOpen();
        this.creationTime = System.currentTimeMillis();
        this.compressionType = compressionType;
        this.objects = new HashSet<>();
    }

    public void setDatabaseSize(long databaseSize) {
        this.databaseSize = databaseSize;
    }

    public long getDatabaseSize() {
        return databaseSize;
    }

    public void resetLastOpen() {
        this.openTime = System.currentTimeMillis();
    }

    public long getLastOpenTime() {
        return openTime;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getObjectsLoadTime() {
        return objectsLoadTime;
    }

    public DatabaseCompressionType getCompressionType() {
        return compressionType;
    }

    public void setCompressionType(DatabaseCompressionType compressionType) {
        this.compressionType = compressionType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<AbstractDatabaseObject> getObjects() {
        return objects;
    }

    public void setDataPoolLoadTime(long dataPoolLoadTime) {
        this.dataPoolLoadTime = dataPoolLoadTime;
    }

    public long getDataPoolLoadTime() {
        return dataPoolLoadTime;
    }

    public void save(IDatabaseSavable<?> savable) {
        if (!this.opened) {
            Logging.warn("Database modified during load time with " + savable.getClass().getSimpleName());
            return;
        }
        AbstractDatabaseObject obj = savable.createDatabaseObject();
        if (obj == null) return;
        this.objects.remove(obj);
        this.objects.add(obj);
    }

    public boolean isOpened() {
        return opened;
    }

    public boolean isLoading() {
        return !isOpened();
    }

    public void setPath(File path) {
        this.path = path;
    }

    public File getPath() {
        return path;
    }

    /**
     * Loads this database back into Trinity.
     * Classes are loaded manually, right after database creation, unrelated to here.
     */
    public void reload(Trinity trinity) {
        long millis = System.currentTimeMillis();
        this.resetLastOpen();
        this.opened = true;
        this.objectsLoadTime = System.currentTimeMillis() - millis;
    }

    public boolean addLoadTasks(AsynchronousLoad asynchronousLoad) {
        if (this.loadTasks == null) {
            return false;
        }
        this.loadTasks.forEach(asynchronousLoad::add);
        this.loadTasks = null;
        return true;
    }
}
