package me.f1nal.trinity.application;

import java.util.List;

/** Immutable offset page used by every potentially large application query. */
public record Page<T>(List<T> items, int offset, int limit, int total, Integer nextOffset) {
    public Page {
        items = List.copyOf(items);
        if (offset < 0 || limit < 1 || total < 0) {
            throw new IllegalArgumentException("invalid page bounds");
        }
    }

    public static <T> Page<T> slice(List<T> values, int offset, int limit) {
        int start = Math.min(offset, values.size());
        int end = Math.min(values.size(), start + limit);
        return new Page<>(values.subList(start, end), offset, limit, values.size(),
                end < values.size() ? end : null);
    }
}
