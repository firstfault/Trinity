package me.f1nal.trinity.adapter;

import com.google.common.eventbus.Subscribe;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.application.ApplicationException;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.events.EventRefreshDecompilerText;
import me.f1nal.trinity.events.api.IEventListener;

import java.util.concurrent.atomic.AtomicLong;

/** Shared project identity, readiness, revision, and render-thread policy for all live services. */
final class LiveApplicationState {
    private final RenderThreadExecutor executor;
    private final AtomicLong revision = new AtomicLong();
    private Trinity trackedProject;

    LiveApplicationState(RenderThreadExecutor executor) {
        this.executor = executor;
    }

    <T> T read(boolean requireReady, CheckedFunction<Trinity, T> operation) {
        return executor.call(() -> {
            Trinity project = observe(Main.getTrinity());
            requireProject(project);
            if (requireReady && !project.getExecution().getAsynchronousLoad().isFinished()) {
                throw new ApplicationException(ApplicationException.Code.PROJECT_NOT_READY,
                        "The active project is still loading");
            }
            return operation.apply(project);
        });
    }

    <T> Changed<T> mutate(long expectedRevision, CheckedFunction<Trinity, T> operation) {
        return executor.call(() -> {
            Trinity project = observe(Main.getTrinity());
            requireProject(project);
            if (!project.getExecution().getAsynchronousLoad().isFinished()) {
                throw new ApplicationException(ApplicationException.Code.PROJECT_NOT_READY,
                        "The active project is still loading");
            }
            checkRevision(expectedRevision);
            long previous = revision.get();
            T value = operation.apply(project);
            long current = revision.get();
            if (current == previous) {
                current = revision.incrementAndGet();
            }
            return new Changed<>(value, previous, current);
        });
    }

    Changed<Trinity> createAndInstall(CheckedSupplier<Trinity> factory) {
        return executor.call(() -> {
            if (Main.getTrinity() != null) {
                throw new ApplicationException(ApplicationException.Code.PROJECT_ALREADY_OPEN,
                        "Close the active project before installing another project");
            }
            Trinity project = factory.get();
            long previous = revision.get();
            Main.getDisplayManager().setDatabase(project);
            observe(project);
            long current = revision.get();
            if (current == previous) current = revision.incrementAndGet();
            return new Changed<>(project, previous, current);
        });
    }

    Changed<Trinity> uninstall(long expectedRevision) {
        return executor.call(() -> {
            Trinity project = observe(Main.getTrinity());
            requireProject(project);
            checkRevision(expectedRevision);
            long previous = revision.get();
            Main.getDisplayManager().setDatabase(null);
            trackedProject = null;
            long current = revision.incrementAndGet();
            return new Changed<>(project, previous, current);
        });
    }

    Trinity currentOrNull() {
        return executor.call(() -> observe(Main.getTrinity()));
    }

    long revision() {
        return revision.get();
    }

    void checkRevision(long expectedRevision) {
        long current = revision.get();
        if (expectedRevision != current) {
            throw new ApplicationException(ApplicationException.Code.REVISION_CONFLICT,
                    "Expected project revision " + expectedRevision + " but current revision is " + current);
        }
    }

    private Trinity observe(Trinity project) {
        if (project != trackedProject) {
            trackedProject = project;
            revision.incrementAndGet();
            if (project != null) {
                project.getEventManager().registerListener(new RevisionListener(this));
            }
        }
        return project;
    }

    private static void requireProject(Trinity project) {
        if (project == null) {
            throw new ApplicationException(ApplicationException.Code.PROJECT_NOT_LOADED,
                    "No Trinity project is open");
        }
    }

    private void changed() {
        revision.incrementAndGet();
    }

    record Changed<T>(T value, long previousRevision, long revision) {
    }

    @FunctionalInterface
    interface CheckedFunction<T, R> {
        R apply(T value) throws Exception;
    }

    @FunctionalInterface
    interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    private static final class RevisionListener implements IEventListener {
        private final LiveApplicationState state;

        private RevisionListener(LiveApplicationState state) {
            this.state = state;
        }

        @Subscribe
        public void onClassesChanged(EventClassesLoaded ignored) {
            state.changed();
        }

        @Subscribe
        public void onMemberChanged(EventMemberModified ignored) {
            state.changed();
        }

        @Subscribe
        public void onNamesChanged(EventRefreshDecompilerText ignored) {
            state.changed();
        }
    }
}
