package me.f1nal.trinity.adapter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchMatchingTest {
    @Test
    void dexTextMatchingHonorsExactCaseAndEmptyFilters() {
        assertTrue(LiveDexService.matchesText("sample/DexMain", "DEXMAIN", false, false));
        assertFalse(LiveDexService.matchesText("sample/DexMain", "DEXMAIN", false, true));
        assertTrue(LiveDexService.matchesText("sample/DexMain", "SAMPLE/DEXMAIN", true, false));
        assertFalse(LiveDexService.matchesText("sample/DexMain", "DexMain", true, false));
        assertTrue(LiveDexService.matchesText("sample/DexMain", "", true, true));
    }

    @Test
    void projectScoringRestrictsExactMatchesWithoutLosingDisplayNames() {
        assertEquals(1000, LiveProjectService.score(
                "sample/Main.run()V", "ReadableRun", "readablerun", true, false));
        assertEquals(-1, LiveProjectService.score(
                "sample/Main.run()V", "ReadableRun", "Readable", true, true));
        assertTrue(LiveProjectService.score(
                "sample/Main.run()V", "ReadableRun", "Readable", false, true) > 0);
        assertEquals(-1, LiveProjectService.score(
                "sample/Main.run()V", "ReadableRun", "readable", false, true));
        assertEquals(0, LiveProjectService.score(
                "sample/Main.run()V", "ReadableRun", "", true, true));
    }
}
