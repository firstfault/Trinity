package me.f1nal.trinity.database.semaphore;

import java.io.File;

public interface DatabaseLoaderRunnable {
    void run(File path) throws Throwable;
}
