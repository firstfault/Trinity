package me.f1nal.trinity.gui.navigation;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

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
