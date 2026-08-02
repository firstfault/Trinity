package me.f1nal.trinity.execution.xref;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.database.DatabaseLoader;
import me.f1nal.trinity.database.object.AbstractDatabaseObject;
import me.f1nal.trinity.database.object.DatabaseXrefViewerSettings;
import me.f1nal.trinity.util.UnsafeUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XrefViewerSettingsTest {
    @Test
    void descriptorMetadataAndStackFrameAreDisabledByDefault() {
        XrefViewerSettings settings = new XrefViewerSettings();

        assertFalse(settings.isKindEnabled(XrefKind.DESCRIPTOR));
        assertFalse(settings.isKindEnabled(XrefKind.METADATA));
        assertFalse(settings.isKindEnabled(XrefKind.STACK_FRAME));
        assertTrue(settings.isKindEnabled(XrefKind.TYPE));
        assertTrue(settings.isKindEnabled(XrefKind.INVOKE));
    }

    @Test
    void settingsRoundTripThroughTheDatabase() throws Exception {
        XrefViewerSettings source = new XrefViewerSettings();
        source.setKindEnabled(XrefKind.DESCRIPTOR, true);
        source.setKindEnabled(XrefKind.METADATA, true);
        source.setKindEnabled(XrefKind.LITERAL, false);
        Database database = new Database("test", new File("test.tdb"), null);
        database.getObjects().add(source.createDatabaseObject());

        Database restored =
                DatabaseLoader.fromXML(DatabaseLoader.toXML(database));
        AbstractDatabaseObject object = restored.getObjects().iterator().next();
        XrefViewerSettings target = new XrefViewerSettings();
        Trinity trinity =
                (Trinity) UnsafeUtil.getUnsafe().allocateInstance(Trinity.class);
        setField(trinity, "xrefViewerSettings", target);

        assertTrue(object.load(trinity));
        assertTrue(target.isKindEnabled(XrefKind.DESCRIPTOR));
        assertTrue(target.isKindEnabled(XrefKind.METADATA));
        assertFalse(target.isKindEnabled(XrefKind.STACK_FRAME));
        assertFalse(target.isKindEnabled(XrefKind.LITERAL));
        assertTrue(target.isKindEnabled(XrefKind.TYPE));
    }

    @Test
    void legacySavedFiltersReceiveTheNewDisabledMetadataDefault() throws Exception {
        XrefViewerSettings source = new XrefViewerSettings();
        source.setKindEnabled(XrefKind.METADATA, true);
        DatabaseXrefViewerSettings legacy = source.createDatabaseObject();
        setField(legacy, "version", 0);
        XrefViewerSettings target = new XrefViewerSettings();
        Trinity trinity =
                (Trinity) UnsafeUtil.getUnsafe().allocateInstance(Trinity.class);
        setField(trinity, "xrefViewerSettings", target);

        assertTrue(legacy.load(trinity));
        assertFalse(target.isKindEnabled(XrefKind.METADATA));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
