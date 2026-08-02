package me.f1nal.trinity.gui.components.filter.kind;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KindFilterTest {
    @Test
    void appliesConfiguredInitialStateToOrderedAndDiscoveredKinds() {
        KindFilter<TestItem> filter =
                new KindFilter<>(new IKindType[]{TestKind.VISIBLE},
                        kind -> kind != TestKind.HIDDEN);
        TestItem visible = new TestItem(TestKind.VISIBLE);
        TestItem hidden = new TestItem(TestKind.HIDDEN);

        filter.initialize(List.of(visible, hidden));

        assertTrue(filter.isEnabled(TestKind.VISIBLE));
        assertFalse(filter.isEnabled(TestKind.HIDDEN));
        assertTrue(filter.filter().test(visible));
        assertFalse(filter.filter().test(hidden));
    }

    @Test
    void tracksTypeNamesFromTheCurrentUpstreamResults() {
        KindFilter<TestItem> filter = new KindFilter<>(TestKind.values());
        TestItem first = new TestItem(TestKind.VISIBLE, "First");
        TestItem second = new TestItem(TestKind.VISIBLE, "Second");
        filter.initialize(List.of(first, second));

        filter.update(List.of(second));

        assertFalse(filter.getPresentTypeNames(TestKind.VISIBLE).contains("First"));
        assertTrue(filter.getPresentTypeNames(TestKind.VISIBLE).contains("Second"));
    }

    private record TestItem(TestKind kind, String typeName) implements IKind, IKindTypeName {
        private TestItem(TestKind kind) {
            this(kind, kind.name());
        }

        @Override
        public IKindType getKind() {
            return kind;
        }

        @Override
        public String getKindTypeName() {
            return typeName;
        }
    }

    private enum TestKind implements IKindType {
        VISIBLE,
        HIDDEN;

        @Override
        public int getColor() {
            return 0;
        }

        @Override
        public String getName() {
            return name();
        }
    }
}
