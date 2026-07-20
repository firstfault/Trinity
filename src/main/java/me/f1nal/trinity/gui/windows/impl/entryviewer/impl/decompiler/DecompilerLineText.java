package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import me.f1nal.trinity.theme.CodeColorScheme;

public class DecompilerLineText {
    private final String text;
    private final DecompilerComponent component;
    private ImVec2 renderedMin;
    private ImVec2 renderedMax;

    public DecompilerLineText(String text, DecompilerComponent component) {
        this.text = text;
        this.component = component;
    }

    public DecompilerComponent getComponent() {
        return component;
    }

    public String getText() {
        return text;
    }

    public void render(boolean highlighted) {
        final DecompilerComponentRenameState renameState = component.getRenameState();
        final int color = this.component.getColorFunction().get();
        ImGui.pushStyleColor(ImGuiCol.Text, color);

        if (renameState != null) {
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0.F, 0.F);
            ImGui.pushStyleColor(ImGuiCol.FrameBg, ImGui.getColorU32(ImGuiCol.WindowBg));

            renameState.drawInputBox();

            ImGui.popStyleVar();
            ImGui.popStyleColor();
        } else {
            ImGui.text(text);

            if (highlighted) {
                float spacing = 1.F;
                final ImVec2 min = ImGui.getItemRectMin().minus(spacing, spacing);
                spacing *= 2.F;
                final ImVec2 size = ImGui.getItemRectSize().plus(spacing, spacing);
                ImGui.getWindowDrawList().addRect(min.x, min.y, min.x + size.x, min.y + size.y, CodeColorScheme.setAlpha(color, 80));
                ImGui.getWindowDrawList().addRectFilled(min.x, min.y, min.x + size.x, min.y + size.y, CodeColorScheme.setAlpha(color, 20));
            }
        }

        this.captureRenderedBounds();
        ImGui.popStyleColor();
    }

    public void captureRenderedBounds() {
        ImVec2 min = ImGui.getItemRectMin();
        ImVec2 max = ImGui.getItemRectMax();
        this.renderedMin = new ImVec2(min.x, min.y);
        this.renderedMax = new ImVec2(max.x, max.y);
    }

    public boolean hasRenderedBounds() {
        return this.renderedMin != null && this.renderedMax != null;
    }

    public ImVec2 getRenderedMin() {
        return renderedMin;
    }

    public ImVec2 getRenderedMax() {
        return renderedMax;
    }
}
