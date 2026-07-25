package me.f1nal.trinity.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class FileUtil {
    private FileUtil() {
    }

    public static String normalizeFileName(String fileName) {
        fileName = fileName.replace("\\", "");
        fileName = fileName.replace("/", "");
        fileName = fileName.replace(".", "");
        return fileName;
    }

    public static byte[] readAllBytes(File file, int maximumBytes, String description) throws IOException {
        if (file.length() > maximumBytes) {
            throw tooLarge(description, maximumBytes);
        }
        try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
            return readAllBytes(input, maximumBytes, description);
        }
    }

    public static byte[] readAllBytes(InputStream input, int maximumBytes, String description) throws IOException {
        if (maximumBytes < 0 || maximumBytes == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("maximumBytes");
        }
        byte[] bytes = input.readNBytes(maximumBytes + 1);
        if (bytes.length > maximumBytes) {
            throw tooLarge(description, maximumBytes);
        }
        return bytes;
    }

    private static IOException tooLarge(String description, int maximumBytes) {
        return new IOException(description + " exceeds the " + maximumBytes + "-byte limit");
    }
}
