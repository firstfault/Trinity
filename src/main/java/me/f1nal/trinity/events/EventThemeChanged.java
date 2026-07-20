package me.f1nal.trinity.events;

import me.f1nal.trinity.theme.AccentColor;
import me.f1nal.trinity.theme.Theme;

/** Fired after either the active theme or its accent-derived palette changes. */
public record EventThemeChanged(Theme theme, AccentColor accentColor) {
}
