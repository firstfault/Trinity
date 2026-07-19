package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiMouseCursor;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.main.extern.IFernflowerPreferences;
import me.f1nal.trinity.decompiler.output.FontEnum;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.hierarchy.MethodHierarchy;
import me.f1nal.trinity.gui.components.FontSettings;
import me.f1nal.trinity.gui.windows.impl.xref.XrefViewerFrame;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DecompilerGhostTextRenderer implements Runnable {
    private static boolean interactionBlocked;
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
            this.indent = IFernflowerPreferences.DEFAULTS.get(IFernflowerPreferences.INDENT_STRING).toString();
            if (false) {
                MethodHierarchy methodHierarchy = ((MethodInput) input).getMethodHierarchy();
                if (methodHierarchy != null) {
                    Set<MethodInput> linkedMethods = methodHierarchy.getLinkedMethods();
                    if (linkedMethods.size() > 1) {
                        this.text.add(linkedMethods.size() + " linked methods");
                    }
                }
            }
        } else {
            this.indent = "";
        }
    }

    static void setInteractionBlocked(boolean blocked) {
        interactionBlocked = blocked;
    }

    @Override
    public void run() {
        float cursorPosX = ImGui.getCursorPosX();

        for (int i = 0, size = text.size(); i < size; i++) {
            String line = text.get(i);
            FontSettings defaultFont = Main.getPreferences().getDecompilerFont();
            float fontSize = Math.max(defaultFont.getSize() - 1.F, 12.F);
            float globalScale = fontSize / 14;
            ImFont font = defaultFont.getImFont();
            float lineCursorPosY = ImGui.getCursorPosY();
            float indentWidth = font.calcTextSizeA(defaultFont.getSize(), Float.MAX_VALUE, -1.F, this.indent).x;
            ImGui.setCursorPosX(cursorPosX + indentWidth);
            ImVec2 rectMin = ImGui.getCursorScreenPos();
            ImVec2 textSize = font.calcTextSizeA(fontSize, Float.MAX_VALUE, -1.F, line);
            ImGui.dummy(textSize.x, textSize.y);
            boolean hovered = !interactionBlocked && ImGui.isItemHovered();
            ImGui.getWindowDrawList().addText(font, Math.round(fontSize), rectMin.x, rectMin.y - globalScale, hovered ? CodeColorScheme.TEXT : CodeColorScheme.DISABLED, line);
            if (hovered) {
                ImGui.setMouseCursor(ImGuiMouseCursor.Hand);

                if (ImGui.isMouseClicked(0)) {
                    Main.getWindowManager().addClosableWindow(new XrefViewerFrame(this.input.createXrefBuilder(this.trinity.getExecution().getXrefMap()), trinity));
                }
            }
            ImGui.setCursorPosX(cursorPosX);
            ImGui.setCursorPosY(lineCursorPosY + textSize.y + (i == size - 1 ? 0.F : globalScale));
        }
    }
}
