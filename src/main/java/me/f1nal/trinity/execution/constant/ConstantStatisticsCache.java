package me.f1nal.trinity.execution.constant;

import com.google.common.eventbus.Subscribe;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class ConstantStatisticsCache implements IEventListener {
    private final Execution execution;
    private volatile Map<ConstantKey, Integer> occurrences = Map.of();
    private volatile boolean dirty = true;

    public ConstantStatisticsCache(Execution execution) {
        this.execution = execution;
    }

    public int getOccurrences(Object value) {
        ConstantKey key = ConstantKey.of(value);
        if (key == null) {
            return 0;
        }
        if (dirty) {
            rebuild();
        }
        return occurrences.getOrDefault(key, 0);
    }

    private synchronized void rebuild() {
        if (!dirty) {
            return;
        }
        Map<ConstantKey, Integer> rebuilt = new HashMap<>();
        for (ClassInput classInput : new ArrayList<>(execution.getClassList())) {
            addClassOccurrences(rebuilt, classInput.getNode());
        }
        occurrences = Map.copyOf(rebuilt);
        dirty = false;
    }

    private static void addClassOccurrences(Map<ConstantKey, Integer> target, ClassNode node) {
        for (AsmConstantScanner.Occurrence occurrence : AsmConstantScanner.scan(node)) {
            ConstantKey key = ConstantKey.of(occurrence.value());
            if (key != null) target.merge(key, 1, Integer::sum);
        }
    }

    static int countOccurrences(ClassNode node, Object value) {
        ConstantKey key = ConstantKey.of(value);
        if (key == null) return 0;
        Map<ConstantKey, Integer> counted = new HashMap<>();
        addClassOccurrences(counted, node);
        return counted.getOrDefault(key, 0);
    }

    private void invalidate() {
        dirty = true;
    }

    @Subscribe
    public void onClassesLoaded(EventClassesLoaded event) {
        invalidate();
    }

    @Subscribe
    public void onClassModified(EventClassModified event) {
        invalidate();
    }

    @Subscribe
    public void onMemberModified(EventMemberModified event) {
        invalidate();
    }

    private enum ConstantKind {
        STRING,
        INTEGER,
        LONG,
        FLOAT,
        DOUBLE
    }

    private record ConstantKey(ConstantKind kind, Object value) {
        private static ConstantKey of(Object value) {
            if (value instanceof String string) {
                return new ConstantKey(ConstantKind.STRING, string);
            }
            if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
                return new ConstantKey(ConstantKind.INTEGER, ((Number) value).intValue());
            }
            if (value instanceof Character character) {
                return new ConstantKey(ConstantKind.INTEGER, (int) character);
            }
            if (value instanceof Long number) {
                return new ConstantKey(ConstantKind.LONG, number);
            }
            if (value instanceof Float number) {
                return new ConstantKey(ConstantKind.FLOAT, number);
            }
            if (value instanceof Double number) {
                return new ConstantKey(ConstantKind.DOUBLE, number);
            }
            return null;
        }
    }
}
