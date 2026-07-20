package me.f1nal.trinity.gui.navigation;

import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.database.DatabaseLoader;
import me.f1nal.trinity.database.object.DatabaseNavigationHistory;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationHistoryTest {
    @Test
    void backAndForwardMoveWithoutAddingEntries() {
        NavigationHistory history = new NavigationHistory();
        NavigationTarget first = target("sample/First");
        NavigationTarget second = target("sample/Second");
        history.record(first, NavigationAction.NAVIGATE);
        history.record(second, NavigationAction.FOLLOW_MEMBER);

        assertSame(first, history.back().orElseThrow().target());
        assertEquals(2, history.getEntries().size());
        assertSame(second, history.forward().orElseThrow().target());
        assertFalse(history.canGoForward());
    }

    @Test
    void newNavigationAfterBackDiscardsTheForwardBranch() {
        NavigationHistory history = new NavigationHistory();
        NavigationTarget first = target("sample/First");
        NavigationTarget discarded = target("sample/Discarded");
        NavigationTarget replacement = target("sample/Replacement");
        history.record(first, NavigationAction.NAVIGATE);
        history.record(discarded, NavigationAction.NAVIGATE);
        history.back();

        history.record(replacement, NavigationAction.FOLLOW_XREF);

        assertEquals(2, history.getEntries().size());
        assertSame(replacement, history.getCurrent().target());
        assertFalse(history.getEntries().stream().anyMatch(entry -> entry.target() == discarded));
    }

    @Test
    void selectingAnEntryMakesItTheBackForwardCursor() {
        NavigationHistory history = new NavigationHistory();
        NavigationTarget first = target("sample/First");
        NavigationTarget second = target("sample/Second");
        NavigationTarget third = target("sample/Third");
        history.record(first, NavigationAction.NAVIGATE);
        history.record(second, NavigationAction.NAVIGATE);
        history.record(third, NavigationAction.NAVIGATE);

        assertSame(first, history.select(0).orElseThrow().target());
        assertTrue(history.canGoForward());
        assertSame(second, history.forward().orElseThrow().target());
    }

    @Test
    void consecutiveNavigationsToTheSameTargetAreNotDuplicated() {
        NavigationHistory history = new NavigationHistory();
        NavigationTarget target = target("sample/Target");

        NavigationEntry first = history.record(target, NavigationAction.NAVIGATE);
        NavigationEntry duplicate = history.record(target, NavigationAction.FOLLOW_MEMBER);

        assertSame(first, duplicate);
        assertEquals(1, history.getEntries().size());
    }

    @Test
    void retainsOnlyTheNewestFiveHundredEntries() {
        NavigationHistory history = new NavigationHistory();
        for (int i = 0; i < NavigationHistory.MAX_ENTRIES + 25; i++) {
            history.record(target("sample/Target" + i), NavigationAction.NAVIGATE);
        }

        assertEquals(NavigationHistory.MAX_ENTRIES, history.getEntries().size());
        assertEquals("sample/Target25",
                history.getEntries().get(0).target().getClassTarget().getRealName());
        assertEquals(NavigationHistory.MAX_ENTRIES - 1, history.getCurrentIndex());
    }

    @Test
    void mutationsRequestPersistenceButSessionResetDoesNot() {
        AtomicInteger changes = new AtomicInteger();
        NavigationHistory history = new NavigationHistory(changes::incrementAndGet);
        history.record(target("sample/First"), NavigationAction.NAVIGATE);
        history.record(target("sample/Second"), NavigationAction.NAVIGATE);
        history.back();
        history.clear();

        assertEquals(4, changes.get());
        history.reset();
        assertEquals(4, changes.get());
    }

    @Test
    void databaseObjectRoundTripsThroughProjectXml() {
        NavigationHistory history = new NavigationHistory();
        history.record(target("sample/Persisted"), NavigationAction.FOLLOW_CONSTANT);
        Database database = new Database("test", new File("test.tdb"), null);
        database.getObjects().add(history.createDatabaseObject());

        Database restored = DatabaseLoader.fromXML(DatabaseLoader.toXML(database));

        assertTrue(restored.getObjects().stream()
                .anyMatch(DatabaseNavigationHistory.class::isInstance));
    }

    @Test
    void constantNavigationUsesItsOwnHistoryLabel() {
        assertEquals("constant", NavigationAction.FOLLOW_CONSTANT.getHistoryLabel());
    }

    @Test
    void constantNavigationRetainsItsDisplayText() {
        NavigationHistory history = new NavigationHistory();
        NavigationTarget target = target("sample/Constants");

        history.record(target, NavigationAction.FOLLOW_CONSTANT, "\"first\"");
        history.record(target, NavigationAction.FOLLOW_CONSTANT, "\"second\"");

        assertEquals(2, history.getEntries().size());
        assertEquals("\"first\"", history.getEntries().get(0).displayText());
        assertEquals("\"second\"", history.getEntries().get(1).displayText());
    }

    private static NavigationTarget target(String name) {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        node.name = name;
        node.superName = "java/lang/Object";
        ClassTarget target = new ClassTarget(name, 0);
        ClassInput input = new ClassInput(null, node, target);
        target.setInput(input);
        return NavigationTarget.capture(input, null);
    }
}
