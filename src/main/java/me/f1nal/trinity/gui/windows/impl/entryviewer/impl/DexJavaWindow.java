package me.f1nal.trinity.gui.windows.impl.entryviewer.impl;

import imgui.ImGui;
import imgui.extension.texteditor.TextEditor;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.dex.DexClassEntry;
import me.f1nal.trinity.execution.dex.DexJavaDecompiler;
import me.f1nal.trinity.gui.windows.impl.entryviewer.ArchiveEntryViewerWindow;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Read-only JADX source projection. Native smali remains the editable representation. */
public final class DexJavaWindow extends ArchiveEntryViewerWindow<DexClassEntry> {
    private final TextEditor editor = new TextEditor();
    private final CompletableFuture<DexJavaDecompiler.ClassView> decompilation;
    private boolean loaded;
    private String error;

    public DexJavaWindow(Trinity trinity, DexClassEntry entry) {
        super(trinity, entry);
        editor.setReadOnlyEnabled(true);
        editor.setShowWhitespacesEnabled(false);
        List<DexJavaDecompiler.Input> dexInputs = trinity.getExecution().getDexIndex()
                .getFiles().stream()
                .map(file -> new DexJavaDecompiler.Input(file.getName(), file.getBytes()))
                .toList();
        String internalName = entry.getInternalName();
        DexJavaDecompiler.Workspace decompiler = trinity.getExecution().getDexIndex()
                .getJavaDecompiler();
        decompilation = CompletableFuture.supplyAsync(
                () -> decompiler.decompile(dexInputs, internalName));
    }

    @Override
    public String getTitle() {
        return super.getTitle() + " - Java";
    }

    @Override
    protected void renderFrame() {
        applyResult();
        if (!loaded) {
            if (error == null) {
                ImGui.textDisabled("Decompiling with JADX...");
            } else {
                ImGui.textWrapped("Unable to decompile this DEX class: " + error);
            }
            return;
        }
        editor.render(getTitle());
    }

    private void applyResult() {
        if (loaded || error != null || !decompilation.isDone()) return;
        try {
            editor.setText(decompilation.join().source());
            loaded = true;
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            String message = cause == null ? exception.getMessage() : cause.getMessage();
            error = message == null ? exception.getClass().getSimpleName() : message;
        }
    }
}
