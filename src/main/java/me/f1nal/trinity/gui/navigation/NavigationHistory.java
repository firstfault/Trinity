package me.f1nal.trinity.gui.navigation;

import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.object.DatabaseNavigationHistory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class NavigationHistory implements IDatabaseSavable<DatabaseNavigationHistory> {
    public static final int MAX_ENTRIES = 500;

    private final List<NavigationEntry> entries = new ArrayList<>();
    private final Runnable changeListener;
    private int currentIndex = -1;
    private long nextId;

    public NavigationHistory() {
        this(null);
    }

    public NavigationHistory(Runnable changeListener) {
        this.changeListener = changeListener;
    }

    public NavigationEntry record(NavigationTarget target, NavigationAction action) {
        return record(target, action, null);
    }

    public NavigationEntry record(NavigationTarget target, NavigationAction action, String displayText) {
        NavigationEntry current = getCurrent();
        if (current != null && current.target().equals(target)
                && Objects.equals(current.displayText(), displayText)) {
            return current;
        }
        if (currentIndex + 1 < entries.size()) {
            entries.subList(currentIndex + 1, entries.size()).clear();
        }
        NavigationEntry entry = new NavigationEntry(
                ++nextId, target, action, System.currentTimeMillis(), displayText);
        entries.add(entry);
        currentIndex = entries.size() - 1;
        trimToLimit();
        changed();
        return entry;
    }

    public Optional<NavigationEntry> back() {
        if (!canGoBack()) return Optional.empty();
        NavigationEntry entry = entries.get(--currentIndex);
        changed();
        return Optional.of(entry);
    }

    public Optional<NavigationEntry> forward() {
        if (!canGoForward()) return Optional.empty();
        NavigationEntry entry = entries.get(++currentIndex);
        changed();
        return Optional.of(entry);
    }

    public Optional<NavigationEntry> select(int index) {
        if (index < 0 || index >= entries.size()) return Optional.empty();
        currentIndex = index;
        changed();
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
        if (entries.isEmpty() && currentIndex == -1) return;
        entries.clear();
        currentIndex = -1;
        changed();
    }

    public void reset() {
        entries.clear();
        currentIndex = -1;
        nextId = 0L;
    }

    public void restore(List<NavigationEntry> restoredEntries, int restoredCurrentIndex) {
        entries.clear();
        int firstEntry = Math.max(0, restoredEntries.size() - MAX_ENTRIES);
        entries.addAll(restoredEntries.subList(firstEntry, restoredEntries.size()));
        if (entries.isEmpty()) {
            currentIndex = -1;
        } else {
            currentIndex = Math.max(0, Math.min(entries.size() - 1,
                    restoredCurrentIndex - firstEntry));
        }
        nextId = entries.stream()
                .max(Comparator.comparingLong(NavigationEntry::id))
                .map(NavigationEntry::id)
                .orElse(0L);
    }

    private void trimToLimit() {
        int removeCount = entries.size() - MAX_ENTRIES;
        if (removeCount <= 0) return;
        entries.subList(0, removeCount).clear();
        currentIndex -= removeCount;
    }

    private void changed() {
        if (changeListener != null) changeListener.run();
    }

    @Override
    public DatabaseNavigationHistory createDatabaseObject() {
        return new DatabaseNavigationHistory(entries, currentIndex);
    }
}
