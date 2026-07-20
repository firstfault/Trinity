package me.f1nal.trinity.gui.navigation;

public record NavigationEntry(long id, NavigationTarget target, NavigationAction action, long timestampMillis) {
}
