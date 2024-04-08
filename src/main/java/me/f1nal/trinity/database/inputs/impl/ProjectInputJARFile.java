package me.f1nal.trinity.database.inputs.impl;

import me.f1nal.trinity.database.inputs.AbstractProjectInputFile;
import me.f1nal.trinity.database.inputs.UnreadClassBytes;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ProjectInputJARFile extends AbstractProjectInputFile {
    public ProjectInputJARFile(File file, byte[] bytes) throws IOException {
        super(file);
        this.readZipFile(bytes);
    }

    private void readZipFile(byte[] bytes) throws IOException {
        boolean hasEntry = false;
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry jarEntry;
            while ((jarEntry = zipInputStream.getNextEntry()) != null) {
                String entryName = cleanEntryName(jarEntry.getName());
                byte[] entryBytes = zipInputStream.readAllBytes();

                hasEntry = true;

                if (!jarEntry.isDirectory() || entryBytes.length != 0) {
                    if (entryName.endsWith(".class")) {
                        this.getClassPath().getClasses().add(new UnreadClassBytes(entryName, entryBytes));
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
