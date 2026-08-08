package me.f1nal.trinity.gui.windows.impl.entryviewer.impl;
import imgui.ImGui;

import imgui.extension.texteditor.TextEditor;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.dex.DexClassEntry;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.execution.dex.DexEditor;
import me.f1nal.trinity.execution.dex.DexFileUnit;
import me.f1nal.trinity.execution.dex.DexIndex;
import me.f1nal.trinity.gui.windows.impl.entryviewer.ArchiveEntryViewerWindow;
import me.f1nal.trinity.gui.viewport.notifications.ICaption;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.util.ByteUtil;

/** Editable native smali view that atomically rebuilds the containing DEX file. */
public final class DexSmaliWindow extends ArchiveEntryViewerWindow<DexClassEntry>
        implements ICaption {
    private final TextEditor editor = new TextEditor();
    private String loadedSmali;

    public DexSmaliWindow(Trinity trinity, DexClassEntry entry) {
        super(trinity, entry);
        loadedSmali = entry.disassemble();
        editor.setText(loadedSmali);
        editor.setShowWhitespacesEnabled(false);
        this.windowFlags |= ImGuiWindowFlags.MenuBar;
    }

    @Override
    protected void renderFrame() {
        if (ImGui.beginMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("Validate and Save", "Ctrl-S")) save();
                ImGui.endMenu();
            }
            if (ImGui.beginMenu("Edit")) {
                if (ImGui.menuItem("Undo", "ALT-Backspace", editor.canUndo())) editor.undo();
                if (ImGui.menuItem("Redo", "Ctrl-Y", editor.canRedo())) editor.redo();
                ImGui.separator();
                if (ImGui.menuItem("Copy", "Ctrl-C", editor.anyCursorHasSelection())) editor.copy();
                if (ImGui.menuItem("Cut", "Ctrl-X", editor.anyCursorHasSelection())) editor.cut();
                if (ImGui.menuItem("Delete", "Del", editor.anyCursorHasSelection())) {
                    editor.replaceTextInAllCursors("");
                }
                if (ImGui.menuItem("Paste", "Ctrl-V", ImGui.getClipboardText() != null)) {
                    editor.paste();
                }
                ImGui.endMenu();
            }
            ImGui.endMenuBar();
        }
        editor.render(getTitle());
    }

    private void save() {
        try {
            DexIndex index = trinity.getExecution().getDexIndex();
            DexClassEntry current = index.getClass(getArchiveEntry().getInternalName());
            if (current == null) {
                notify(NotificationType.ERROR, "DEX class is no longer in the project");
                return;
            }
            if (current != getArchiveEntry() && !current.disassemble().equals(loadedSmali)) {
                notify(NotificationType.ERROR,
                        "DEX class changed after this editor opened; reopen it before saving");
                return;
            }

            DexEditor.Candidate candidate = DexEditor.replaceClass(current, editor.getText());
            if (!candidate.valid()) {
                DexEditor.Diagnostic first = candidate.diagnostics().get(0);
                String location = first.line() <= 0 ? ""
                        : String.format(" at line %d, column %d", first.line(), first.column());
                notify(NotificationType.ERROR, "Invalid smali" + location + ": " + first.message());
                return;
            }
            DexFileUnit replacement = index.parse(candidate.dexFile(), candidate.bytes());
            index.replace(current.getFile(), replacement);
            setArchiveEntry(index.getClass(candidate.target()));
            loadedSmali = getArchiveEntry().disassemble();
            editor.setText(loadedSmali);
            notify(NotificationType.SUCCESS, String.format("Rebuilt %s (%s)",
                    candidate.dexFile(),
                    ByteUtil.getHumanReadableByteCountSI(candidate.bytes().length)));
        } catch (Exception exception) {
            notify(NotificationType.ERROR, "Failed to rebuild DEX: " + exception.getMessage());
        }
    }

    private void notify(NotificationType type, String message) {
        Main.getDisplayManager().addNotification(new Notification(type, this,
                ColoredStringBuilder.create().fmt("{}", message).get()));
    }

    @Override
    public String getCaption() {
        return "DEX Smali Editor";
    }
}
