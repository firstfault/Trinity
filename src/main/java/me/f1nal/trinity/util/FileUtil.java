package me.f1nal.trinity.util;

public class FileUtil {
    public static String normalizeFileName(String fileName) {
        fileName = fileName.replace("\\", "");
        fileName = fileName.replace("/", "");
        fileName = fileName.replace(".", "");
        return fileName;
    }
}
