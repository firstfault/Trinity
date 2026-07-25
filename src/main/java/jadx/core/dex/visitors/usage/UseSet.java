package jadx.core.dex.visitors.usage;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Trinity patch for JADX 1.5.6: make usage collection safe for parallel class scans.
 * Remove when the equivalent change is available upstream.
 */
public class UseSet<K, V> {
    private final Map<K, Set<V>> useMap = new ConcurrentHashMap<>();

    public void add(K obj, V use) {
        if (obj == use) {
            // self excluded
            return;
        }
        Set<V> set = useMap.computeIfAbsent(obj, key -> ConcurrentHashMap.newKeySet());
        set.add(use);
    }

    public Set<V> get(K obj) {
        return useMap.get(obj);
    }

    public Set<V> getOrDefault(K obj, Set<V> defaultValue) {
        return useMap.getOrDefault(obj, defaultValue);
    }

    public void visit(BiConsumer<K, Set<V>> consumer) {
        for (Map.Entry<K, Set<V>> entry : useMap.entrySet()) {
            consumer.accept(entry.getKey(), entry.getValue());
        }
    }

    public void parallelVisit(BiConsumer<K, Set<V>> consumer) {
        useMap.entrySet().parallelStream()
                .forEach(entry -> consumer.accept(entry.getKey(), entry.getValue()));
    }
}
