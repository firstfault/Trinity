package me.f1nal.trinity.database.inputs.impl;

import me.f1nal.trinity.database.inputs.ProjectInputFileFactory;
import me.f1nal.trinity.execution.dex.DexTestFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectInputAPKMFileTest {
    @Test
    void parsesBaseAndSplitApksWithStableNestedNames() throws Exception {
        byte[] dex = DexTestFixture.create("sample/BundleClass", "bundle");
        byte[] baseApk = archive(Map.of(
                "classes.dex", dex,
                "AndroidManifest.xml", new byte[]{1, 2}));
        byte[] splitApk = archive(Map.of(
                "resources.arsc", new byte[]{3, 4}));
        byte[] apkm = archive(linkedEntries(
                "info.json", "{}".getBytes(),
                "base.apk", baseApk,
                "split_config.xhdpi.apk", splitApk));

        ProjectInputAPKMFile input = new ProjectInputAPKMFile(
                new File("fixture.apkm"), apkm);

        assertEquals(1, input.getClassPath().getDexFiles().size());
        assertEquals("fixture.apkm!/base.apk!/classes.dex",
                input.getClassPath().getDexFiles().get(0).getEntryName());
        assertArrayEquals(dex, input.getClassPath().getDexFiles().get(0).getBytes());
        assertTrue(input.getClassPath().getResources().containsKey(
                "fixture.apkm!/info.json"));
        assertTrue(input.getClassPath().getResources().containsKey(
                "fixture.apkm!/base.apk!/AndroidManifest.xml"));
        assertTrue(input.getClassPath().getResources().containsKey(
                "fixture.apkm!/split_config.xhdpi.apk!/resources.arsc"));
    }

    @Test
    void factorySelectsNativeApkmParser(@TempDir Path directory) throws Exception {
        byte[] base = archive(Map.of(
                "classes.dex", DexTestFixture.create("sample/FactoryClass", "factory")));
        Path bundle = directory.resolve("fixture.apkm");
        Files.write(bundle, archive(Map.of("base.apk", base)));

        assertInstanceOf(ProjectInputAPKMFile.class,
                new ProjectInputFileFactory().create(bundle.toFile()));
    }

    @Test
    void rejectsBundlesWithoutBaseApk() throws Exception {
        byte[] split = archive(Map.of("resources.arsc", new byte[]{1}));
        byte[] apkm = archive(Map.of("split_config.xhdpi.apk", split));

        assertThrows(IOException.class, () -> new ProjectInputAPKMFile(
                new File("missing-base.apkm"), apkm));
    }

    static byte[] archive(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream output = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue());
                output.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static Map<String, byte[]> linkedEntries(Object... values) {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            entries.put((String) values[index], (byte[]) values[index + 1]);
        }
        return entries;
    }
}
