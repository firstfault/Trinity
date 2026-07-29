package me.f1nal.trinity.database.datapool;

import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.execution.dependency.DependencyArchive;
import me.f1nal.trinity.execution.dependency.DependencyArchiveReader;
import me.f1nal.trinity.execution.dependency.DependencyKind;
import me.f1nal.trinity.execution.loading.tasks.ClassInputReaderLoadTask;
import me.f1nal.trinity.execution.loading.tasks.RuntimeDependencyLoadTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataPoolDependencyArchiveTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void dependencyArchivesRoundTripThroughTheDatabaseDataPool() throws Exception {
        UUID id = UUID.randomUUID();
        DependencyArchive original = new DependencyArchive(id, "dependency.jar",
                DependencyKind.ARCHIVE, "libs/dependency.jar",
                "/opt/project/libs/dependency.jar", null);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            DataPool.writeDependencyArchives(output, List.of(original));
        }

        List<DependencyArchive> restored;
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = DataPool.readDependencyArchives(input);
        }

        assertEquals(1, restored.size());
        assertEquals(id, restored.get(0).getId());
        assertEquals("dependency.jar", restored.get(0).getName());
        assertEquals(DependencyKind.ARCHIVE, restored.get(0).getKind());
        assertEquals("libs/dependency.jar", restored.get(0).getRelativePath());
        assertEquals("/opt/project/libs/dependency.jar", restored.get(0).getAbsolutePath());
        assertEquals(0, restored.get(0).getClassCount());
    }

    @Test
    void versionTwoDatabasesAreMigratedWithJavaBase() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeShort(2);
            output.writeInt(0);
        }
        Database database = new Database("legacy", new File("legacy.tdb"), null);
        database.loadTasks = new ArrayList<>();

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            new DataPool().deserialize(database, input);
        }

        assertEquals(2, database.loadTasks.size());
        assertInstanceOf(RuntimeDependencyLoadTask.class, database.loadTasks.get(0));
        assertInstanceOf(ClassInputReaderLoadTask.class, database.loadTasks.get(1));
    }

    @Test
    void embeddedVersionThreeDependenciesMigrateToReferences() throws Exception {
        UUID runtimeId = UUID.randomUUID();
        UUID archiveId = UUID.randomUUID();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(2);
            writeEmbeddedDependency(output, runtimeId, "java.base");
            writeEmbeddedDependency(output, archiveId, "library.jar");
        }

        List<DependencyArchive> migrated;
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            migrated = DataPool.readEmbeddedDependencyArchives(input);
        }

        assertEquals(DependencyKind.RUNTIME_MODULE, migrated.get(0).getKind());
        assertEquals("java.base", migrated.get(0).getRuntimeModule());
        assertEquals(DependencyKind.ARCHIVE, migrated.get(1).getKind());
        assertEquals("library.jar", migrated.get(1).getRelativePath());
        assertEquals(0, migrated.get(1).getClassCount());
    }

    @Test
    void resolvedClassBytesAreNotWrittenToTheDatabase() throws Exception {
        Path jar = temporaryDirectory.resolve("large-dependency.jar");
        try (OutputStream stream = Files.newOutputStream(jar);
             ZipOutputStream zip = new ZipOutputStream(stream)) {
            zip.putNextEntry(new ZipEntry("dep/Large.class"));
            zip.write(largeClassBytes());
            zip.closeEntry();
        }
        DependencyArchive archive = DependencyArchiveReader.read(
                jar.toFile(), temporaryDirectory.resolve("project.tdb").toFile());
        assertTrue(archive.getLoadedSize() > 40_000);

        ByteArrayOutputStream persisted = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(persisted)) {
            DataPool.writeDependencyArchives(output, List.of(archive));
        }

        assertTrue(persisted.size() < 1_024,
                "Dependency persistence should contain locator metadata, not loaded class bytes");
    }

    private static byte[] largeClassBytes() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "dep/Large", null, "java/lang/Object", null);
        writer.visitSource("Large.java", "x".repeat(50_000));
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void writeEmbeddedDependency(DataOutputStream output, UUID id, String name)
            throws Exception {
        output.writeLong(id.getMostSignificantBits());
        output.writeLong(id.getLeastSignificantBits());
        output.writeUTF(name);
        output.writeInt(1);
        output.writeUTF("dep/Type");
        output.writeInt(4);
        output.write(new byte[]{1, 2, 3, 4});
    }
}
