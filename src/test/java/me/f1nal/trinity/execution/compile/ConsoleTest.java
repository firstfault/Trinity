package me.f1nal.trinity.execution.compile;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsoleTest {
    @Test
    void expandableWarningsRetainEveryDetailAndCanBeUpdated() {
        Console console = new Console();
        List<String> dependencies = IntStream.range(0, 25)
                .mapToObj(index -> "missing/Dependency" + index)
                .toList();

        Console.ExpandableLog log = console.warnExpandable(
                "Unresolved Dependencies ({})", dependencies, String.valueOf(dependencies.size()));

        assertEquals("Unresolved Dependencies (25)", log.getSummary());
        assertEquals(dependencies, log.getDetails());

        List<String> updated = IntStream.range(0, 30)
                .mapToObj(index -> "missing/Dependency" + index)
                .toList();
        log.update("Unresolved Dependencies ({})", updated, String.valueOf(updated.size()));

        assertEquals("Unresolved Dependencies (30)", log.getSummary());
        assertEquals(updated, log.getDetails());
        assertEquals("Unresolved Dependencies (30)" + System.lineSeparator()
                        + updated.stream()
                        .map(dependency -> "  " + dependency)
                        .collect(java.util.stream.Collectors.joining(System.lineSeparator())),
                console.getPlainText());
    }
}
