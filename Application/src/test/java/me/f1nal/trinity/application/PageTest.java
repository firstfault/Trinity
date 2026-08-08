package me.f1nal.trinity.application;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageTest {
    @Test
    void slicesResultsWithStableContinuationOffset() {
        Page<Integer> first = Page.slice(List.of(1, 2, 3, 4, 5), 1, 2);
        assertEquals(List.of(2, 3), first.items());
        assertEquals(3, first.nextOffset());
        assertEquals(5, first.total());

        Page<Integer> last = Page.slice(List.of(1, 2, 3), 2, 10);
        assertEquals(List.of(3), last.items());
        assertNull(last.nextOffset());
    }

    @Test
    void ownsAnImmutableResultSnapshot() {
        ArrayList<Integer> mutable = new ArrayList<>(List.of(1, 2));
        Page<Integer> page = new Page<>(mutable, 0, 2, 2, null);
        mutable.add(3);
        assertEquals(List.of(1, 2), page.items());
        assertThrows(UnsupportedOperationException.class, () -> page.items().add(4));
    }

    @Test
    void rejectsInvalidBounds() {
        assertThrows(IllegalArgumentException.class,
                () -> new Page<>(List.of(), -1, 1, 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Page<>(List.of(), 0, 0, 0, null));
    }
}
