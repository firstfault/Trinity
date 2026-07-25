package me.f1nal.trinity.database;

import me.f1nal.trinity.database.inputs.UnreadClassBytes;
import me.f1nal.trinity.database.inputs.UnreadDexBytes;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.util.ByteUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClassPath {
    public List<UnreadClassBytes> classes = new ArrayList<>();
    public List<UnreadDexBytes> dexFiles = new ArrayList<>();
    public Map<String, byte[]> resources = new HashMap<>();
    /**
     * Warnings related to class path loading.
     */
    private int warnings;

    public ClassPath() {

    }


    public List<byte[]> createClassByteList() {
        return this.classes.stream().map(UnreadClassBytes::getBytes).collect(Collectors.toCollection(() -> new ArrayList<>(this.classes.size())));
    }

    public List<UnreadDexBytes> getDexFiles() {
        return dexFiles;
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

    public void addClassPath(ClassPath classPath) {
        this.getClasses().addAll(classPath.getClasses());
        this.getDexFiles().addAll(classPath.getDexFiles());
        this.getResources().putAll(classPath.getResources());
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
        this.getDexFiles().clear();
        this.getResources().clear();
        this.warnings = 0;
    }

    public void putResource(String entryName, byte[] entryBytes) {
        final byte[] currentResource = this.getResources().get(entryName);

        if (currentResource != null) {
            Logging.warn("Double resource add: {} ({}) collides with existing resource ({})", entryName, ByteUtil.getHumanReadableByteCountSI(entryBytes.length), ByteUtil.getHumanReadableByteCountSI(currentResource.length));
            this.addWarning();
        }

        this.getResources().put(entryName, entryBytes);
    }
}
