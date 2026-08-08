package me.f1nal.trinity.database.inputs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Encodes nested APKM paths in project state and reconstructs their ZIP hierarchy on export. */
public final class ApkmArchiveLayout {
    public static final String ARCHIVE_SEPARATOR = "!/";

    private ApkmArchiveLayout() {
    }

    public static String entry(String bundleName, String relativePath) {
        return bundleName + ARCHIVE_SEPARATOR + relativePath;
    }

    public static boolean isBundleEntry(String path) {
        int separator = path.indexOf(ARCHIVE_SEPARATOR);
        return separator > 0 && path.substring(0, separator)
                .toLowerCase(Locale.ROOT).endsWith(".apkm");
    }

    /** Converts encoded bundle paths into one APKM root, or nested APKM files for mixed projects. */
    public static Map<String, byte[]> materialize(Map<String, byte[]> encodedEntries)
            throws IOException {
        Map<String, byte[]> plain = new TreeMap<>();
        Map<String, Map<String, byte[]>> bundles = new TreeMap<>();
        for (Map.Entry<String, byte[]> entry : encodedEntries.entrySet()) {
            String path = entry.getKey();
            if (!isBundleEntry(path)) {
                putUnique(plain, path, entry.getValue());
                continue;
            }
            int separator = path.indexOf(ARCHIVE_SEPARATOR);
            String bundleName = path.substring(0, separator);
            String relativePath = path.substring(separator + ARCHIVE_SEPARATOR.length());
            putUnique(bundles.computeIfAbsent(bundleName, ignored -> new TreeMap<>()),
                    relativePath, entry.getValue());
        }
        if (bundles.isEmpty()) return new LinkedHashMap<>(plain);
        if (bundles.size() == 1 && plain.isEmpty()) {
            return materializePaths(bundles.values().iterator().next());
        }
        for (Map.Entry<String, Map<String, byte[]>> bundle : bundles.entrySet()) {
            putUnique(plain, bundle.getKey(), zip(materializePaths(bundle.getValue())));
        }
        return new LinkedHashMap<>(plain);
    }

    private static Map<String, byte[]> materializePaths(Map<String, byte[]> paths)
            throws IOException {
        Map<String, byte[]> files = new TreeMap<>();
        Map<String, Map<String, byte[]>> children = new TreeMap<>();
        for (Map.Entry<String, byte[]> entry : paths.entrySet()) {
            int separator = entry.getKey().indexOf(ARCHIVE_SEPARATOR);
            if (separator < 0) {
                putUnique(files, entry.getKey(), entry.getValue());
                continue;
            }
            String childName = entry.getKey().substring(0, separator);
            String childPath = entry.getKey().substring(separator + ARCHIVE_SEPARATOR.length());
            putUnique(children.computeIfAbsent(childName, ignored -> new TreeMap<>()),
                    childPath, entry.getValue());
        }
        for (Map.Entry<String, Map<String, byte[]>> child : children.entrySet()) {
            putUnique(files, child.getKey(), zip(materializePaths(child.getValue())));
        }
        return new LinkedHashMap<>(files);
    }

    private static byte[] zip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream output = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipEntry.setTime(0L);
                output.putNextEntry(zipEntry);
                output.write(entry.getValue());
                output.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static void putUnique(Map<String, byte[]> entries, String name, byte[] bytes) {
        if (name.isBlank()) throw new IllegalArgumentException("Archive entry must not be blank");
        if (entries.putIfAbsent(name, bytes) != null) {
            throw new IllegalArgumentException("Archive entry collision: " + name);
        }
    }
}
