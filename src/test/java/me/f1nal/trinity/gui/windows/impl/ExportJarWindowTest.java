package me.f1nal.trinity.gui.windows.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportJarWindowTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void rejectsBlankNonJarAndDirectoryDestinations() throws Exception {
        assertFalse(ExportJarWindow.validateOutputPath(" ").valid());
        assertFalse(ExportJarWindow.validateOutputPath(
                temporaryDirectory.resolve("output.zip").toString()).valid());
        assertFalse(ExportJarWindow.validateOutputPath(
                temporaryDirectory.toString()).valid());

        Path fileParent = temporaryDirectory.resolve("not-a-directory");
        Files.writeString(fileParent, "content");
        assertFalse(ExportJarWindow.validateOutputPath(
                fileParent.resolve("output.jar").toString()).valid());
    }

    @Test
    void acceptsNewJarInDirectoriesThatWillBeCreated() {
        ExportJarWindow.OutputValidation validation = ExportJarWindow.validateOutputPath(
                temporaryDirectory.resolve("new/nested/output.JAR").toString());

        assertTrue(validation.valid());
        assertFalse(validation.overwriteRequired());
    }

    @Test
    void requiresConfirmationForAnExistingJar() throws Exception {
        Path output = temporaryDirectory.resolve("existing.jar");
        Files.write(output, new byte[]{1, 2, 3});

        ExportJarWindow.OutputValidation validation =
                ExportJarWindow.validateOutputPath(output.toString());

        assertTrue(validation.valid());
        assertTrue(validation.overwriteRequired());
    }
}
