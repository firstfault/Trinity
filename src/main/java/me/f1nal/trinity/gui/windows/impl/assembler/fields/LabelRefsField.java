package me.f1nal.trinity.gui.windows.impl.assembler.fields;

import imgui.ImColor;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.execution.labels.MethodLabel;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerFrame;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionComponent;
import me.f1nal.trinity.gui.windows.impl.assembler.args.AbstractInsnArgument;

import java.util.ArrayList;
import java.util.List;

public class LabelRefsField extends InstructionField {
    private final AssemblerFrame assemblerFrame;
    private final MethodLabel label;

    public LabelRefsField(AssemblerFrame assemblerFrame, MethodLabel label) {
        this.assemblerFrame = assemblerFrame;
        this.label = label;
    }

    @Override
    public void draw() {
        List<InstructionComponent> refs = getReferencingInstructions();
        // EMpty text to clear sameline
        ImGui.text("");
        ImGui.text("Showing you " + refs.size() + " references to this label");
        for (int i = 0, refsSize = refs.size(); i < refsSize; i++) {
            InstructionComponent ref = refs.get(i);
            ImGui.pushStyleColor(ImGuiCol.HeaderHovered, ImColor.rgba(45, 45, 49, 100));
            ImGui.pushStyleColor(ImGuiCol.HeaderActive, ImColor.rgba(85, 85, 89, 100));
            if (ImGui.selectable("###LabelRef" + i)) {
                assemblerFrame.setInstructionView(ref);
            }
            ImGui.popStyleColor();
            ImGui.popStyleColor();
            ImGui.sameLine(0.F, 0.F);
            ColoredString.drawText(ref.asText());
        }
    }

    private List<InstructionComponent> getReferencingInstructions() {
        List<InstructionComponent> labelRefs = new ArrayList<>();

        componentLoop:
        for (InstructionComponent component : assemblerFrame.getInstructions()) {
            for (AbstractInsnArgument argument : component.getArguments()) {
                for (InstructionField field : argument.getFields()) {
                    if (field instanceof LabelRefsField) {
                        if (field == this) {
                            continue componentLoop;
                        }

                        if (((LabelRefsField) field).label == this.label) {
                            labelRefs.add(component);
                        }
                    }
                }
            }
        }

        return labelRefs;
    }
}
