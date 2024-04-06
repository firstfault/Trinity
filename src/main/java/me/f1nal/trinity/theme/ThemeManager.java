package me.f1nal.trinity.theme;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.logging.Logging;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    private final List<Theme> themes = new ArrayList<>();
    private Theme currentTheme;
    private final Theme defaultTheme;

    public ThemeManager() {
        this.defaultTheme = this.currentTheme = new Theme("Trinity Dark", false);
        this.themes.add(defaultTheme);
    }

    public void setCurrentTheme(Theme currentTheme) {
        if (!this.getThemes().contains(currentTheme)) {
            Logging.warn("Attempted to set removed theme as the current theme: {}", currentTheme.getName());
            return;
        }
        this.currentTheme = currentTheme;
        for (ThemeColor color : this.currentTheme.getColors()) {
            color.getCodeColor().setColor(CodeColorScheme.getRgb(color.getRgba()));
        }
        Main.getAppDataManager().getPreferences().setCurrentTheme(currentTheme);
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public Theme getDefaultTheme() {
        return defaultTheme;
    }

    public List<Theme> getThemes() {
        return themes;
    }

    public Theme getTheme(String name) {
        return getThemes().stream().filter(theme -> theme.getName().equals(name)).findFirst().orElse(null);
    }

    public boolean addTheme(Theme theme) {
        if (getTheme(theme.getName()) != null) {
            Logging.warn("Attempted to add duplicate theme '{}'", theme.getName());
            return false;
        }
        return this.themes.add(theme);
    }

    public void deleteThemePermanently(Theme theme) {
        this.deleteTheme(theme);
        File themeFile = Main.getAppDataManager().getThemeFile(theme);
        if (themeFile.exists()) {
            themeFile.delete();
        }
    }

    private void deleteTheme(Theme theme) {
        this.themes.remove(theme);
        if (this.getCurrentTheme() == theme) this.setCurrentTheme(this.getDefaultTheme());
    }

    public void clearThemes() {
        List<Theme> removal = new ArrayList<>();
        for (Theme theme : themes) {
            if (theme.isEditable()) removal.add(theme);
        }
        for (Theme theme : removal) {
            this.deleteTheme(theme);
        }
    }
}
