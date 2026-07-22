package me.f1nal.trinity.gui.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalSearchMatcherTest {
    @Test
    void ranksExactAndPrefixMatchesAheadOfFuzzyMatches() {
        int exact = match("constant search", "Constant Search").score();
        int prefix = match("constant", "Constant Search").score();
        int fuzzy = match("cnss", "Constant Search").score();

        assertTrue(exact > prefix);
        assertTrue(prefix > fuzzy);
    }

    @Test
    void recognizesPathSegmentsAndPreservesPrimaryHighlightPositions() {
        GlobalSearchMatcher.Match match = GlobalSearchMatcher.match(
                "Search", "SearchService", List.of("me/example/SearchService"))
                .orElseThrow();

        assertEquals(List.of(0, 1, 2, 3, 4, 5), match.matchedCharacters());
    }

    @Test
    void aliasesCanMatchWithoutHighlightingUnrelatedTitleCharacters() {
        GlobalSearchMatcher.Match match = GlobalSearchMatcher.match(
                "constant viewer", "Constant Search", List.of("constant viewer", "literal search"))
                .orElseThrow();

        assertTrue(match.matchedCharacters().isEmpty());
    }

    @Test
    void matchingIsCaseInsensitive() {
        assertTrue(GlobalSearchMatcher.match("GLOBAL rename", "Global Rename", List.of()).isPresent());
    }

    private static GlobalSearchMatcher.Match match(String query, String title) {
        return GlobalSearchMatcher.match(query, title, List.of()).orElseThrow();
    }
}
