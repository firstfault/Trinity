package me.f1nal.trinity.gui.windows.impl.classstructure.nodes;

import me.f1nal.trinity.gui.windows.impl.classstructure.StructureKind;
import me.f1nal.trinity.gui.windows.impl.cp.BrowserViewerNode;
import me.f1nal.trinity.gui.windows.impl.cp.RenameHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassStructureNodeTest {
    @Test
    void themeRefreshPreservesViewerAndTreeState() {
        TestNode root = new TestNode();
        TestNode child = new TestNode();
        root.addChild(child);
        BrowserViewerNode viewer = root.getBrowserViewerNode();
        child.getBrowserViewerNode();
        viewer.setDefaultOpen(true);

        root.refreshTheme();

        assertSame(viewer, root.getBrowserViewerNode());
        assertTrue(root.getBrowserViewerNode().isDefaultOpen());
        assertEquals(1, root.refreshCount);
        assertEquals(1, child.refreshCount);
    }

    private static final class TestNode extends ClassStructureNode {
        private int refreshCount;

        private TestNode() {
            super("");
        }

        @Override
        protected void refreshTheme(BrowserViewerNode node) {
            this.refreshCount++;
        }

        @Override
        protected RenameHandler getRenameFunction() {
            return null;
        }

        @Override
        protected String getText() {
            return "test";
        }

        @Override
        public StructureKind getKind() {
            return StructureKind.CLASSES;
        }
    }
}
