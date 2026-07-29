package me.f1nal.trinity.execution.loading.tasks;

import me.f1nal.trinity.execution.dependency.DependencyArchiveReader;
import me.f1nal.trinity.execution.loading.ProgressiveLoadTask;

import java.io.IOException;

/** Seeds a new or migrated project with java.base from Trinity's running JDK. */
public final class RuntimeDependencyLoadTask extends ProgressiveLoadTask {
    public RuntimeDependencyLoadTask() {
        super("Reading java.base");
    }

    @Override
    public void runImpl() {
        startWork(1);
        try {
            getTrinity().getExecution().getDependencies()
                    .addArchive(DependencyArchiveReader.readRuntimeJavaBase());
        } catch (IOException exception) {
            throw new RuntimeException("Unable to add the running JDK java.base dependency", exception);
        }
        finishedWork();
    }
}
