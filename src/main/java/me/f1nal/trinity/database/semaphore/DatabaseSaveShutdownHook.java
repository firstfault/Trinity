package me.f1nal.trinity.database.semaphore;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.database.DatabaseLoader;
import me.f1nal.trinity.logging.Logging;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DatabaseSaveShutdownHook implements Runnable {
    @Override
    public void run() {
        final Trinity trinity = Main.getTrinity();

        if (trinity == null) {
            return;
        }

        if (!DatabaseLoader.save.getLastRun().hasPassed(1500L)) {
            return;
        }

        Database database = trinity.getDatabase();
        Future<Boolean> future = DatabaseLoader.save.get(database.getPath());
        Logging.info("Saving database '{}'. Please wait a few seconds before killing the process.", database.getName());
        try {
            future.get(25L, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Logging.error("Failed to save database '{}' during shutdown: {}", database.getName(), e);
        }
    }
}
