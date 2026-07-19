package me.f1nal.trinity.gui.windows.impl.entryviewer.impl;

import imgui.ImGui;
import imgui.extension.texteditor.TextEditor;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.packages.ResourceArchiveEntry;
import me.f1nal.trinity.gui.windows.impl.entryviewer.ArchiveEntryViewerWindow;
import me.f1nal.trinity.gui.viewport.notifications.ICaption;

public class TextEditorWindow extends ArchiveEntryViewerWindow<ResourceArchiveEntry> implements ICaption {
    private final TextEditor textEditor = new TextEditor();

    public TextEditorWindow(Trinity trinity, ResourceArchiveEntry archiveEntry) {
        super(trinity, archiveEntry);
        textEditor.setText(new String(archiveEntry.getBytes()));
        textEditor.setShowWhitespacesEnabled(false);
        this.windowFlags |= ImGuiWindowFlags.MenuBar;
    }

    @Override
    protected void renderFrame() {
        if (ImGui.beginMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("Save")) {
                    this.saveBytes(textEditor.getText().getBytes(), this);
                }

                ImGui.endMenu();
            }
            if (ImGui.beginMenu("Edit")) {
                final boolean ro = textEditor.isReadOnlyEnabled();
                if (ImGui.menuItem("Read-only mode", "", ro)) {
                    textEditor.setReadOnlyEnabled(!ro);
                }

                ImGui.separator();

                if (ImGui.menuItem("Undo", "ALT-Backspace", !ro && textEditor.canUndo())) {
                    textEditor.undo();
                }
                if (ImGui.menuItem("Redo", "Ctrl-Y", !ro && textEditor.canRedo())) {
                    textEditor.redo();
                }

                ImGui.separator();

                if (ImGui.menuItem("Copy", "Ctrl-C", textEditor.anyCursorHasSelection())) {
                    textEditor.copy();
                }
                if (ImGui.menuItem("Cut", "Ctrl-X", !ro && textEditor.anyCursorHasSelection())) {
                    textEditor.cut();
                }
                if (ImGui.menuItem("Delete", "Del", !ro && textEditor.anyCursorHasSelection())) {
                    textEditor.replaceTextInAllCursors("");
                }
                if (ImGui.menuItem("Paste", "Ctrl-V", !ro && ImGui.getClipboardText() != null)) {
                    textEditor.paste();
                }

                ImGui.endMenu();
            }

            ImGui.endMenuBar();
        }

        textEditor.render(this.getTitle());
    }

    @Override
    public String getCaption() {
        return "Text Editor";
    }
}
