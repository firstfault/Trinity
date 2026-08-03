package me.f1nal.trinity.execution.loading.tasks;

import me.f1nal.trinity.execution.ClassTarget;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassInputReaderLoadTaskTest {
    @Test
    void detectsExistingNameWithoutPartiallyReservingRejectedContainer() {
        Set<String> reserved = new HashSet<>(Set.of("existing/Type"));

        String duplicate = ClassInputReaderLoadTask.reserveClassNames(reserved,
                List.of(target("new/First"), target("existing/Type"), target("new/Second")));

        assertEquals("existing/Type", duplicate);
        assertEquals(Set.of("existing/Type"), reserved);
    }

    @Test
    void detectsDuplicateWithinOneContainerWithoutReservingIt() {
        Set<String> reserved = new HashSet<>();

        String duplicate = ClassInputReaderLoadTask.reserveClassNames(reserved,
                List.of(target("same/Type"), target("same/Type")));

        assertEquals("same/Type", duplicate);
        assertEquals(Set.of(), reserved);
    }

    private static ClassTarget target(String name) {
        return new ClassTarget(name, 0);
    }
}
