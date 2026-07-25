package me.f1nal.trinity.execution.packages;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.dex.DexClassEntry;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.gui.windows.impl.entryviewer.ArchiveEntryViewerWindow;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.HexEditWindow;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.DexJavaWindow;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.DexSmaliWindow;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.TextEditorWindow;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerWindow;
import me.f1nal.trinity.util.INameable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public enum ArchiveEntryViewerType implements INameable {
    DECOMPILER("Decompiler", e -> e instanceof ClassTarget, DecompilerWindow.class, e -> new DecompilerWindow((ClassTarget) e, Main.getTrinity())),
    DEX_JAVA("Java (JADX)", e -> e instanceof DexClassEntry, DexJavaWindow.class,
            e -> new DexJavaWindow(Main.getTrinity(), (DexClassEntry) e)),
    DEX_SMALI("Smali", e -> e instanceof DexClassEntry, DexSmaliWindow.class,
            e -> new DexSmaliWindow(Main.getTrinity(), (DexClassEntry) e)),
    TEXT_VIEWER("Text Viewer", e -> e instanceof ResourceArchiveEntry, TextEditorWindow.class, e -> new TextEditorWindow(Main.getTrinity(), (ResourceArchiveEntry) e)),
    HEX_VIEWER("Hex Viewer", e -> e instanceof ResourceArchiveEntry, HexEditWindow.class, e -> new HexEditWindow(Main.getTrinity(), (ResourceArchiveEntry) e)),
    ;

    private final String name;
    private final Predicate<ArchiveEntry> valid;
    private final Class<? extends ArchiveEntryViewerWindow<?>> viewerClass;
    private final Function<ArchiveEntry, ArchiveEntryViewerWindow<?>> windowFunction;

    ArchiveEntryViewerType(String name, Predicate<ArchiveEntry> valid, Class<? extends ArchiveEntryViewerWindow<?>> viewerClass, Function<ArchiveEntry, ArchiveEntryViewerWindow<?>> windowFunction) {
        this.name = name;
        this.valid = valid;
        this.viewerClass = viewerClass;
        this.windowFunction = windowFunction;
    }

    @Override
    public String getName() {
        return name;
    }

    public Predicate<ArchiveEntry> getValid() {
        return valid;
    }

    public ArchiveEntryViewerWindow<?> getWindow(ArchiveEntry archiveEntry) {
        List<ClosableWindow> windows = Main.getWindowManager().getWindows(w -> w.getClass() == this.viewerClass);

        for (ClosableWindow window : windows) {
            ArchiveEntryViewerWindow<?> viewerWindow = (ArchiveEntryViewerWindow<?>) window;

            if (viewerWindow.getArchiveEntry() == archiveEntry) {
                return viewerWindow;
            }
        }

        return this.windowFunction.apply(archiveEntry);
    }
}
