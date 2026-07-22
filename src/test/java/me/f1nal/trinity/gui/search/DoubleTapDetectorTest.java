package me.f1nal.trinity.gui.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoubleTapDetectorTest {
    @Test
    void acceptsTwoTapsInsideTheConfiguredWindow() {
        DoubleTapDetector detector = new DoubleTapDetector(350L);

        assertFalse(detector.tap(1_000L));
        assertTrue(detector.tap(1_300L));
    }

    @Test
    void expiredTapBecomesTheStartOfTheNextPair() {
        DoubleTapDetector detector = new DoubleTapDetector(350L);

        assertFalse(detector.tap(1_000L));
        assertFalse(detector.tap(1_351L));
        assertTrue(detector.tap(1_600L));
    }

    @Test
    void successfulPairDoesNotLeakIntoAThirdTap() {
        DoubleTapDetector detector = new DoubleTapDetector(350L);

        assertFalse(detector.tap(1_000L));
        assertTrue(detector.tap(1_100L));
        assertFalse(detector.tap(1_200L));
    }
}
