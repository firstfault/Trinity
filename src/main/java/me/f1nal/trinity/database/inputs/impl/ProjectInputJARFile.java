package me.f1nal.trinity.database.inputs.impl;

import me.f1nal.trinity.database.inputs.AbstractProjectInputFile;
import me.f1nal.trinity.database.inputs.UnreadClassBytes;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.util.UnsafeUtil;
import sun.misc.Unsafe;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ProjectInputJARFile extends AbstractProjectInputFile {
    public ProjectInputJARFile(File file, byte[] bytes) throws IOException {
        super(file);
        this.readZipFile(bytes);
    }

    private void readZipFile(byte[] bytes) throws IOException {
        boolean hasEntry = false;
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            final boolean crcPatched = patchZipStreamCrc(zipInputStream);
            ZipEntry jarEntry;

            while ((jarEntry = zipInputStream.getNextEntry()) != null) {
                long crc = jarEntry.getCrc();
                String entryName = cleanEntryName(jarEntry.getName());

                if (crcPatched) jarEntry.setCrc(0L);
                byte[] entryBytes = zipInputStream.readAllBytes();
                if (crcPatched)
                    try {
                        jarEntry.setCrc(crc);
                    } catch (IllegalArgumentException illegalArgumentException) {
                        // welp?
                    }

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

    private static long CRC_FIELD_OFFSET = -1L;
    private static final ZeroCRC32 zeroCrc32 = new ZeroCRC32();

    private static boolean patchZipStreamCrc(ZipInputStream zipInputStream) {
        if (CRC_FIELD_OFFSET == -1L) {
            return false;
        }

        try {
            Unsafe unsafe = UnsafeUtil.getUnsafe();
            unsafe.putObject(zipInputStream, CRC_FIELD_OFFSET, zeroCrc32);
            return true;
        } catch (Throwable throwable) {
            Logging.warn("Failed to set CRC field!");
            return false;
        }
    }

    private static class ZeroCRC32 extends CRC32 {
        @Override
        public long getValue() {
            return 0L;
        }
    }

    static {
        try {
            CRC_FIELD_OFFSET = UnsafeUtil.getUnsafe().objectFieldOffset(ZipInputStream.class.getDeclaredField("crc"));
        } catch (Throwable e) {
            Logging.warn("Failed to retrieve CRC field, may not be able to read certain ZIP files!");
        }
    }
}
