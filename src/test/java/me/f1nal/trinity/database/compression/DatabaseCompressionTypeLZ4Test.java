package me.f1nal.trinity.database.compression;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class DatabaseCompressionTypeLZ4Test {
    @Test
    void roundTripsDatabasePayload() throws IOException {
        byte[] payload = "Trinity database payload".repeat(100).getBytes();
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        DatabaseCompressionTypeLZ4 compression = new DatabaseCompressionTypeLZ4();

        compression.compress(compressed, payload);

        assertArrayEquals(payload, compression.decompress(
                new ByteArrayInputStream(compressed.toByteArray())));
    }
}
