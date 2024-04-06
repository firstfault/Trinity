package me.f1nal.trinity.decompiler.output.lines;

import me.f1nal.trinity.decompiler.output.component.AbstractTextComponent;
import me.f1nal.trinity.gui.frames.impl.entryviewer.impl.decompiler.DecompilerHighlight;
import me.f1nal.trinity.gui.frames.impl.entryviewer.impl.decompiler.DecompilerWindow;
import imgui.ImGui;

import java.util.ArrayList;
import java.util.List;

public class ComponentGroup {
    private final AbstractTextComponent component;
    private final List<LineText> lineTextList = new ArrayList<>();

    public void addText(LineText text) {
        this.lineTextList.add(text);
    }

    public void render(DecompilerWindow window) {
        for (LineText lineText : lineTextList) {
            lineText.render();

            if (window.hoveredGroup == null && ImGui.isItemHovered()) {
                window.hoveredGroup = this;
            }
        }

        component.handleAfterDrawing();

        DecompilerHighlight highlight = window.getHighlight();
        if (highlight != null && highlight.getTextComponent() == this.getComponent()) {
            if (!highlight.isScrolled()) {
                highlight.setScrolled(true);
                if (!ImGui.isItemVisible()) {
                    ImGui.setScrollHereY();
                }
            }

            if (highlight.isFinished()) {
                return;
            }

            ImGui.getWindowDrawList().addRectFilled(0, ImGui.getItemRectMinY(), 0x10000, ImGui.getItemRectMaxY() + 2, highlight.getColor());
        }
    }

    public AbstractTextComponent getComponent() {
        return component;
    }

    public ComponentGroup(AbstractTextComponent component) {
        this.component = component;
    }
}
