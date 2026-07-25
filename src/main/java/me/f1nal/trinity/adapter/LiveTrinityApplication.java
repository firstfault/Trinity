package me.f1nal.trinity.adapter;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.application.AnalysisService;
import me.f1nal.trinity.application.BrowseService;
import me.f1nal.trinity.application.DexService;
import me.f1nal.trinity.application.MutationService;
import me.f1nal.trinity.application.ProjectService;
import me.f1nal.trinity.application.TrinityApplication;
import me.f1nal.trinity.application.TrinityStatus;
import me.f1nal.trinity.execution.loading.AsynchronousLoad;
import me.f1nal.trinity.execution.loading.ProgressiveLoadTask;

import java.io.File;

/** Adapter from the running desktop application to the headless application contract. */
public final class LiveTrinityApplication implements TrinityApplication {
    private final LiveApplicationState state;
    private final ProjectService projects;
    private final BrowseService browse;
    private final AnalysisService analysis;
    private final DexService dex;
    private final MutationService mutations;

    public LiveTrinityApplication() {
        RenderThreadExecutor executor = new RenderThreadExecutor();
        this.state = new LiveApplicationState(executor);
        this.projects = new LiveProjectService(state);
        this.browse = new LiveBrowseService(state);
        this.analysis = new LiveAnalysisService(state);
        this.dex = new LiveDexService(state);
        this.mutations = new LiveMutationService(state);
    }

    @Override
    public String version() {
        return Main.VERSION;
    }

    @Override
    public TrinityStatus status() {
        if (state.currentOrNull() == null) {
            return new TrinityStatus(version(), null);
        }
        return state.read(false, this::snapshot);
    }

    @Override
    public ProjectService projects() {
        return projects;
    }

    @Override
    public BrowseService browse() {
        return browse;
    }

    @Override
    public AnalysisService analysis() {
        return analysis;
    }

    @Override
    public DexService dex() {
        return dex;
    }

    @Override
    public MutationService mutations() {
        return mutations;
    }

    private TrinityStatus snapshot(Trinity trinity) {
        AsynchronousLoad load = trinity.getExecution().getAsynchronousLoad();
        ProgressiveLoadTask currentTask = load.getCurrentTask();
        boolean ready = load.isFinished();
        String loadingStage = currentTask == null ? null : currentTask.getName();
        int progress = ready ? 100 : currentTask == null ? 0 : clamp(currentTask.getProgress());
        File databasePath = trinity.getDatabase().getPath();

        TrinityStatus.ProjectStatus project = new TrinityStatus.ProjectStatus(
                trinity.getDatabase().getName(),
                databasePath == null ? "" : databasePath.getAbsolutePath(),
                ready,
                loadingStage,
                progress,
                trinity.getExecution().getClassList().size()
                        + trinity.getExecution().getDexIndex().classCount(),
                trinity.getExecution().getResourceMap().size(),
                trinity.getExecution().getAllPackages().size());
        return new TrinityStatus(version(), project);
    }

    private static int clamp(int progress) {
        return Math.max(0, Math.min(100, progress));
    }
}
