package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiMouseCursor;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.hierarchy.MethodHierarchy;
import me.f1nal.trinity.gui.windows.impl.xref.XrefViewerFrame;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.ArrayList;
import java.util.List;

public class DecompilerGhostTextRenderer implements Runnable {
    private final List<String> text = new ArrayList<>(2);
    private final Trinity trinity;
    private final Input<?> input;
    private float addX = 0.F;

    public DecompilerGhostTextRenderer(Trinity trinity, Input<?> input) {
        this.trinity = trinity;
        this.input = input;

        final int xrefsCount = input.createXrefBuilder(trinity.getExecution().getXrefMap()).createXrefs().size();
        this.text.add(xrefsCount == 0 ? "no usages" : (xrefsCount + " usage" + (xrefsCount == 1 ? "" : "s")));

        if (input instanceof MethodInput) {
            addX += 10.F;

            if (false) {
                MethodHierarchy methodHierarchy = ((MethodInput) input).getMethodHierarchy();
                if (methodHierarchy != null) {
                    List<MethodInput> linkedMethods = methodHierarchy.getLinkedMethods();
                    if (linkedMethods.size() > 1) {
                        this.text.add(linkedMethods.size() + " linked methods");
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        for (int i = 0, size = text.size(); i < size; i++) {
            String line = text.get(i);
            ImGui.text("");
            ImGui.sameLine(0.F, 0.F);

            float fontSize = Math.max(Main.getDisplayManager().getFontManager().getFontSize() - 1.F, 12.F);
            float globalScale = fontSize / 14;
            ImVec2 rectMin = ImGui.getItemRectMin().plus(addX * globalScale, 0.F);
            ImFont font = ImGui.getFont();
            ImVec2 textSize = font.calcTextSizeA(fontSize, Float.MAX_VALUE, -1.F, line);
            ImVec2 mousePos = ImGui.getMousePos();
            boolean hovered = ImGui.isWindowHovered() && mousePos.x >= rectMin.x && mousePos.y >= rectMin.y && mousePos.x <= rectMin.x + textSize.x && mousePos.y <= rectMin.y + textSize.y;
            ImGui.getWindowDrawList().addText(font, fontSize, rectMin.x, rectMin.y - globalScale, hovered ? CodeColorScheme.TEXT : CodeColorScheme.DISABLED, line);
            if (hovered) {
                ImGui.setMouseCursor(ImGuiMouseCursor.Hand);

                if (ImGui.isMouseClicked(0)) {
                    Main.getWindowManager().addClosableWindow(new XrefViewerFrame(this.input.createXrefBuilder(this.trinity.getExecution().getXrefMap()), trinity));
                }
            }
            ImGui.setCursorPosY(ImGui.getCursorPosY() + ((14.F + (i == size - 1 ? 0.F : 1.F)) * globalScale));
        }
    }
}
