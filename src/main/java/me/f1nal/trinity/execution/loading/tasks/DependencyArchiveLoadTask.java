package me.f1nal.trinity.execution.loading.tasks;

import me.f1nal.trinity.execution.dependency.DependencyArchive;
import me.f1nal.trinity.execution.dependency.DependencyArchiveReader;
import me.f1nal.trinity.execution.loading.ProgressiveLoadTask;

import java.util.List;

/** Restores dependency archives persisted in a Trinity database. */
public final class DependencyArchiveLoadTask extends ProgressiveLoadTask {
    private final List<DependencyArchive> archives;

    public DependencyArchiveLoadTask(List<DependencyArchive> archives) {
        super("Reading Dependencies");
        this.archives = List.copyOf(archives);
    }

    @Override
    public void runImpl() {
        startWork(Math.max(1, archives.size()));
        for (DependencyArchive archive : archives) {
            DependencyArchiveReader.resolve(archive, getTrinity().getDatabase().getPath());
            getTrinity().getExecution().getDependencies().addArchive(archive);
            finishedWork();
        }
        if (archives.isEmpty()) finishedWork();
    }
}
