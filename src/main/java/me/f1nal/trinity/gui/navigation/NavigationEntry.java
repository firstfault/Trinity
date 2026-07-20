package me.f1nal.trinity.gui.navigation;

public record NavigationEntry(long id, NavigationTarget target, NavigationAction action,
                              long timestampMillis, String displayText) {
    public NavigationEntry(long id, NavigationTarget target, NavigationAction action, long timestampMillis) {
        this(id, target, action, timestampMillis, null);
    }
}
