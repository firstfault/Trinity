package me.f1nal.trinity.execution.dependency;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyManagerTest {
    @Test
    void usesClasspathOrderAndReindexesAfterRemoval() {
        DependencyArchive first = resolvedArchive("first.jar",
                classBytes("example/Shared", "first/Base"));
        DependencyArchive second = resolvedArchive("second.jar",
                classBytes("example/Shared", "second/Base"));
        DependencyManager manager = new DependencyManager();

        manager.addArchive(first);
        manager.addArchive(second);
        assertEquals("first/Base", manager.getClass("example/Shared").superName);

        assertTrue(manager.removeArchive(first));
        assertEquals("second/Base", manager.getClass("example/Shared").superName);

        manager.removeArchive(second);
        assertNull(manager.getClass("example/Shared"));
    }

    @Test
    void movingAnArchiveChangesFirstMatchResolution() {
        DependencyArchive first = resolvedArchive("first.jar",
                classBytes("example/Shared", "first/Base"));
        DependencyArchive second = resolvedArchive("second.jar",
                classBytes("example/Shared", "second/Base"));
        DependencyManager manager = new DependencyManager();
        manager.addArchive(first);
        manager.addArchive(second);

        assertTrue(manager.moveArchive(second, -1));
        assertEquals(java.util.List.of(second, first), manager.getArchives());
        assertEquals("second/Base", manager.getClass("example/Shared").superName);

        assertFalse(manager.moveArchive(second, -1));
        assertFalse(manager.moveArchive(first, 1));
        assertEquals(java.util.List.of(second, first), manager.getArchives());
    }

    private static DependencyArchive resolvedArchive(String name, byte[] bytes) {
        DependencyArchive archive = new DependencyArchive(UUID.randomUUID(), name,
                DependencyKind.ARCHIVE, name, "/tmp/" + name, null);
        archive.setResolved(Map.of("example/Shared", bytes), "/tmp/" + name);
        return archive;
    }

    private static byte[] classBytes(String name, String superName) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, superName, null);
        writer.visitEnd();
        return writer.toByteArray();
    }
}
