package me.f1nal.trinity.database.inputs.impl;

import me.f1nal.trinity.database.ClassPath;
import me.f1nal.trinity.database.inputs.AbstractProjectInputFile;
import me.f1nal.trinity.database.inputs.ApkmArchiveLayout;
import me.f1nal.trinity.database.inputs.UnreadClassBytes;
import me.f1nal.trinity.database.inputs.UnreadDexBytes;
import me.f1nal.trinity.util.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** APKMirror bundle containing a base APK, optional split APKs, and bundle metadata. */
public final class ProjectInputAPKMFile extends AbstractProjectInputFile {
    public ProjectInputAPKMFile(File file, byte[] bytes) throws IOException {
        super(file);
        readBundle(bytes);
    }

    private void readBundle(byte[] bytes) throws IOException {
        boolean hasEntry = false;
        boolean hasBaseApk = false;
        int apkCount = 0;
        int totalSize = 0;
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry zipEntry;
            while ((zipEntry = input.getNextEntry()) != null) {
                String entryName = cleanEntryName(zipEntry.getName());
                int remainingBytes = ProjectInputJARFile.MAX_ARCHIVE_CONTENT_BYTES - totalSize;
                int entryLimit = Math.min(ProjectInputJARFile.MAX_ARCHIVE_ENTRY_BYTES,
                        remainingBytes);
                byte[] entryBytes = FileUtil.readAllBytes(input, entryLimit,
                        "APKM entry '" + entryName + "'");
                totalSize += entryBytes.length;
                hasEntry = true;

                if (!zipEntry.isDirectory() || entryBytes.length != 0) {
                    if (entryName.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                        ProjectInputJARFile apk = new ProjectInputJARFile(
                                new File(entryName), entryBytes);
                        addApk(entryName, apk.getClassPath());
                        apkCount++;
                        if (entryName.equalsIgnoreCase("base.apk")) hasBaseApk = true;
                    } else {
                        getClassPath().putResource(
                                ApkmArchiveLayout.entry(getName(), entryName), entryBytes);
                    }
                }
                input.closeEntry();
            }
        }
        if (!hasEntry) throw new IOException("Empty APKM file");
        if (apkCount == 0) throw new IOException("APKM contains no APK files");
        if (!hasBaseApk) throw new IOException("APKM does not contain base.apk");
    }

    private void addApk(String apkName, ClassPath apk) {
        String apkPrefix = ApkmArchiveLayout.entry(getName(), apkName);
        for (UnreadClassBytes classBytes : apk.getClasses()) {
            getClassPath().addClass(new UnreadClassBytes(
                    ApkmArchiveLayout.entry(apkPrefix, classBytes.getEntryName()),
                    classBytes.getBytes()));
        }
        for (UnreadDexBytes dexFile : apk.getDexFiles()) {
            int separator = dexFile.getEntryName().indexOf(ApkmArchiveLayout.ARCHIVE_SEPARATOR);
            String relativeName = separator < 0 ? dexFile.getEntryName()
                    : dexFile.getEntryName().substring(
                            separator + ApkmArchiveLayout.ARCHIVE_SEPARATOR.length());
            getClassPath().getDexFiles().add(new UnreadDexBytes(
                    ApkmArchiveLayout.entry(apkPrefix, relativeName), dexFile.getBytes()));
        }
        for (Map.Entry<String, byte[]> resource : apk.getResources().entrySet()) {
            getClassPath().putResource(
                    ApkmArchiveLayout.entry(apkPrefix, resource.getKey()), resource.getValue());
        }
        for (int index = 0; index < apk.getWarnings(); index++) {
            getClassPath().addWarning();
        }
    }

    private static String cleanEntryName(String name) {
        while (name.endsWith("/")) name = name.substring(0, name.length() - 1);
        return name;
    }
}
