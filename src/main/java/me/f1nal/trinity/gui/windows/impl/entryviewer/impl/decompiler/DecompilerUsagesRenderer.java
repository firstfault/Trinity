package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiMouseCursor;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.xref.AbstractXref;
import me.f1nal.trinity.execution.xref.MemberXref;
import me.f1nal.trinity.gui.windows.impl.xref.XrefViewerFrame;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilderMemberRef;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.ArrayList;
import java.util.List;

public class DecompilerUsagesRenderer implements Runnable {
    private final Trinity trinity;
    private final Input input;
    private final int xrefsCount;
    private float addX = 0.F;

    public DecompilerUsagesRenderer(Trinity trinity, Input input) {
        this.trinity = trinity;
        this.input = input;
        if (input instanceof MethodInput) {
            addX += 10.F;
        }
        this.xrefsCount = input.createXrefBuilder(trinity.getExecution().getXrefMap()).createXrefs().size();
    }

    @Override
    public void run() {
        ImGui.text("");
        ImGui.sameLine(0.F, 0.F);
        float fontSize = Math.max(Main.getDisplayManager().getFontManager().getFontSize() - 1.F, 12.F);
        float globalScale = fontSize / 14;
        ImVec2 rectMin = ImGui.getItemRectMin().plus(addX * globalScale, 0.F);
        ImFont font = ImGui.getFont();
        String text = this.xrefsCount + " usage" + (this.xrefsCount == 1 ? "" : "s");
        ImVec2 textSize = font.calcTextSizeA(fontSize, Float.MAX_VALUE, -1.F, text);
        ImVec2 mousePos = ImGui.getMousePos();
        boolean hovered = ImGui.isWindowHovered() && mousePos.x >= rectMin.x && mousePos.y >= rectMin.y && mousePos.x <= rectMin.x + textSize.x && mousePos.y <= rectMin.y + textSize.y;
        ImGui.getWindowDrawList().addText(font, fontSize, rectMin.x, rectMin.y - globalScale, hovered ? CodeColorScheme.TEXT : CodeColorScheme.DISABLED, text);
        if (hovered) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand);

            if (ImGui.isMouseClicked(0)) {
                Main.getWindowManager().addClosableWindow(new XrefViewerFrame(this.input.createXrefBuilder(this.trinity.getExecution().getXrefMap()), trinity));
            }
        }
        ImGui.setCursorPosY(ImGui.getCursorPosY() + (14.F * globalScale));
    }
}
