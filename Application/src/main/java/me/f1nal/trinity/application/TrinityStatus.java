package me.f1nal.trinity.application;

import java.util.Objects;

/** Immutable snapshot returned by the application status query. */
public record TrinityStatus(String version, ProjectStatus project) {
    public TrinityStatus {
        Objects.requireNonNull(version, "version");
    }

    public boolean projectLoaded() {
        return project != null;
    }

    public record ProjectStatus(
            String name,
            String databasePath,
            boolean ready,
            String loadingStage,
            int loadingProgress,
            int classCount,
            int resourceCount,
            int packageCount) {
        public ProjectStatus {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(databasePath, "databasePath");
            if (loadingProgress < 0 || loadingProgress > 100) {
                throw new IllegalArgumentException("loadingProgress must be between 0 and 100");
            }
            if (classCount < 0 || resourceCount < 0 || packageCount < 0) {
                throw new IllegalArgumentException("project counts must not be negative");
            }
        }
    }
}
