package me.f1nal.trinity.appdata;

import me.f1nal.trinity.theme.AccentColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreferencesFileTest {
    @Test
    void persistsAccentColor() {
        PreferencesFile preferences = new PreferencesFile(null);
        preferences.setAccentColor(AccentColor.VIOLET);
        String xml = preferences.serialize();

        PreferencesFile restored = new PreferencesFile(null);
        restored.getStream().fromXML(xml, restored);

        assertEquals(AccentColor.VIOLET, restored.getAccentColor());
    }
}
