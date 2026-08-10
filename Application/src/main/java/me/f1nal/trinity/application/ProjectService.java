package me.f1nal.trinity.application;

import java.util.List;

/** Headless project lifecycle and workspace-discovery use cases. */
public interface ProjectService {
    ProjectSnapshot current();

    ProjectSnapshot create(CreateProject command);

    ProjectSnapshot open(OpenProject command);

    ProjectSnapshot save(long expectedRevision);

    ProjectSnapshot close(CloseProject command);

    ExportResult exportJar(ExportJar command);

    Page<TreeEntry> tree(TreeQuery query);

    Page<SearchResult> search(SearchQuery query);

    record CreateProject(String name, String databasePath, String compression,
                         List<String> inputPaths) {
        public CreateProject {
            inputPaths = List.copyOf(inputPaths);
        }
    }

    record OpenProject(String databasePath) {
    }

    record CloseProject(long expectedRevision, boolean save) {
    }

    record ExportJar(String outputPath, long expectedRevision) {
    }

    record TreeQuery(String prefix, String kind, int offset, int limit) {
    }

    record SearchQuery(String query, String kind, boolean exact,
                       boolean caseSensitive, int offset, int limit) {
    }

    record ProjectSnapshot(String name, String databasePath, String compression,
                           boolean ready, int classCount, int resourceCount,
                           int packageCount, long revision) {
    }

    record ExportResult(String outputPath, long byteCount, int classCount,
                        int resourceCount, long revision) {
    }

    record TreeEntry(String kind, String path, String displayName, Long size) {
    }

    record SearchResult(String kind, String identity, String displayName,
                        String owner, String descriptor, int score) {
    }
}
