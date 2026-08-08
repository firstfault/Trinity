package me.f1nal.trinity.database.inputs.impl;

import me.f1nal.trinity.database.inputs.AbstractProjectInputFile;
import me.f1nal.trinity.database.inputs.UnreadClassBytes;
import me.f1nal.trinity.database.inputs.UnreadDexBytes;
import me.f1nal.trinity.execution.packages.ArchiveDirectoryEntry;
import me.f1nal.trinity.execution.packages.ProjectContainerKind;
import me.f1nal.trinity.execution.packages.ZipEntryMetadata;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.util.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ProjectInputJARFile extends AbstractProjectInputFile {
    static final int MAX_ARCHIVE_ENTRY_BYTES = 256 * 1024 * 1024;
    static final int MAX_ARCHIVE_CONTENT_BYTES = 512 * 1024 * 1024;

    public ProjectInputJARFile(File file, byte[] bytes) throws IOException {
        super(file);
        this.readZipFile(file, bytes);
    }

    @Override
    public ProjectContainerKind getContainerKind() {
        return ProjectContainerKind.JAR;
    }

    private void readZipFile(File file, byte[] bytes) throws IOException {
        try {
            readCentralDirectory(file);
            return;
        } catch (IOException exception) {
            this.getClassPath().clear();
            Logging.warn("Could not read ZIP central directory for {}, falling back to stream input: {}",
                    file.getName(), exception.getMessage());
        }
        readZipStream(bytes);
    }

    private void readCentralDirectory(File file) throws IOException {
        boolean hasEntry = false;
        int totalSize = 0;
        Set<String> entryNames = new HashSet<>();
        try (ZipFile zipFile = new ZipFile(file)) {
            this.getClassPath().setArchiveComment(zipFile.getComment());
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            int order = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = cleanEntryName(entry.getName(), entry.isDirectory());
                if (!entryNames.add(entryName)) throw new IOException("Duplicate ZIP entry: " + entryName);
                byte[] entryBytes;
                try (InputStream stream = zipFile.getInputStream(entry)) {
                    int remainingBytes = MAX_ARCHIVE_CONTENT_BYTES - totalSize;
                    int entryLimit = Math.min(MAX_ARCHIVE_ENTRY_BYTES, remainingBytes);
                    entryBytes = FileUtil.readAllBytes(
                            stream, entryLimit, "ZIP entry '" + entryName + "'");
                }
                totalSize += entryBytes.length;
                if (!entryName.isEmpty()) addEntry(entryName, entryBytes, entry, order++);
                hasEntry = true;
            }
        }
        if (!hasEntry) throw new IOException("Empty ZIP file");
    }

    private void readZipStream(byte[] bytes) throws IOException {
        boolean hasEntry = false;
        int totalSize = 0;
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry jarEntry;
            Set<String> entryNames = new HashSet<>();
            int order = 0;
            while ((jarEntry = zipInputStream.getNextEntry()) != null) {
                String entryName = cleanEntryName(jarEntry.getName(), jarEntry.isDirectory());
                if (!entryName.isEmpty() && !entryNames.add(entryName)) {
                    throw new IOException("Duplicate ZIP entry: " + entryName);
                }
                int remainingBytes = MAX_ARCHIVE_CONTENT_BYTES - totalSize;
                int entryLimit = Math.min(MAX_ARCHIVE_ENTRY_BYTES, remainingBytes);
                byte[] entryBytes = FileUtil.readAllBytes(
                        zipInputStream, entryLimit, "ZIP entry '" + entryName + "'");
                totalSize += entryBytes.length;
                hasEntry = true;

                if (!entryName.isEmpty()) addEntry(entryName, entryBytes, jarEntry, order++);

                zipInputStream.closeEntry();
            }
        }
        if (!hasEntry) {
            throw new IOException("Empty ZIP file");
        }
    }

    private void addEntry(String entryName, byte[] bytes, ZipEntry entry, int order) {
        ZipEntryMetadata metadata = ZipEntryMetadata.fromZipEntry(entry, order);
        String lowerEntryName = entryName.toLowerCase(Locale.ROOT);
        if (entry.isDirectory() && bytes.length == 0) {
            this.getClassPath().getDirectories().add(new ArchiveDirectoryEntry(entryName, metadata));
        } else if (lowerEntryName.endsWith(".class") && !entryName.startsWith("META-INF/versions/")) {
            this.getClassPath().addClass(new UnreadClassBytes(entryName, bytes, metadata, false));
        } else if (lowerEntryName.endsWith(".dex")) {
            String dexName = String.format("%s!/%s", getName(), entryName);
            this.getClassPath().getDexFiles().add(new UnreadDexBytes(dexName, bytes));
        } else {
            this.getClassPath().putResource(entryName, bytes, metadata);
        }
    }

    private static String cleanEntryName(String name, boolean directory) throws IOException {
        name = name.replace('\\', '/');
        while (name.endsWith("/")) name = name.substring(0, name.length() - 1);
        if (name.startsWith("/") || name.equals("..") || name.startsWith("../")
                || name.endsWith("/..") || name.contains("/../")) {
            throw new IOException("Unsafe ZIP entry path: " + name);
        }
        return directory && !name.isEmpty() ? name + "/" : name;
    }

}
