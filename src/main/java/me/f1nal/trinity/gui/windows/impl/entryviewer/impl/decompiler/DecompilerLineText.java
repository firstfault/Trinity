package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import me.f1nal.trinity.theme.CodeColorScheme;

public class DecompilerLineText {
    private final String text;
    private final DecompilerComponent component;
    private float renderedMinX;
    private float renderedMinY;
    private float renderedMaxX;
    private float renderedMaxY;
    private boolean rendered;

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
                float minX = ImGui.getItemRectMinX() - spacing;
                float minY = ImGui.getItemRectMinY() - spacing;
                float maxX = ImGui.getItemRectMaxX() + spacing;
                float maxY = ImGui.getItemRectMaxY() + spacing;
                ImGui.getWindowDrawList().addRect(minX, minY, maxX, maxY,
                        CodeColorScheme.setAlpha(color, 80));
                ImGui.getWindowDrawList().addRectFilled(minX, minY, maxX, maxY,
                        CodeColorScheme.setAlpha(color, 20));
            }
        }

        this.captureRenderedBounds();
        ImGui.popStyleColor();
    }

    public void captureRenderedBounds() {
        this.renderedMinX = ImGui.getItemRectMinX();
        this.renderedMinY = ImGui.getItemRectMinY();
        this.renderedMaxX = ImGui.getItemRectMaxX();
        this.renderedMaxY = ImGui.getItemRectMaxY();
        this.rendered = true;
    }

    public boolean hasRenderedBounds() {
        return this.rendered;
    }

    public void clearRenderedBounds() {
        this.rendered = false;
    }

    public float getRenderedMinX() {
        return renderedMinX;
    }

    public float getRenderedMinY() {
        return renderedMinY;
    }

    public float getRenderedMaxX() {
        return renderedMaxX;
    }

    public float getRenderedMaxY() {
        return renderedMaxY;
    }
}
