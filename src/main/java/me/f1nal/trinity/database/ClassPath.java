package me.f1nal.trinity.database;

import me.f1nal.trinity.database.inputs.UnreadClassBytes;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.util.ByteUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import me.f1nal.trinity.execution.packages.ArchiveDirectoryEntry;
import me.f1nal.trinity.execution.packages.ZipEntryMetadata;

public class ClassPath {
    private final List<UnreadClassBytes> classes = new ArrayList<>();
    private final Map<String, byte[]> resources = new LinkedHashMap<>();
    private final Map<String, ZipEntryMetadata> resourceMetadata = new HashMap<>();
    private final List<ArchiveDirectoryEntry> directories = new ArrayList<>();
    private String archiveComment;
    /**
     * Warnings related to class path loading.
     */
    private int warnings;

    public ClassPath() {

    }

    public ClassPath(ZipInputStream zipInputStream) throws IOException {
        ZipEntry jarEntry;
        while ((jarEntry = zipInputStream.getNextEntry()) != null) {
            String entryName = jarEntry.getName();
            byte[] entryBytes = zipInputStream.readAllBytes();

            if (!jarEntry.isDirectory()) {
                if (entryName.endsWith(".class")) {
                    classes.add(new UnreadClassBytes(entryName, entryBytes,
                            ZipEntryMetadata.fromZipEntry(jarEntry, classes.size() + resources.size()), false));
                } else {
                    putResource(entryName, entryBytes,
                            ZipEntryMetadata.fromZipEntry(jarEntry, classes.size() + resources.size()));
                }
            }

            zipInputStream.closeEntry();
        }
    }

    public List<byte[]> createClassByteList() {
        return this.classes.stream().map(UnreadClassBytes::getBytes).collect(Collectors.toCollection(() -> new ArrayList<>(this.classes.size())));
    }

    public void addClass(UnreadClassBytes classBytes) {
        this.classes.add(classBytes);
    }

    public List<UnreadClassBytes> getClasses() {
        return classes;
    }

    public Map<String, byte[]> getResources() {
        return resources;
    }

    public ZipEntryMetadata getResourceMetadata(String name) {
        return resourceMetadata.getOrDefault(name, ZipEntryMetadata.createDefault());
    }

    public List<ArchiveDirectoryEntry> getDirectories() {
        return directories;
    }

    public String getArchiveComment() {
        return archiveComment;
    }

    public void setArchiveComment(String archiveComment) {
        this.archiveComment = archiveComment;
    }

    public void addClassPath(ClassPath classPath) {
        this.getClasses().addAll(classPath.getClasses());
        classPath.getResources().forEach((name, bytes) ->
                this.putResource(name, bytes, classPath.getResourceMetadata(name)));
        classPath.getDirectories().forEach(directory -> this.directories.add(
                new ArchiveDirectoryEntry(directory.getName(), directory.getMetadata().copy())));
        this.warnings += classPath.warnings;
    }

    public void addWarning() {
        ++this.warnings;
    }

    public int getWarnings() {
        return warnings;
    }

    public void clear() {
        this.getClasses().clear();
        this.getResources().clear();
        this.resourceMetadata.clear();
        this.directories.clear();
        this.archiveComment = null;
        this.warnings = 0;
    }

    public void putResource(String entryName, byte[] entryBytes) {
        putResource(entryName, entryBytes, ZipEntryMetadata.createDefault());
    }

    public void putResource(String entryName, byte[] entryBytes, ZipEntryMetadata metadata) {
        final byte[] currentResource = this.getResources().get(entryName);

        if (currentResource != null) {
            Logging.warn("Double resource add: {} ({}) collides with existing resource ({})", entryName, ByteUtil.getHumanReadableByteCountSI(entryBytes.length), ByteUtil.getHumanReadableByteCountSI(currentResource.length));
            this.addWarning();
        }

        this.getResources().put(entryName, entryBytes);
        this.resourceMetadata.put(entryName, metadata == null ? ZipEntryMetadata.createDefault() : metadata);
    }
}
