package me.f1nal.trinity.database.inputs.impl;

import me.f1nal.trinity.database.inputs.AbstractProjectInputFile;
import me.f1nal.trinity.database.inputs.UnreadClassBytes;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.util.UnsafeUtil;
import sun.misc.Unsafe;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.CRC32;
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
            ZeroCRC32 zeroCRC32 = new ZeroCRC32();
            patchZipStreamCrc(zeroCRC32, zipInputStream);
            ZipEntry jarEntry;

            while ((jarEntry = zipInputStream.getNextEntry()) != null) {
                String entryName = cleanEntryName(jarEntry.getName());

                zeroCRC32.setZipEntry(jarEntry);
                byte[] entryBytes = zipInputStream.readAllBytes();
                zeroCRC32.setZipEntry(null);

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
