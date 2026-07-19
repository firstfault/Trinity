package me.f1nal.trinity.gui.components.filter;

import imgui.ImGui;
import me.f1nal.trinity.gui.components.SearchBar;
import me.f1nal.trinity.util.SearchTermMatchable;

import java.util.Collection;
import java.util.function.Predicate;

public class SearchBarFilter<T extends SearchTermMatchable> extends Filter<T> {
    private final SearchBar searchBar;
    private final boolean showCaseSensitivityToggle;

    public SearchBarFilter(SearchBar searchBar) {
        this(searchBar, false);
    }

    public SearchBarFilter(SearchBar searchBar, boolean showCaseSensitivityToggle) {
        this.searchBar = searchBar;
        this.showCaseSensitivityToggle = showCaseSensitivityToggle;
    }

    public SearchBarFilter() {
        this(new SearchBar());
    }

    public SearchBarFilter(boolean showCaseSensitivityToggle) {
        this(new SearchBar(), showCaseSensitivityToggle);
    }

    public SearchBar getSearchBar() {
        return searchBar;
    }

    @Override
    public void initialize(Collection<T> collection) {
    }

    @Override
    public Predicate<T> filter() {
        final String search = searchBar.getSearchText().get();
        if (search.isEmpty()) return text -> true;
        return searchBar.isCaseSensitive()
                ? text -> text.matches(search)
                : text -> text.matchesIgnoreCase(search);
    }

    @Override
    public boolean draw() {
        return this.showCaseSensitivityToggle
                ? searchBar.drawWithCaseSensitivityToggle()
                : searchBar.draw();
    }
}
