package me.f1nal.trinity.execution.dependency;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;

/**
 * A dependency reference stored inside the Trinity database.
 *
 * <p>The locator metadata is persisted. Resolved class bytes are an in-memory
 * cache populated when the database is open and are never serialized.</p>
 */
public final class DependencyArchive {
    private final UUID id;
    private final String name;
    private final DependencyKind kind;
    private final String relativePath;
    private final String absolutePath;
    private final String runtimeModule;
    private Map<String, byte[]> classes = Map.of();
    private long loadedSize;
    private String resolvedLocation;
    private String resolutionError = "Dependency has not been resolved";

    public DependencyArchive(UUID id, String name, DependencyKind kind,
                             String relativePath, String absolutePath, String runtimeModule) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = requireName(name);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.relativePath = blankToNull(relativePath);
        this.absolutePath = blankToNull(absolutePath);
        this.runtimeModule = blankToNull(runtimeModule);
        if (kind == DependencyKind.RUNTIME_MODULE && this.runtimeModule == null) {
            throw new IllegalArgumentException("Runtime dependency must name a module");
        }
        if (kind == DependencyKind.RUNTIME_MODULE
                && (this.runtimeModule.contains("/") || this.runtimeModule.contains("\\")
                || this.runtimeModule.startsWith(".") || this.runtimeModule.endsWith(".")
                || this.runtimeModule.contains(".."))) {
            throw new IllegalArgumentException("Invalid runtime module name: " + this.runtimeModule);
        }
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Dependency archive name cannot be blank");
        }
        return name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    void setResolved(Map<String, byte[]> classes, String resolvedLocation) {
        if (classes == null || classes.isEmpty()) {
            throw new IllegalArgumentException("Resolved dependency must contain at least one class");
        }
        LinkedHashMap<String, byte[]> copy = new LinkedHashMap<>();
        long size = 0L;
        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            byte[] bytes = Objects.requireNonNull(entry.getValue(), "class bytes");
            copy.put(Objects.requireNonNull(entry.getKey(), "class name"), bytes);
            size += bytes.length;
        }
        this.classes = Collections.unmodifiableMap(copy);
        this.loadedSize = size;
        this.resolvedLocation = resolvedLocation;
        this.resolutionError = null;
    }

    void setResolutionError(String resolutionError) {
        this.classes = Map.of();
        this.loadedSize = 0L;
        this.resolvedLocation = null;
        this.resolutionError = resolutionError == null || resolutionError.isBlank()
                ? "Dependency could not be resolved" : resolutionError;
    }

    public Map<String, byte[]> getClasses() {
        return classes;
    }

    public int getClassCount() {
        return classes.size();
    }

    public long getLoadedSize() {
        return loadedSize;
    }

    public DependencyKind getKind() {
        return kind;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public String getRuntimeModule() {
        return runtimeModule;
    }

    public boolean isResolved() {
        return resolutionError == null;
    }

    public String getResolvedLocation() {
        return resolvedLocation;
    }

    public String getResolutionError() {
        return resolutionError;
    }

    public String getStoredReference() {
        if (kind == DependencyKind.RUNTIME_MODULE) return "runtime:" + runtimeModule;
        if (relativePath != null) return relativePath;
        return absolutePath == null ? "(location required)" : absolutePath;
    }
}
