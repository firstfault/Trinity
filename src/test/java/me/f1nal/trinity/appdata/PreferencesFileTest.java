package me.f1nal.trinity.appdata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreferencesFileTest {
    @Test
    void decompilerThreadDefaultTracksAvailableProcessorsAndValuesAreClamped() {
        assertEquals(Math.max(1, Runtime.getRuntime().availableProcessors() - 1),
                PreferencesFile.defaultDecompilerThreads());

        PreferencesFile preferences = new PreferencesFile(null);
        preferences.setDecompilerThreads(0);
        assertEquals(1, preferences.getDecompilerThreads());
        preferences.setDecompilerThreads(Integer.MAX_VALUE);
        assertEquals(256, preferences.getDecompilerThreads());
    }
}
