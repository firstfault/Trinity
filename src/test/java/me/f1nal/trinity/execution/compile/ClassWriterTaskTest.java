package me.f1nal.trinity.execution.compile;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.packages.ArchiveDirectoryEntry;
import me.f1nal.trinity.execution.packages.ProjectContainer;
import me.f1nal.trinity.execution.packages.ProjectContainerKind;
import me.f1nal.trinity.execution.packages.ResourceArchiveEntry;
import me.f1nal.trinity.execution.packages.ZipEntryMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassWriterTaskTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void recognizesOnlyJarSignatureFiles() {
        assertTrue(ClassWriterTask.isSignatureEntry("META-INF/APP.SF"));
        assertTrue(ClassWriterTask.isSignatureEntry("meta-inf/app.rsa"));
        assertTrue(ClassWriterTask.isSignatureEntry("META-INF/APP.DSA"));
        assertTrue(ClassWriterTask.isSignatureEntry("META-INF/APP.EC"));
        assertFalse(ClassWriterTask.isSignatureEntry("META-INF/MANIFEST.MF"));
        assertFalse(ClassWriterTask.isSignatureEntry("assets/example.sf"));
    }

    @Test
    void exportsCleanClassBytesAndZipMetadataWithoutRebuilding() throws Exception {
        byte[] classBytes = createClassBytes();
        ZipEntryMetadata classMetadata = new ZipEntryMetadata(0, ZipEntry.STORED,
                1_700_000_000_000L, ZipEntryMetadata.MISSING_TIME, ZipEntryMetadata.MISSING_TIME,
                "class comment", new byte[]{(byte) 0xCA, (byte) 0xFE, 0, 0}, -1L, -1L);
        ClassNode node = new ClassNode();
        new ClassReader(classBytes).accept(node, 0);
        ClassTarget target = new ClassTarget(node.name, classBytes.length, classMetadata);
        ClassInput input = new ClassInput(null, node, target, classBytes, "unusual/Original.class", false);
        target.setInput(input);

        ProjectContainer container = new ProjectContainer(UUID.randomUUID(), "input.jar",
                ProjectContainerKind.JAR, null);
        container.setArchiveComment("archive comment");
        container.register(target);
        ZipEntryMetadata resourceMetadata = ZipEntryMetadata.createDefault();
        resourceMetadata.setOrder(1);
        resourceMetadata.setComment("resource comment");
        container.register(new ResourceArchiveEntry("data.txt", new byte[]{4, 5, 6}, resourceMetadata));
        ZipEntryMetadata directoryMetadata = ZipEntryMetadata.createDefault();
        directoryMetadata.setOrder(2);
        container.addDirectory(new ArchiveDirectoryEntry("empty/", directoryMetadata));

        Path output = temporaryDirectory.resolve("output.jar");
        new ClassWriterTask(container, null, new Console(), output.toFile(), false).buildJar(progress -> { });

        try (ZipFile zip = new ZipFile(output.toFile())) {
            assertEquals("archive comment", zip.getComment());
            List<String> names = new ArrayList<>();
            Enumeration<? extends ZipEntry> enumeration = zip.entries();
            while (enumeration.hasMoreElements()) names.add(enumeration.nextElement().getName());
            assertEquals(List.of("unusual/Original.class", "data.txt", "empty/"), names);
            ZipEntry classEntry = zip.getEntry("unusual/Original.class");
            assertEquals(ZipEntry.STORED, classEntry.getMethod());
            assertEquals("class comment", classEntry.getComment());
            assertArrayEquals(classBytes, zip.getInputStream(classEntry).readAllBytes());
            assertTrue(zip.getEntry("empty/").isDirectory());
        }
        assertFalse(input.isRebuildRequired());
    }

    private static byte[] createClassBytes() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "example/Test", null, "java/lang/Object", null);
        writer.visitEnd();
        return writer.toByteArray();
    }
}
