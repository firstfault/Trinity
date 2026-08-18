package me.f1nal.trinity.gui.navigation;

public record NavigationEntry(long id, NavigationTarget target, NavigationAction action,
                              long timestampMillis, String displayText,
                              NavigationViewState viewState) {
    public NavigationEntry(long id, NavigationTarget target, NavigationAction action, long timestampMillis) {
        this(id, target, action, timestampMillis, null, null);
    }

    public NavigationEntry(long id, NavigationTarget target, NavigationAction action,
                           long timestampMillis, String displayText) {
        this(id, target, action, timestampMillis, displayText, null);
    }

    public NavigationEntry withViewState(NavigationViewState viewState) {
        return new NavigationEntry(id, target, action, timestampMillis, displayText, viewState);
    }
}
