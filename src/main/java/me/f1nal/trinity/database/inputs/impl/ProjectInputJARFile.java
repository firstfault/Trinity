package me.f1nal.trinity.database.inputs.impl;

import me.f1nal.trinity.database.inputs.AbstractProjectInputFile;
import me.f1nal.trinity.database.inputs.UnreadClassBytes;
import me.f1nal.trinity.database.inputs.UnreadDexBytes;
import me.f1nal.trinity.util.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Locale;

public class ProjectInputJARFile extends AbstractProjectInputFile {
    static final int MAX_ARCHIVE_ENTRY_BYTES = 256 * 1024 * 1024;
    static final int MAX_ARCHIVE_CONTENT_BYTES = 512 * 1024 * 1024;

    public ProjectInputJARFile(File file, byte[] bytes) throws IOException {
        super(file);
        this.readZipFile(bytes);
    }

    private void readZipFile(byte[] bytes) throws IOException {
        boolean hasEntry = false;
        int totalSize = 0;
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry jarEntry;
            while ((jarEntry = zipInputStream.getNextEntry()) != null) {
                String entryName = cleanEntryName(jarEntry.getName());
                int remainingBytes = MAX_ARCHIVE_CONTENT_BYTES - totalSize;
                int entryLimit = Math.min(MAX_ARCHIVE_ENTRY_BYTES, remainingBytes);
                byte[] entryBytes = FileUtil.readAllBytes(
                        zipInputStream, entryLimit, "ZIP entry '" + entryName + "'");
                totalSize += entryBytes.length;
                hasEntry = true;

                if (!jarEntry.isDirectory() || entryBytes.length != 0) {
                    String lowerEntryName = entryName.toLowerCase(Locale.ROOT);
                    if (lowerEntryName.endsWith(".class")) {
                        this.getClassPath().getClasses().add(new UnreadClassBytes(entryName, entryBytes));
                    } else if (lowerEntryName.endsWith(".dex")) {
                        String dexName = String.format("%s!/%s", getName(), entryName);
                        this.getClassPath().getDexFiles().add(new UnreadDexBytes(dexName, entryBytes));
                    } else {
                        this.getClassPath().putResource(entryName, entryBytes);
                    }
                }

                zipInputStream.closeEntry();
            }
        }
        if (!hasEntry) {
            throw new IOException("Empty ZIP file");
        }
    }

    private static String cleanEntryName(String name) {
        while (name.endsWith("/")) name = name.substring(0, name.length() - 1);
        return name;
    }

}
