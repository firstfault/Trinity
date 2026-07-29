package me.f1nal.trinity.execution.dependency;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Ordered dependency classpath for one Trinity project. */
public final class DependencyManager {
    private final List<DependencyArchive> archives = new ArrayList<>();
    private final Map<String, byte[]> classBytes = new LinkedHashMap<>();
    private final Map<String, ClassNode> parsedClasses = new HashMap<>();

    /**
     * Adds an archive at the end of the classpath. When archives contain the
     * same class, the first archive wins, matching normal classpath ordering.
     */
    public synchronized void addArchive(DependencyArchive archive) {
        if (archives.stream().anyMatch(existing -> existing.getId().equals(archive.getId()))) {
            throw new IllegalArgumentException("Dependency archive already exists: " + archive.getId());
        }
        archives.add(archive);
        archive.getClasses().forEach(classBytes::putIfAbsent);
    }

    public synchronized boolean removeArchive(DependencyArchive archive) {
        if (!archives.remove(archive)) return false;
        rebuildIndex();
        return true;
    }

    public synchronized void replaceArchive(DependencyArchive previous, DependencyArchive replacement) {
        int index = archives.indexOf(previous);
        if (index == -1) throw new IllegalArgumentException("Dependency archive is not registered");
        if (!previous.getId().equals(replacement.getId())) {
            throw new IllegalArgumentException("Replacement dependency must preserve its ID");
        }
        archives.set(index, replacement);
        rebuildIndex();
    }

    private void rebuildIndex() {
        classBytes.clear();
        parsedClasses.clear();
        for (DependencyArchive archive : archives) {
            archive.getClasses().forEach(classBytes::putIfAbsent);
        }
    }

    public synchronized List<DependencyArchive> getArchives() {
        return List.copyOf(archives);
    }

    public synchronized @Nullable DependencyArchive getArchive(UUID id) {
        return archives.stream().filter(archive -> archive.getId().equals(id)).findFirst().orElse(null);
    }

    public synchronized boolean containsClass(String internalName) {
        return classBytes.containsKey(internalName);
    }

    public synchronized @Nullable ClassNode getClass(String internalName) {
        ClassNode cached = parsedClasses.get(internalName);
        if (cached != null) return cached;

        byte[] bytes = classBytes.get(internalName);
        if (bytes == null) return null;
        ClassNode classNode = new ClassNode();
        new ClassReader(bytes).accept(classNode,
                ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        parsedClasses.put(internalName, classNode);
        return classNode;
    }
}
