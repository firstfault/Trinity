package me.f1nal.trinity.gui.windows.api;

import me.f1nal.trinity.gui.windows.WindowManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClosableWindowDisposalTest {
    @Test
    void closeDisposesTheWindowExactlyOnce() {
        TestWindow window = new TestWindow();
        window.setVisible(true);

        window.close();
        window.close();

        assertTrue(window.isCloseRequested());
        assertTrue(window.isDisposed());
        assertFalse(window.isVisible());
        assertEquals(1, window.disposals);
    }

    @Test
    void removingVisibilityIsTerminalForAClosableWindow() {
        TestWindow window = new TestWindow();
        window.setVisible(true);

        window.setVisible(false);
        window.setVisible(true);

        assertTrue(window.isDisposed());
        assertFalse(window.isVisible());
        assertEquals(1, window.disposals);
    }

    @Test
    void rejectedDuplicateWindowIsDisposed() {
        WindowManager manager = new WindowManager(null);
        TestWindow original = new TestWindow();
        TestWindow duplicate = new TestWindow();

        manager.addClosableWindow(original);
        manager.addClosableWindow(duplicate);

        assertEquals(1, manager.getClosableWindows().size());
        assertSame(original, manager.getClosableWindows().get(0));
        assertTrue(original.isVisible());
        assertFalse(original.isDisposed());
        assertTrue(duplicate.isDisposed());
        assertEquals(1, duplicate.disposals);
    }

    private static final class TestWindow extends ClosableWindow {
        private int disposals;

        private TestWindow() {
            super("Test", 100.F, 100.F, null);
        }

        @Override
        protected void renderFrame() {
        }

        @Override
        protected void removeFromWindowManager() {
            // Tests do not install the global DisplayManager.
        }

        @Override
        protected void onDispose() {
            disposals++;
        }

        @Override
        public boolean isAlreadyOpen(ClosableWindow otherWindow) {
            return otherWindow instanceof TestWindow;
        }
    }
}
