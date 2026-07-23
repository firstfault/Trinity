package me.f1nal.trinity.execution.packages;

import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.execution.ClassTarget;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

/**
 * A persisted top-level input container. JAR containers export independently;
 * the single loose container owns raw class and resource inputs.
 */
public final class ProjectContainer {
    private final UUID id;
    private final ProjectContainerKind kind;
    private String name;
    private final Package rootPackage;
    private final Set<ClassTarget> classes = new LinkedHashSet<>();
    private final Map<String, ResourceArchiveEntry> resources = new LinkedHashMap<>();
    private final List<ArchiveDirectoryEntry> directories = new ArrayList<>();
    private String archiveComment;
    private int nextEntryOrder;

    public ProjectContainer(UUID id, String name, ProjectContainerKind kind, Database database) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.rootPackage = new Package(this, database);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) return;
        this.name = name;
    }

    public ProjectContainerKind getKind() {
        return kind;
    }

    public boolean isJar() {
        return kind == ProjectContainerKind.JAR;
    }

    public Package getRootPackage() {
        return rootPackage;
    }

    public Collection<ClassTarget> getClasses() {
        return Collections.unmodifiableSet(classes);
    }

    public Collection<ResourceArchiveEntry> getResources() {
        return Collections.unmodifiableCollection(resources.values());
    }

    public List<ArchiveDirectoryEntry> getDirectories() {
        return Collections.unmodifiableList(directories);
    }

    public void addDirectory(ArchiveDirectoryEntry directory) {
        directories.add(directory);
        observeOrder(directory.getMetadata());
    }

    public String getArchiveComment() {
        return archiveComment;
    }

    public void setArchiveComment(String archiveComment) {
        this.archiveComment = archiveComment == null || archiveComment.isEmpty() ? null : archiveComment;
    }

    public ResourceArchiveEntry getResource(String path) {
        return resources.get(path);
    }

    public void register(ArchiveEntry entry) {
        if (entry.getContainer() == null) entry.assignContainer(this);
        if (entry.getContainer() != this) {
            throw new IllegalArgumentException("Entry belongs to a different project container");
        }
        if (entry.getZipMetadata().getOrder() == Integer.MAX_VALUE) {
            entry.getZipMetadata().setOrder(nextEntryOrder++);
        } else {
            observeOrder(entry.getZipMetadata());
        }
        if (entry instanceof ClassTarget target) {
            classes.add(target);
        } else if (entry instanceof ResourceArchiveEntry resource) {
            ResourceArchiveEntry conflict = resources.putIfAbsent(resource.getRealName(), resource);
            if (conflict != null && conflict != resource) {
                throw new IllegalArgumentException("Resource already exists in " + name + ": " + resource.getRealName());
            }
        }
    }

    private void observeOrder(ZipEntryMetadata metadata) {
        if (metadata.getOrder() != Integer.MAX_VALUE) {
            nextEntryOrder = Math.max(nextEntryOrder, metadata.getOrder() + 1);
        }
    }

    public void refreshEntryOrderCounter() {
        nextEntryOrder = 0;
        classes.forEach(entry -> observeOrder(entry.getZipMetadata()));
        resources.values().forEach(entry -> observeOrder(entry.getZipMetadata()));
        directories.forEach(entry -> observeOrder(entry.getMetadata()));
    }

    public void unregister(ArchiveEntry entry) {
        if (entry instanceof ClassTarget target) {
            classes.remove(target);
        } else if (entry instanceof ResourceArchiveEntry resource) {
            resources.remove(resource.getRealName(), resource);
        }
    }

    public void reindexResource(ResourceArchiveEntry resource, String oldName) {
        resources.remove(oldName, resource);
        ResourceArchiveEntry conflict = resources.putIfAbsent(resource.getRealName(), resource);
        if (conflict != null && conflict != resource) {
            resources.put(oldName, resource);
            throw new IllegalArgumentException("Resource already exists in " + name + ": " + resource.getRealName());
        }
    }
}
