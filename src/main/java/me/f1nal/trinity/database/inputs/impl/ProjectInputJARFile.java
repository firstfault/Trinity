package me.f1nal.trinity.database.inputs.impl;

import me.f1nal.trinity.database.inputs.AbstractProjectInputFile;
import me.f1nal.trinity.database.inputs.UnreadClassBytes;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.execution.packages.ProjectContainerKind;
import me.f1nal.trinity.execution.packages.ArchiveDirectoryEntry;
import me.f1nal.trinity.execution.packages.ZipEntryMetadata;
import me.f1nal.trinity.util.UnsafeUtil;
import sun.misc.Unsafe;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipFile;
import java.util.Enumeration;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class ProjectInputJARFile extends AbstractProjectInputFile {
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
                    entryBytes = stream.readAllBytes();
                }
                if (!entryName.isEmpty()) addEntry(entryName, entryBytes, entry, order++);
                hasEntry = true;
            }
        }
        if (!hasEntry) throw new IOException("Empty ZIP file");
    }

    private void readZipStream(byte[] bytes) throws IOException {
        boolean hasEntry = false;
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZeroCRC32 zeroCRC32 = new ZeroCRC32();
            patchZipStreamCrc(zeroCRC32, zipInputStream);
            ZipEntry jarEntry;
            Set<String> entryNames = new HashSet<>();

            int order = 0;
            while ((jarEntry = zipInputStream.getNextEntry()) != null) {
                String entryName = cleanEntryName(jarEntry.getName(), jarEntry.isDirectory());
                if (!entryName.isEmpty() && !entryNames.add(entryName)) {
                    throw new IOException("Duplicate ZIP entry: " + entryName);
                }

                zeroCRC32.setZipEntry(jarEntry);
                byte[] entryBytes = zipInputStream.readAllBytes();
                zeroCRC32.setZipEntry(null);

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
        if (entry.isDirectory() && bytes.length == 0) {
            this.getClassPath().getDirectories().add(new ArchiveDirectoryEntry(entryName, metadata));
        } else if (entryName.endsWith(".class") && !entryName.startsWith("META-INF/versions/")) {
            this.getClassPath().addClass(new UnreadClassBytes(entryName, bytes, metadata, false));
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


    private static void patchZipStreamCrc(ZeroCRC32 zeroCRC32, ZipInputStream zipInputStream) {
        if (CRC_FIELD_OFFSET == -1L) {
            return;
        }

        try {
            Unsafe unsafe = UnsafeUtil.getUnsafe();
            unsafe.putObject(zipInputStream, CRC_FIELD_OFFSET, zeroCRC32);
        } catch (Throwable throwable) {
            Logging.warn("Failed to set CRC field! {}", throwable);
        }
    }

    private static class ZeroCRC32 extends CRC32 {
        private ZipEntry zipEntry;

        @Override
        public long getValue() {
            return zipEntry != null ? zipEntry.getCrc() : super.getValue();
        }

        public void setZipEntry(ZipEntry zipEntry) {
            this.zipEntry = zipEntry;
        }
    }

    private static long CRC_FIELD_OFFSET = -1L;

    static {
        try {
            CRC_FIELD_OFFSET = UnsafeUtil.getUnsafe().objectFieldOffset(ZipInputStream.class.getDeclaredField("crc"));
        } catch (Throwable e) {
            Logging.warn("Failed to retrieve CRC field, may not be able to read certain ZIP files! {}", e);
        }
    }
}
