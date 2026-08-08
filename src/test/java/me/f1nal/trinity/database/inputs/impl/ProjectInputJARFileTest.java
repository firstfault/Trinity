package me.f1nal.trinity.database.inputs.impl;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectInputJARFileTest {
    @Test
    void rejectsEntriesWithInvalidCrc() throws IOException {
        byte[] payload = "trinity-crc-payload".getBytes();
        byte[] archive = storedArchive(payload);
        int payloadOffset = indexOf(archive, payload);
        archive[payloadOffset] ^= 1;

        assertThrows(IOException.class,
                () -> new ProjectInputJARFile(new File("invalid-crc.jar"), archive));
    }

    private static byte[] storedArchive(byte[] payload) throws IOException {
        CRC32 crc = new CRC32();
        crc.update(payload);

        ZipEntry entry = new ZipEntry("payload.bin");
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(payload.length);
        entry.setCompressedSize(payload.length);
        entry.setCrc(crc.getValue());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream output = new ZipOutputStream(bytes)) {
            output.putNextEntry(entry);
            output.write(payload);
            output.closeEntry();
        }
        return bytes.toByteArray();
    }

    private static int indexOf(byte[] bytes, byte[] target) {
        for (int i = 0; i <= bytes.length - target.length; i++) {
            int j = 0;
            while (j < target.length && bytes[i + j] == target[j]) j++;
            if (j == target.length) return i;
        }
        throw new AssertionError("Payload not found in stored ZIP");
    }
}
