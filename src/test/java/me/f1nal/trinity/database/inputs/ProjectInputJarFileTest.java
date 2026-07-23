package me.f1nal.trinity.database.inputs;

import me.f1nal.trinity.database.inputs.impl.ProjectInputJARFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectInputJarFileTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void preservesMultiReleaseClassesAsOpaqueResources() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(bytes)) {
            jar.putNextEntry(new ZipEntry("example/Base.class"));
            jar.write(new byte[]{1, 2, 3});
            jar.closeEntry();
            jar.putNextEntry(new ZipEntry("META-INF/versions/17/example/Base.class"));
            jar.write(new byte[]{4, 5, 6});
            jar.closeEntry();
        }
        byte[] jarBytes = bytes.toByteArray();

        File source = temporaryDirectory.resolve("input.jar").toFile();
        Files.write(source.toPath(), jarBytes);
        ProjectInputJARFile input = new ProjectInputJARFile(source, jarBytes);

        assertEquals(1, input.getClassPath().getClasses().size());
        assertTrue(input.getClassPath().getResources()
                .containsKey("META-INF/versions/17/example/Base.class"));
    }

    @Test
    void retainsArchiveAndCentralDirectoryMetadata() throws Exception {
        byte[] payload = {10, 20, 30};
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(bytes)) {
            jar.setComment("archive comment");

            ZipEntry directory = new ZipEntry("data/");
            directory.setComment("directory comment");
            jar.putNextEntry(directory);
            jar.closeEntry();

            CRC32 crc = new CRC32();
            crc.update(payload);
            ZipEntry resource = new ZipEntry("data/value.bin");
            resource.setMethod(ZipEntry.STORED);
            resource.setSize(payload.length);
            resource.setCompressedSize(payload.length);
            resource.setCrc(crc.getValue());
            resource.setLastModifiedTime(FileTime.fromMillis(1_700_000_000_000L));
            resource.setComment("entry comment");
            resource.setExtra(new byte[]{(byte) 0xCA, (byte) 0xFE, 0, 0});
            jar.putNextEntry(resource);
            jar.write(payload);
            jar.closeEntry();
        }
        byte[] jarBytes = bytes.toByteArray();

        File source = temporaryDirectory.resolve("metadata.jar").toFile();
        Files.write(source.toPath(), jarBytes);
        ProjectInputJARFile input = new ProjectInputJARFile(source, jarBytes);

        assertEquals("archive comment", input.getClassPath().getArchiveComment());
        assertEquals(1, input.getClassPath().getDirectories().size());
        assertEquals("directory comment", input.getClassPath().getDirectories().get(0).getMetadata().getComment());
        var metadata = input.getClassPath().getResourceMetadata("data/value.bin");
        assertEquals(ZipEntry.STORED, metadata.getMethod());
        assertEquals("entry comment", metadata.getComment());
        assertEquals(1, metadata.getOrder());
        assertEquals(1_700_000_000_000L, metadata.getModifiedTime());
    }
}
