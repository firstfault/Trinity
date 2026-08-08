package me.f1nal.trinity.execution.packages;

import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.database.compression.DatabaseCompressionTypeRaw;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PackageTest {
    @Test
    void keepsEntriesSortedAcrossBulkAdditions() {
        Package root = new Package(new Database(
                "test", new File("test.tdb"), new DatabaseCompressionTypeRaw()));
        root.add(new ResourceArchiveEntry("z.txt", new byte[0]));
        root.add(new ResourceArchiveEntry("b.txt", new byte[0]));

        assertEquals(List.of("b.txt", "z.txt"), names(root));

        root.add(new ResourceArchiveEntry("a.txt", new byte[0]));
        assertEquals(List.of("a.txt", "b.txt", "z.txt"), names(root));
    }

    private static List<String> names(Package pkg) {
        return pkg.getEntries().stream().map(ArchiveEntry::getDisplaySimpleName).toList();
    }
}
