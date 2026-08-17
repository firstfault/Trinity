package me.f1nal.trinity.gui.windows.impl.assembler.inline;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Stable fuzzy ranking shared by opcode and operand completion. */
public final class InlineSuggestionMatcher {
    private InlineSuggestionMatcher() {
    }

    public static List<String> closest(Collection<String> candidates, String query, int limit) {
        if (candidates == null || candidates.isEmpty() || limit <= 0) return List.of();
        if (query == null || query.trim().isEmpty()) return List.of();
        return ranked(candidates, query).stream().limit(limit).toList();
    }

    public static List<String> ranked(Collection<String> candidates, String query) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        String search = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate != null && !candidate.isBlank())
                .distinct()
                .filter(candidate -> search.isEmpty() || matches(candidate, search))
                .sorted(Comparator.comparingInt((String candidate) -> search.isEmpty() ? 0 : rank(candidate, search))
                        .thenComparingInt(String::length)
                        .thenComparing(String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static boolean matches(String candidate, String search) {
        String value = candidate.toLowerCase(Locale.ROOT);
        for (String part : search.split("\\s+")) {
            if (!value.contains(part)) return false;
        }
        return true;
    }

    private static int rank(String candidate, String search) {
        String value = candidate.toLowerCase(Locale.ROOT);
        if (value.equals(search)) return 0;
        if (value.startsWith(search)) return 100 + value.length() - search.length();
        int contains = value.indexOf(search);
        if (contains >= 0) return 300 + contains * 4 + value.length() - search.length();
        return 1_000 + levenshtein(value, search) * 20 + Math.abs(value.length() - search.length());
    }

    static int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int i = 0; i <= right.length(); i++) previous[i] = i;
        for (int l = 1; l <= left.length(); l++) {
            current[0] = l;
            for (int r = 1; r <= right.length(); r++) {
                int cost = left.charAt(l - 1) == right.charAt(r - 1) ? 0 : 1;
                current[r] = Math.min(Math.min(current[r - 1] + 1, previous[r] + 1),
                        previous[r - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }
}
