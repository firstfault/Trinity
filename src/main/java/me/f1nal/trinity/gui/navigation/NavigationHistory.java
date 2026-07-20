package me.f1nal.trinity.gui.navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class NavigationHistory {
    private final List<NavigationEntry> entries = new ArrayList<>();
    private int currentIndex = -1;
    private long nextId;

    public NavigationEntry record(NavigationTarget target, NavigationAction action) {
        if (currentIndex + 1 < entries.size()) {
            entries.subList(currentIndex + 1, entries.size()).clear();
        }
        NavigationEntry entry = new NavigationEntry(++nextId, target, action);
        entries.add(entry);
        currentIndex = entries.size() - 1;
        return entry;
    }

    public Optional<NavigationEntry> back() {
        if (!canGoBack()) return Optional.empty();
        return Optional.of(entries.get(--currentIndex));
    }

    public Optional<NavigationEntry> forward() {
        if (!canGoForward()) return Optional.empty();
        return Optional.of(entries.get(++currentIndex));
    }

    public Optional<NavigationEntry> select(int index) {
        if (index < 0 || index >= entries.size()) return Optional.empty();
        currentIndex = index;
        return Optional.of(entries.get(index));
    }

    public boolean canGoBack() {
        return currentIndex > 0;
    }

    public boolean canGoForward() {
        return currentIndex >= 0 && currentIndex + 1 < entries.size();
    }

    public NavigationEntry getCurrent() {
        return currentIndex < 0 ? null : entries.get(currentIndex);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public List<NavigationEntry> getEntries() {
        return List.copyOf(entries);
    }

    public void clear() {
        entries.clear();
        currentIndex = -1;
    }
}
