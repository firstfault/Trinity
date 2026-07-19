package me.f1nal.trinity.appdata;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.appdata.keybindings.KeyBindingData;
import me.f1nal.trinity.decompiler.output.FontEnum;
import me.f1nal.trinity.decompiler.output.number.NumberDisplayTypeEnum;
import me.f1nal.trinity.events.EventRefreshDecompilerText;
import me.f1nal.trinity.gui.components.FontSettings;
import me.f1nal.trinity.gui.windows.impl.xref.SearchMaxDisplay;
import me.f1nal.trinity.gui.viewport.FontManager;
import me.f1nal.trinity.theme.AccentColor;
import me.f1nal.trinity.theme.Theme;
import me.f1nal.trinity.theme.ThemeManager;
import me.f1nal.trinity.theme.TrinityStyle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PreferencesFile extends AppDataFile {
    private NumberDisplayTypeEnum defaultNumberDisplayType = NumberDisplayTypeEnum.DECIMAL;
    private SearchMaxDisplay searchMaxDisplay = SearchMaxDisplay.MAX_200;
    private boolean decompilerHideComments = false;
    private boolean decompilerNormalizeText = false;
    private boolean decompilerEnumClass = false;
    private boolean autoviewXref = false;
    private boolean assemblerHideMetadata = false;
    private AccentColor accentColor = AccentColor.SAPPHIRE;
    private String currentTheme;
    private final Set<KeyBindingData> keyBindingData = new HashSet<>();
    private final Map<String, Boolean> memorizedCheckboxes = new HashMap<>();
    @XStreamConverter(FontSettings.FontSettingsConverter.class)
    private final FontSettings decompilerFont = new FontSettings(FontEnum.JETBRAINS_MONO, 16.F, "Decompiler");
    @XStreamConverter(FontSettings.FontSettingsConverter.class)
    private final FontSettings defaultFont = new FontSettings(FontEnum.INTER, 15.F, "Default");

    public PreferencesFile(AppDataManager manager) {
        super("preferences", manager);
        this.addAlias(FontSettings.class, "fontSetting");
        this.addAlias(KeyBindingData.class, "keyBinding");
    }

    public void setDecompilerNormalizeText(boolean decompilerNormalizeText) {
        this.decompilerNormalizeText = decompilerNormalizeText;
        Main.getEventBus().post(new EventRefreshDecompilerText(dc -> true));
    }

    public FontSettings getDefaultFont() {
        return defaultFont;
    }

    public FontSettings getDecompilerFont() {
        return decompilerFont;
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

    public AccentColor getAccentColor() {
        return accentColor == null ? AccentColor.SAPPHIRE : accentColor;
    }

    public void setAccentColor(AccentColor accentColor) {
        this.accentColor = accentColor == null ? AccentColor.SAPPHIRE : accentColor;
        TrinityStyle.applyAccent(this.accentColor);
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
        Main.getKeyBindManager().load(this.keyBindingData);
        ThemeManager themeManager = Main.getThemeManager();
        Theme theme = this.currentTheme == null ? null : themeManager.getTheme(this.currentTheme);
        if (theme == null) {
            theme = themeManager.getDefaultTheme();
        }
        themeManager.setCurrentTheme(theme);
    }

    public void setSearchMaxDisplay(SearchMaxDisplay searchMaxDisplay) {
        this.searchMaxDisplay = searchMaxDisplay;
    }

    public SearchMaxDisplay getSearchMaxDisplay() {
        return searchMaxDisplay;
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

    public boolean isAssemblerHideMetadata() {
        return assemblerHideMetadata;
    }

    public void setAssemblerHideMetadata(boolean assemblerHideMetadata) {
        this.assemblerHideMetadata = assemblerHideMetadata;
    }

    public int getSearchLimit(int size) {
        return Math.min(size, searchMaxDisplay.getMax());
    }

    public Set<KeyBindingData> getKeyBindings() {
        return keyBindingData;
    }

    public void setKeyBinding(KeyBindingData binding) {
        this.keyBindingData.removeIf(existing -> existing.getShortName().equals(binding.getShortName()));
        this.keyBindingData.add(binding);
    }

    public void removeKeyBinding(String identifier) {
        this.keyBindingData.removeIf(binding -> binding.getShortName().equals(identifier));
    }
}
