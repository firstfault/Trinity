package me.f1nal.trinity.execution.dependency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyArchiveReaderTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void readsClassesFromAnArchiveByTheirBytecodeNames() throws Exception {
        Path archivePath = temporaryDirectory.resolve("library.jar");
        try (OutputStream stream = Files.newOutputStream(archivePath);
             ZipOutputStream zip = new ZipOutputStream(stream)) {
            zip.putNextEntry(new ZipEntry("unexpected/location.class"));
            zip.write(classBytes("dependency/Library", "java/lang/Object"));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("resource.txt"));
            zip.write(new byte[]{1, 2, 3});
            zip.closeEntry();
        }

        DependencyArchive archive = DependencyArchiveReader.read(
                archivePath.toFile(), temporaryDirectory.resolve("project.tdb").toFile());

        assertEquals("library.jar", archive.getName());
        assertEquals(DependencyKind.ARCHIVE, archive.getKind());
        assertEquals("library.jar", archive.getRelativePath());
        assertEquals(archivePath.toAbsolutePath().normalize().toString(), archive.getAbsolutePath());
        assertEquals(1, archive.getClassCount());
        assertTrue(archive.getClasses().containsKey("dependency/Library"));
    }

    @Test
    void unresolvedReferencesRemainAvailableForRebinding() {
        DependencyArchive archive = new DependencyArchive(UUID.randomUUID(), "missing.jar",
                DependencyKind.ARCHIVE, "lib/missing.jar", "/missing/fallback.jar", null);

        DependencyArchiveReader.resolve(
                archive, temporaryDirectory.resolve("project.tdb").toFile());

        assertFalse(archive.isResolved());
        assertEquals(0, archive.getClassCount());
        assertTrue(archive.getResolutionError().contains("Archive not found"));
    }

    private static byte[] classBytes(String name, String superName) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, superName, null);
        writer.visitEnd();
        return writer.toByteArray();
    }
}
