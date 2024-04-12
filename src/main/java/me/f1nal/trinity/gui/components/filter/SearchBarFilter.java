package me.f1nal.trinity.gui.components.filter;

import imgui.ImGui;
import me.f1nal.trinity.gui.components.SearchBar;
import me.f1nal.trinity.util.SearchTermMatchable;

import java.util.Collection;
import java.util.function.Predicate;

public class SearchBarFilter<T extends SearchTermMatchable> extends Filter<T> {
    private final SearchBar searchBar;

    public SearchBarFilter(SearchBar searchBar) {
        this.searchBar = searchBar;
    }

    public SearchBarFilter() {
        this(new SearchBar());
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
        return search.isEmpty() ? (text) -> true : (text) -> text.matches(search);
    }

    @Override
    public boolean draw() {
        return searchBar.draw();
    }
}
