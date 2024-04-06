package me.f1nal.trinity.database;

import me.f1nal.trinity.database.inputs.UnreadClassBytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ClassPath {
    public List<UnreadClassBytes> classes = new ArrayList<>();
    public Map<String, byte[]> resources = new HashMap<>();

    public ClassPath() {

    }

    public ClassPath(ZipInputStream zipInputStream) throws IOException {
        ZipEntry jarEntry;
        while ((jarEntry = zipInputStream.getNextEntry()) != null) {
            String entryName = jarEntry.getName();
            byte[] entryBytes = zipInputStream.readAllBytes();

            if (!jarEntry.isDirectory()) {
                if (entryName.endsWith(".class")) {
                    classes.add(new UnreadClassBytes(entryName, entryBytes));
                } else {
                    resources.put(entryName, entryBytes);
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

    public void addClassPath(ClassPath classPath) {
        this.getClasses().addAll(classPath.getClasses());
        this.getResources().putAll(classPath.getResources());
    }
}
