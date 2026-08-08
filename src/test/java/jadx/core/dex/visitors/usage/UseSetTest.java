package jadx.core.dex.visitors.usage;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UseSetTest {
    @Test
    void retainsEveryConcurrentUsageForOneKey() {
        UseSet<String, Integer> usages = new UseSet<>();

        IntStream.range(0, 20_000).parallel().forEach(value -> usages.add("target", value));

        AtomicReference<Set<Integer>> values = new AtomicReference<>();
        usages.visit((key, uses) -> values.set(uses));
        assertEquals(20_000, values.get().size());
    }
}
