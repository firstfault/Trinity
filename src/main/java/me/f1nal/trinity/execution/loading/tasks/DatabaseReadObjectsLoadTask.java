package me.f1nal.trinity.execution.loading.tasks;

import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.database.object.AbstractDatabaseObject;
import me.f1nal.trinity.execution.loading.ProgressiveLoadTask;

import java.util.ArrayList;
import java.util.List;

public class DatabaseReadObjectsLoadTask extends ProgressiveLoadTask {
    public DatabaseReadObjectsLoadTask() {
        super("Reading Objects");
    }

    @Override
    public void runImpl() {
        Database database = getTrinity().getDatabase();

        startWork(database.getObjects().size());
        List<AbstractDatabaseObject> invalid = new ArrayList<>();
        database.getObjects().forEach(obj -> {
            if (!obj.load(getTrinity())) invalid.add(obj);
            finishedWork();
        });
        invalid.forEach(database.getObjects()::remove);
        database.setLoaded(getTrinity());
    }
}

