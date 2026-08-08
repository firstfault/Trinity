package me.f1nal.trinity.database.inputs;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApkmArchiveLayoutTest {
    @Test
    void reconstructsApkmAndNestedApkArchives() throws Exception {
        Map<String, byte[]> encoded = new LinkedHashMap<>();
        encoded.put("fixture.apkm!/info.json", "{}".getBytes());
        encoded.put("fixture.apkm!/base.apk!/classes.dex", new byte[]{1, 2, 3});
        encoded.put("fixture.apkm!/base.apk!/AndroidManifest.xml", new byte[]{4});
        encoded.put("fixture.apkm!/split_config.xhdpi.apk!/resources.arsc", new byte[]{5});

        Map<String, byte[]> output = ApkmArchiveLayout.materialize(encoded);
        Map<String, byte[]> base = unzip(output.get("base.apk"));
        Map<String, byte[]> split = unzip(output.get("split_config.xhdpi.apk"));

        assertEquals(3, output.size());
        assertArrayEquals("{}".getBytes(), output.get("info.json"));
        assertArrayEquals(new byte[]{1, 2, 3}, base.get("classes.dex"));
        assertArrayEquals(new byte[]{4}, base.get("AndroidManifest.xml"));
        assertArrayEquals(new byte[]{5}, split.get("resources.arsc"));
    }

    @Test
    void nestsBundlesWhenCombinedWithOtherProjectInputs() throws Exception {
        Map<String, byte[]> encoded = new LinkedHashMap<>();
        encoded.put("fixture.apkm!/base.apk!/classes.dex", new byte[]{1});
        encoded.put("extra.txt", new byte[]{2});

        Map<String, byte[]> output = ApkmArchiveLayout.materialize(encoded);
        Map<String, byte[]> bundle = unzip(output.get("fixture.apkm"));

        assertTrue(output.containsKey("extra.txt"));
        assertArrayEquals(new byte[]{1}, unzip(bundle.get("base.apk")).get("classes.dex"));
    }

    @Test
    void rejectsFileAndNestedArchiveCollisions() {
        Map<String, byte[]> encoded = new LinkedHashMap<>();
        encoded.put("fixture.apkm!/base.apk", new byte[]{1});
        encoded.put("fixture.apkm!/base.apk!/classes.dex", new byte[]{2});

        assertThrows(IllegalArgumentException.class,
                () -> ApkmArchiveLayout.materialize(encoded));
    }

    private static Map<String, byte[]> unzip(byte[] bytes) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                input.transferTo(output);
                entries.put(entry.getName(), output.toByteArray());
                input.closeEntry();
            }
        }
        return entries;
    }
}
