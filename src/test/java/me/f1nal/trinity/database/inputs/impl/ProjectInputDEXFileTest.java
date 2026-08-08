package me.f1nal.trinity.database.inputs.impl;

import me.f1nal.trinity.database.inputs.ProjectInputFileFactory;
import me.f1nal.trinity.execution.dex.DexTestFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectInputDEXFileTest {
    @Test
    void acceptsStandaloneDexInput() throws Exception {
        byte[] dex = DexTestFixture.create();

        ProjectInputDEXFile input = new ProjectInputDEXFile(new File("classes.dex"), dex);

        assertEquals(1, input.getClassPath().getDexFiles().size());
        assertArrayEquals(dex, input.getClassPath().getDexFiles().get(0).getBytes());
        assertTrue(input.getClassPath().getClasses().isEmpty());
    }

    @Test
    void factoryDetectsDexMagic(@TempDir Path directory) throws Exception {
        Path dexPath = directory.resolve("fixture.dex");
        Files.write(dexPath, DexTestFixture.create());

        assertInstanceOf(ProjectInputDEXFile.class,
                new ProjectInputFileFactory().create(dexPath.toFile()));
    }

    @Test
    void separatesMultidexPayloadsFromApkResources() throws Exception {
        byte[] primary = DexTestFixture.create();
        byte[] secondary = DexTestFixture.create("sample/DexSecondary", "secondary");
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("classes.dex", primary);
        entries.put("classes2.dex", secondary);
        entries.put("AndroidManifest.xml", new byte[]{1, 2, 3});

        ProjectInputJARFile input = new ProjectInputJARFile(
                new File("sample.apk"), archive(entries));

        assertEquals(2, input.getClassPath().getDexFiles().size());
        assertEquals("sample.apk!/classes.dex",
                input.getClassPath().getDexFiles().get(0).getEntryName());
        assertFalse(input.getClassPath().getResources().containsKey("classes.dex"));
        assertArrayEquals(new byte[]{1, 2, 3},
                input.getClassPath().getResources().get("AndroidManifest.xml"));
    }

    private static byte[] archive(Map<String, byte[]> entries) throws Exception {
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
}
