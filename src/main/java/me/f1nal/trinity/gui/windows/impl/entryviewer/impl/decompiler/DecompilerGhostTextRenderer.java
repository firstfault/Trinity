package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiMouseCursor;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.FontEnum;
import me.f1nal.trinity.decompiler.util.TextBuffer;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.hierarchy.MethodHierarchy;
import me.f1nal.trinity.gui.components.FontSettings;
import me.f1nal.trinity.gui.windows.impl.xref.XrefViewerFrame;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.ArrayList;
import java.util.List;

public class DecompilerGhostTextRenderer implements Runnable {
    private final List<String> text = new ArrayList<>(2);
    private final Trinity trinity;
    private final Input<?> input;
    private final String indent;

    public DecompilerGhostTextRenderer(Trinity trinity, Input<?> input) {
        this.trinity = trinity;
        this.input = input;

        final int xrefsCount = input.createXrefBuilder(trinity.getExecution().getXrefMap()).createXrefs().size();
        this.text.add(xrefsCount == 0 ? "no usages" : (xrefsCount + " usage" + (xrefsCount == 1 ? "" : "s")));

        if (input instanceof MethodInput) {
            this.indent = new TextBuffer().appendIndent(1).toString();
            if (false) {
                MethodHierarchy methodHierarchy = ((MethodInput) input).getMethodHierarchy();
                if (methodHierarchy != null) {
                    List<MethodInput> linkedMethods = methodHierarchy.getLinkedMethods();
                    if (linkedMethods.size() > 1) {
                        this.text.add(linkedMethods.size() + " linked methods");
                    }
                }
            }
        } else {
            this.indent = "";
        }
    }

    @Override
    public void run() {
        float cursorPosX = ImGui.getCursorPosX();

        for (int i = 0, size = text.size(); i < size; i++) {
            String line = text.get(i);

            ImGui.text(this.indent);
            ImGui.sameLine(0.F, 0.F);

            FontSettings defaultFont = Main.getPreferences().getDecompilerFont();
            float fontSize = Math.max(defaultFont.getSize() - 1.F, 12.F);
            float globalScale = fontSize / 14;
            ImVec2 rectMin = new ImVec2(ImGui.getItemRectMaxX(), ImGui.getItemRectMinY());
            ImFont font = defaultFont.getImFont();
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

        ImGui.setCursorPosX(cursorPosX);
    }
}
