package me.f1nal.trinity.gui.components.filter;

import me.f1nal.trinity.gui.components.SearchBar;
import me.f1nal.trinity.util.SearchTermMatchable;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchBarFilterTest {
    @Test
    void caseSensitivityChangesTheFilterPredicate() {
        SearchBar searchBar = new SearchBar();
        SearchBarFilter<TestMatchable> filter = new SearchBarFilter<>(searchBar, true);
        TestMatchable value = new TestMatchable("ExampleMethod");
        searchBar.getSearchText().set("example");

        searchBar.setCaseSensitive(false);
        assertTrue(filter.filter().test(value));

        searchBar.setCaseSensitive(true);
        assertFalse(filter.filter().test(value));

        searchBar.getSearchText().set("Example");
        assertTrue(filter.filter().test(value));
    }

    private record TestMatchable(String value) implements SearchTermMatchable {
        @Override
        public boolean matches(String searchTerm) {
            return this.value.contains(searchTerm);
        }

        @Override
        public boolean matchesIgnoreCase(String searchTerm) {
            return this.value.toLowerCase(Locale.ROOT).contains(searchTerm.toLowerCase(Locale.ROOT));
        }
    }
}
