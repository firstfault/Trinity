package me.f1nal.trinity.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface SearchTermMatchable {
    boolean matches(String searchTerm);

    static <T extends SearchTermMatchable> List<T> match(String searchTerm, Collection<T> list) {
        List<T> copy = new ArrayList<>(list);
        copy.removeIf(matchable -> !searchTerm.isEmpty() && !matchable.matches(searchTerm));
        return copy;
    }
}
