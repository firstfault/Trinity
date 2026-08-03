package me.f1nal.trinity.database;

import me.f1nal.trinity.database.object.DatabaseDecompiler;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseDecompilerFlagsTest {
    @Test
    void enumPresentationFlagsAreBackwardCompatibleAndSurviveSerialization() {
        Database legacyDatabase = new Database("legacy", new File("legacy.tdb"), null);
        legacyDatabase.getObjects().add(new DatabaseDecompiler("example/Legacy"));
        String legacyXml = DatabaseLoader.toXML(legacyDatabase)
                .replaceFirst("<flags>[^<]*</flags>", "");
        DatabaseDecompiler legacy = assertInstanceOf(DatabaseDecompiler.class,
                DatabaseLoader.fromXML(legacyXml).getObjects().iterator().next());
        assertFalse(legacy.hasEnumPresentation());

        DatabaseDecompiler enumView = new DatabaseDecompiler(
                "example/EnumView", DatabaseDecompiler.createFlags(false));
        assertTrue(enumView.hasEnumPresentation());
        assertFalse(enumView.isEnumAsClass());

        Database database = new Database("test", new File("test.tdb"), null);
        database.getObjects().add(new DatabaseDecompiler(
                "example/ClassView", DatabaseDecompiler.createFlags(true)));

        Database restored = DatabaseLoader.fromXML(DatabaseLoader.toXML(database));
        DatabaseDecompiler classView = assertInstanceOf(
                DatabaseDecompiler.class, restored.getObjects().iterator().next());
        assertTrue(classView.hasEnumPresentation());
        assertTrue(classView.isEnumAsClass());
    }
}
