package me.f1nal.trinity.gui.windows.impl.assembler.inline;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InlineSuggestionMatcherTest {
    @Test
    void exactAndPrefixMatchesBeatContainsAndFuzzyMatches() {
        List<String> matches = InlineSuggestionMatcher.closest(List.of(
                "invokevirtual", "invokestatic", "virtual", "invokeinterface",
                "invokespecial", "invokedynamic"), "invokestatic", 5);

        assertEquals("invokestatic", matches.get(0));
    }

    @Test
    void limitsResultsToFiveStableCandidates() {
        List<String> matches = InlineSuggestionMatcher.closest(List.of(
                "aload", "aaload", "baload", "caload", "daload", "faload", "iaload"),
                "load", 5);

        assertEquals(5, matches.size());
        assertEquals(matches, InlineSuggestionMatcher.closest(List.of(
                "aload", "aaload", "baload", "caload", "daload", "faload", "iaload"),
                "load", 5));
    }

    @Test
    void blankQueriesDoNotOpenACompletionList() {
        assertEquals(List.of(), InlineSuggestionMatcher.closest(List.of("nop", "pop"), " ", 5));
    }

    @Test
    void rankedResultsRetainEveryCandidateForScrolling() {
        List<String> candidates = List.of("nop", "pop", "pop2", "dup", "dup2", "swap");
        List<String> ranked = InlineSuggestionMatcher.ranked(candidates, "p");

        assertEquals(candidates.size(), ranked.size());
        assertEquals("pop", ranked.get(0));
    }

    @Test
    void rankedResultsExcludeNonMatches() {
        List<String> ranked = InlineSuggestionMatcher.ranked(
                List.of("invokevirtual", "invokestatic", "getfield", "putfield"), "invoke");

        assertEquals(List.of("invokestatic", "invokevirtual"), ranked);
    }
}
