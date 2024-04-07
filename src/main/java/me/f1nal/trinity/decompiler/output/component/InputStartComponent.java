package me.f1nal.trinity.decompiler.output.component;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.xref.MemberXref;
import me.f1nal.trinity.gui.frames.impl.xref.XrefViewerFrame;
import me.f1nal.trinity.gui.frames.impl.xref.builder.XrefBuilderMemberRef;
import me.f1nal.trinity.theme.CodeColorScheme;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiMouseCursor;

import java.util.List;

public class InputStartComponent extends RawTextComponent {
    private final Input input;
    private final List<MemberXref> xrefs;
    private MemberDetails details;
    private float addX = 0.F;

    public InputStartComponent(Input input) {
        super("");
        this.input = input;
        if (input instanceof MethodInput) {
            addX += 10.F;
            details = ((MethodInput) input).getDetails();
        } else if (input instanceof FieldInput) {
            details = ((FieldInput) input).getDetails();
        } else {
            this.xrefs = null;
            return;
        }
        this.xrefs = Main.getTrinity().getExecution().getXrefMap().getReferences(details);
    }

    @Override
    public void handleAfterDrawing() {
        ImGui.text("");
        ImGui.sameLine(0.F, 0.F);
        float globalScale = Main.getPreferences().getGlobalScale();
        ImVec2 rectMin = ImGui.getItemRectMin().plus(addX * globalScale, 0.F);
        ImFont font = ImGui.getFont();
        String text = this.xrefs.size() + " usage" + (this.xrefs.size() == 1 ? "" : "s");
        float fontSize = 13.F * globalScale;
        ImVec2 textSize = font.calcTextSizeA(fontSize, Float.MAX_VALUE, -1.F, text);
        ImVec2 mousePos = ImGui.getMousePos();
        boolean hovered = mousePos.x >= rectMin.x && mousePos.y >= rectMin.y && mousePos.x <= rectMin.x + textSize.x && mousePos.y <= rectMin.y + textSize.y;
        ImGui.getWindowDrawList().addText(font, fontSize, rectMin.x, rectMin.y - globalScale, hovered ? CodeColorScheme.TEXT : CodeColorScheme.DISABLED, text);
        if (hovered) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand);

            if (ImGui.isMouseClicked(0)) {
                Main.getDisplayManager().addClosableWindow(new XrefViewerFrame(new XrefBuilderMemberRef(Main.getTrinity().getExecution().getXrefMap(), this.details), Main.getTrinity()));
            }
        }
        ImGui.setCursorPosY (ImGui.getCursorPosY() + (14.F * globalScale));
    }

    public Input getInput() {
        return input;
    }
}
