package me.f1nal.trinity.gui.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Deterministic ranking shared by action and project-file search. */
public final class GlobalSearchMatcher {
    private GlobalSearchMatcher() {
    }

    public static Optional<Match> match(String query, String primary, List<String> additionalText) {
        String needle = normalize(query);
        List<String> normalizedAdditional = additionalText.stream().map(GlobalSearchMatcher::normalize).toList();
        return matchNormalized(needle, normalize(primary), normalizedAdditional);
    }

    static Optional<Match> matchNormalized(String needle, String primary,
                                           List<String> additionalText) {
        if (needle.isEmpty()) return Optional.of(new Match(0, List.of()));

        ScoredText primaryScore = score(needle, primary);
        int bestScore = primaryScore == null ? Integer.MIN_VALUE : primaryScore.score();
        List<Integer> matchedCharacters = primaryScore == null ? List.of() : primaryScore.characters();

        for (int i = 0; i < additionalText.size(); i++) {
            ScoredText candidate = score(needle, additionalText.get(i));
            if (candidate == null) continue;
            int adjusted = candidate.score() - 350 - i * 12;
            if (adjusted > bestScore) {
                bestScore = adjusted;
                matchedCharacters = List.of();
            }
        }
        return bestScore == Integer.MIN_VALUE
                ? Optional.empty() : Optional.of(new Match(bestScore, matchedCharacters));
    }

    private static ScoredText score(String needle, String haystack) {
        if (haystack.isEmpty()) return null;
        if (haystack.equals(needle)) {
            return new ScoredText(10_000, range(0, needle.length()));
        }
        if (haystack.startsWith(needle)) {
            return new ScoredText(9_200 - Math.min(300, haystack.length() - needle.length()),
                    range(0, needle.length()));
        }

        int wordStart = findWordPrefix(haystack, needle);
        if (wordStart >= 0) {
            return new ScoredText(8_500 - Math.min(500, wordStart),
                    range(wordStart, wordStart + needle.length()));
        }

        int substring = haystack.indexOf(needle);
        if (substring >= 0) {
            return new ScoredText(7_500 - Math.min(800, substring),
                    range(substring, substring + needle.length()));
        }

        List<Integer> characters = new ArrayList<>(needle.length());
        int cursor = 0;
        int gaps = 0;
        int previous = -1;
        for (int i = 0; i < needle.length(); i++) {
            int found = haystack.indexOf(needle.charAt(i), cursor);
            if (found == -1) return null;
            characters.add(found);
            if (previous != -1) gaps += found - previous - 1;
            previous = found;
            cursor = found + 1;
        }
        int startPenalty = characters.isEmpty() ? 0 : characters.get(0) * 4;
        return new ScoredText(5_500 - Math.min(2_000, gaps * 9 + startPenalty), characters);
    }

    private static int findWordPrefix(String haystack, String needle) {
        int index = haystack.indexOf(needle);
        while (index >= 0) {
            if (index == 0 || isBoundary(haystack.charAt(index - 1))) return index;
            index = haystack.indexOf(needle, index + 1);
        }
        return -1;
    }

    private static boolean isBoundary(char character) {
        return Character.isWhitespace(character) || character == '/' || character == '.'
                || character == '_' || character == '-' || character == '$';
    }

    private static List<Integer> range(int start, int end) {
        List<Integer> result = new ArrayList<>(Math.max(0, end - start));
        for (int i = start; i < end; i++) result.add(i);
        return List.copyOf(result);
    }

    static String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    public record Match(int score, List<Integer> matchedCharacters) {
        public Match {
            matchedCharacters = List.copyOf(matchedCharacters);
        }
    }

    private record ScoredText(int score, List<Integer> characters) {
    }
}
