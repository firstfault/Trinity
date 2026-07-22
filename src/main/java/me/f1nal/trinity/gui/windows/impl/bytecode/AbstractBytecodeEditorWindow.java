package me.f1nal.trinity.gui.windows.impl.bytecode;

import imgui.ImGui;
import imgui.flag.ImGuiFocusedFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.theme.CodeColorScheme;

abstract class AbstractBytecodeEditorWindow extends ClosableWindow {
    private String error;
    private String savedState;

    protected AbstractBytecodeEditorWindow(String title, Trinity trinity) {
        super(title, 760.F, 650.F, trinity);
        this.windowFlags |= ImGuiWindowFlags.HorizontalScrollbar;
        this.setDialog(true);
    }

    @Override
    public final void render() {
        String currentState = stateFingerprint();
        if (savedState == null) {
            savedState = currentState;
        }
        if (savedState.equals(currentState)) {
            this.windowFlags &= ~ImGuiWindowFlags.UnsavedDocument;
        } else {
            this.windowFlags |= ImGuiWindowFlags.UnsavedDocument;
        }
        super.render();
    }

    @Override
    protected final void renderFrame() {
        if (error != null) {
            ImGui.textColored(CodeColorScheme.NOTIFY_ERROR, error);
        }

        if (ImGui.beginChild(getId("EditorContent"), 0.F, 0.F, false, ImGuiWindowFlags.HorizontalScrollbar)) {
            drawEditor();
        }
        ImGui.endChild();

        if (ImGui.isWindowFocused(ImGuiFocusedFlags.RootAndChildWindows)
                && ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(ImGuiKey.S)) {
            try {
                saveChanges();
                savedState = stateFingerprint();
                this.windowFlags &= ~ImGuiWindowFlags.UnsavedDocument;
                close();
            } catch (Throwable throwable) {
                error = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
            }
        }
    }

    protected abstract void drawEditor();

    protected abstract String stateFingerprint();

    protected abstract void saveChanges();
}
