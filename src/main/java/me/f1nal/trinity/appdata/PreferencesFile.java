package me.f1nal.trinity.appdata;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.appdata.keybindings.KeyBindingData;
import me.f1nal.trinity.decompiler.output.DecompilerFontEnum;
import me.f1nal.trinity.decompiler.output.number.NumberDisplayTypeEnum;
import me.f1nal.trinity.events.EventRefreshDecompilerText;
import me.f1nal.trinity.gui.windows.impl.xref.SearchMaxDisplay;
import me.f1nal.trinity.gui.viewport.FontManager;
import me.f1nal.trinity.theme.Theme;
import me.f1nal.trinity.theme.ThemeManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PreferencesFile extends AppDataFile {
    private DecompilerFontEnum decompilerFontEnum = DecompilerFontEnum.JETBRAINS_MONO;
    private NumberDisplayTypeEnum defaultNumberDisplayType = NumberDisplayTypeEnum.DECIMAL;
    private SearchMaxDisplay searchMaxDisplay = SearchMaxDisplay.MAX_200;
    private boolean decompilerHideComments = false;
    private boolean decompilerNormalizeText = false;
    private boolean decompilerEnumClass = false;
    private boolean autoviewXref = false;
    private String currentTheme;
    private final Set<KeyBindingData> keyBindingData = new HashSet<>();
    private final Map<String, Boolean> memorizedCheckboxes = new HashMap<>();
    private float fontSize = FontManager.DEFAULT_SIZE;

    public void setDecompilerNormalizeText(boolean decompilerNormalizeText) {
        this.decompilerNormalizeText = decompilerNormalizeText;
        Main.getEventBus().post(new EventRefreshDecompilerText(dc -> true));
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    public float getFontSize() {
        return Math.min(Math.max(fontSize, 12.F), 30.F);
    }

    public Map<String, Boolean> getMemorizedCheckboxes() {
        return memorizedCheckboxes;
    }

    public boolean isDecompilerNormalizeText() {
        return decompilerNormalizeText;
    }

    public String getCurrentTheme() {
        return currentTheme;
    }

    public void setDecompilerEnumClass(boolean decompilerEnumClass) {
        this.decompilerEnumClass = decompilerEnumClass;
    }

    public boolean isDecompilerEnumClass() {
        return decompilerEnumClass;
    }

    public void setCurrentTheme(Theme currentTheme) {
        this.currentTheme = currentTheme.getName();
    }

    @Override
    public void handleLoad() {
        ThemeManager themeManager = Main.getThemeManager();
        Theme theme = this.currentTheme == null ? null : themeManager.getTheme(this.currentTheme);
        if (theme == null) {
            this.setCurrentTheme(themeManager.getDefaultTheme());
            return;
        }
        themeManager.setCurrentTheme(theme);
    }

    public PreferencesFile(AppDataManager manager) {
        super("preferences", manager);
    }

    public void setSearchMaxDisplay(SearchMaxDisplay searchMaxDisplay) {
        this.searchMaxDisplay = searchMaxDisplay;
    }

    public SearchMaxDisplay getSearchMaxDisplay() {
        return searchMaxDisplay;
    }

    public void setDecompilerFontEnum(DecompilerFontEnum decompilerFontEnum) {
        this.decompilerFontEnum = decompilerFontEnum;
    }

    public DecompilerFontEnum getDecompilerFontEnum() {
        return decompilerFontEnum;
    }

    public void setDecompilerHideComments(boolean decompilerHideComments) {
        this.decompilerHideComments = decompilerHideComments;
        Main.getEventBus().post(new EventRefreshDecompilerText(dc -> true));
    }

    public boolean isDecompilerHideComments() {
        return decompilerHideComments;
    }

    public NumberDisplayTypeEnum getDefaultNumberDisplayType() {
        return defaultNumberDisplayType;
    }

    public void setDefaultNumberDisplayType(NumberDisplayTypeEnum defaultNumberDisplayType) {
        this.defaultNumberDisplayType = defaultNumberDisplayType;
    }

    public boolean isAutoviewXref() {
        return autoviewXref;
    }

    public void setAutoviewXref(boolean autoviewXref) {
        this.autoviewXref = autoviewXref;
    }

    public int getSearchLimit(int size) {
        return Math.min(size, searchMaxDisplay.getMax());
    }

    public Set<KeyBindingData> getKeyBindings() {
        return keyBindingData;
    }
}
