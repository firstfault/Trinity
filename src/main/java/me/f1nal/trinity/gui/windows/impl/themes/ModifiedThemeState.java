package me.f1nal.trinity.gui.windows.impl.themes;

import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.theme.Theme;

public class ModifiedThemeState {
    private final Theme theme;
    private final int[] originalColors;

    public ModifiedThemeState(Theme theme) {
        this.theme = theme;
        this.originalColors = new int[theme.getColors().size()];
        for (int i = 0; i < this.originalColors.length; i++) {
            this.originalColors[i] = CodeColorScheme.getRgb(theme.getColors().get(i).getRgba());
        }
    }

    public void revertTheme() {
        for (int i = 0; i < this.theme.getColors().size(); i++) {
            this.theme.getColors().get(i).setRgba(CodeColorScheme.toRgba(this.originalColors[i]));
        }
    }

    public Theme getTheme() {
        return theme;
    }
}
