package me.f1nal.trinity.gui.search;

import com.google.common.eventbus.Subscribe;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.events.EventPackageStructureReload;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.packages.ArchiveEntry;
import me.f1nal.trinity.execution.packages.Package;

import java.util.ArrayList;
import java.util.List;

/** Lazily rebuilt project-file snapshot for the global search hot path. */
public final class GlobalSearchFileIndex implements IEventListener {
    private Trinity trinity;
    private List<SearchFile> files = List.of();
    private boolean dirty = true;
    private long revision;

    public synchronized void setTrinity(Trinity trinity) {
        if (this.trinity == trinity) return;
        this.trinity = trinity;
        this.files = List.of();
        this.dirty = true;
        this.revision++;
        if (trinity != null) trinity.getEventManager().registerListener(this);
    }

    public synchronized Snapshot snapshot() {
        if (dirty) this.rebuild();
        return new Snapshot(files, revision);
    }

    public synchronized Snapshot revisionOnly() {
        return new Snapshot(List.of(), revision);
    }

    private void rebuild() {
        List<SearchFile> rebuilt = new ArrayList<>();
        if (trinity != null) this.collect(trinity.getExecution().getRootPackage(), rebuilt);
        this.files = List.copyOf(rebuilt);
        this.dirty = false;
        this.revision++;
    }

    private void collect(Package pkg, List<SearchFile> destination) {
        for (ArchiveEntry entry : pkg.getEntries()) {
            if (!(entry instanceof ClassTarget classTarget) || classTarget.getInput() != null) {
                destination.add(SearchFile.create(entry));
            }
        }
        for (Package child : pkg.getPackages()) this.collect(child, destination);
    }

    private synchronized void invalidate() {
        this.dirty = true;
    }

    @Subscribe
    public void onClassesLoaded(EventClassesLoaded event) {
        this.invalidate();
    }

    @Subscribe
    public void onPackageStructureReload(EventPackageStructureReload event) {
        this.invalidate();
    }

    @Subscribe
    public void onClassModified(EventClassModified event) {
        this.invalidate();
    }

    public record Snapshot(List<SearchFile> files, long revision) {
        public Snapshot {
            files = List.copyOf(files);
        }
    }

    public record SearchFile(ArchiveEntry entry, String title, String path, String realPath,
                             String parentPath, String type, String size, String icon,
                             String normalizedTitle, List<String> normalizedSearchableText) {
        public SearchFile {
            normalizedSearchableText = List.copyOf(normalizedSearchableText);
        }

        private static SearchFile create(ArchiveEntry entry) {
            String path = entry.getDisplayOrRealName();
            int separator = path.lastIndexOf('/');
            String parent = separator == -1 ? "Project root" : path.substring(0, separator);
            List<String> searchable = new ArrayList<>();
            searchable.add(path);
            searchable.add(entry.getRealName());
            searchable.add(parent);
            searchable.add(entry.getArchiveEntryTypeName());
            searchable.add(path.replace('/', '.'));
            searchable.add(entry.getRealName().replace('/', '.'));
            if (entry instanceof ClassTarget) {
                searchable.add(path + ".class");
                searchable.add(entry.getRealName() + ".class");
            }
            return new SearchFile(entry, entry.getDisplaySimpleName(), path, entry.getRealName(),
                    parent, entry.getArchiveEntryTypeName(), entry.getSize(),
                    entry.getBrowserViewerNode().getIcon(),
                    GlobalSearchMatcher.normalize(entry.getDisplaySimpleName()),
                    searchable.stream().map(GlobalSearchMatcher::normalize).toList());
        }

        public int color() {
            return entry.getBrowserViewerNode().getColor().get();
        }
    }
}
