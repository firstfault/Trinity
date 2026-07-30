package me.f1nal.trinity.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseLoaderAtomicSaveTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsACompleteDatabaseWithoutLeavingTemporaryFiles() throws Exception {
        Path database = temporaryDirectory.resolve("nested/project.tdb");
        byte[] contents = new byte[]{1, 2, 3, 4};

        DatabaseLoader.writeDatabaseAtomically(database.toFile(), contents);

        assertArrayEquals(contents, Files.readAllBytes(database));
        assertFalse(Files.exists(database.resolveSibling("project.tdb.bak")));
        assertNoTemporaryFiles(database.getParent());
    }

    @Test
    void atomicallyRotatesThePreviousDatabaseIntoABackup() throws Exception {
        Path database = temporaryDirectory.resolve("project.tdb");
        Path backup = temporaryDirectory.resolve("project.tdb.bak");
        byte[] original = new byte[]{1, 2, 3};
        byte[] replacement = new byte[]{4, 5, 6};
        byte[] secondReplacement = new byte[]{7, 8, 9};
        Files.write(database, original);

        DatabaseLoader.writeDatabaseAtomically(database.toFile(), replacement);

        assertArrayEquals(replacement, Files.readAllBytes(database));
        assertArrayEquals(original, Files.readAllBytes(backup));

        DatabaseLoader.writeDatabaseAtomically(database.toFile(), secondReplacement);

        assertArrayEquals(secondReplacement, Files.readAllBytes(database));
        assertArrayEquals(replacement, Files.readAllBytes(backup));
        assertNoTemporaryFiles(temporaryDirectory);
    }

    @Test
    void failedReplacementCleansUpWithoutRemovingTheExistingDestination() throws Exception {
        Path databaseDirectory = temporaryDirectory.resolve("project.tdb");
        Files.createDirectory(databaseDirectory);
        Path marker = databaseDirectory.resolve("keep.txt");
        Files.writeString(marker, "existing data");

        assertThrows(IOException.class, () ->
                DatabaseLoader.writeDatabaseAtomically(
                        databaseDirectory.toFile(), new byte[]{9, 9, 9}));

        assertTrue(Files.isDirectory(databaseDirectory));
        assertTrue(Files.exists(marker));
        assertNoTemporaryFiles(temporaryDirectory);
    }

    private static void assertNoTemporaryFiles(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            List<String> names = files.map(path -> path.getFileName().toString()).toList();
            assertFalse(names.stream().anyMatch(name ->
                    name.endsWith(".trinity.tmp")
                            || name.contains(".backup.") && name.endsWith(".tmp")));
        }
    }
}
