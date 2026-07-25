package me.f1nal.trinity.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileUtilTest {
    @Test
    void acceptsInputAtLimit() throws IOException {
        byte[] input = {1, 2, 3, 4};

        assertArrayEquals(input, FileUtil.readAllBytes(
                new ByteArrayInputStream(input), input.length, "Test input"));
    }

    @Test
    void rejectsStreamAboveLimit() {
        byte[] input = {1, 2, 3, 4, 5};

        assertThrows(IOException.class, () -> FileUtil.readAllBytes(
                new ByteArrayInputStream(input), input.length - 1, "Test input"));
    }

    @Test
    void rejectsFileAboveLimit(@TempDir Path directory) throws IOException {
        Path input = directory.resolve("oversized.bin");
        Files.write(input, new byte[] {1, 2, 3, 4, 5});

        assertThrows(IOException.class, () -> FileUtil.readAllBytes(
                input.toFile(), 4, "Test file"));
    }
}
