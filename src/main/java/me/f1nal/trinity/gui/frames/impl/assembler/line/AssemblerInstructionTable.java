package me.f1nal.trinity.gui.frames.impl.assembler.line;

import imgui.*;
import imgui.flag.ImGuiCol;
import me.f1nal.trinity.gui.frames.impl.assembler.AssemblerFrame;
import me.f1nal.trinity.gui.frames.impl.assembler.InstructionComponent;
import me.f1nal.trinity.gui.frames.impl.assembler.InstructionList;

import java.util.ArrayList;
import java.util.List;

public class AssemblerInstructionTable {
    private final AssemblerFrame assemblerFrame;
    private final InstructionList instructions;
    private final ImDrawList drawList = ImGui.getWindowDrawList();
    private final Instruction2SourceMapping sourceMapping;
    public float sourceFileStartX;
    private SourceLineNumber hoveredSourceLine;
    private InstructionComponent hoveredInstruction;
    private InstructionComponent draggingInstruction;
    private InstructionReferenceArrow hoveredReferenceArrow;
    public float instructionStartX;
    /**
     * If non-null, then every instruction that isn't in this list gets a lower opacity.
     */
    private List<InstructionComponent> highlightedInstructions;
    public boolean draggingArrowNow;

    public AssemblerInstructionTable(AssemblerFrame assemblerFrame, InstructionList instructions, Instruction2SourceMapping sourceMapping) {
        this.assemblerFrame = assemblerFrame;
        this.instructions = instructions;
        this.sourceMapping = sourceMapping;
    }

    public void setDraggingInstruction(InstructionComponent draggingInstruction) {
        assemblerFrame.draggingInstruction = new InstructionDrag(draggingInstruction, ImGui.getMousePos(), instructions.indexOf(draggingInstruction));
    }

    public InstructionDrag getDraggingInstruction() {
        return assemblerFrame.draggingInstruction;
    }

    public boolean isDraggingInstruction(InstructionComponent component) {
        InstructionDrag instructionDrag = assemblerFrame.draggingInstruction;
        return instructionDrag != null && instructionDrag.getComponent() == component;
    }

    public InstructionReferenceArrow getDraggingReferenceArrow() {
        return assemblerFrame.draggingReferenceArrow;
    }

    public void setDraggingReferenceArrow(InstructionReferenceArrow draggingReferenceArrow) {
        assemblerFrame.draggingReferenceArrow = draggingReferenceArrow;
    }

    public void setHighlightedInstructions(List<InstructionComponent> highlightedInstructions) {
        this.highlightedInstructions = highlightedInstructions;
    }

    public List<InstructionComponent> getHighlightedInstructions() {
        return highlightedInstructions;
    }

    public InstructionReferenceArrow getHoveredReferenceArrow() {
        return hoveredReferenceArrow;
    }

    public void setHoveredReferenceArrow(InstructionReferenceArrow hoveredReferenceArrow) {
        this.hoveredReferenceArrow = hoveredReferenceArrow;
    }

    public InstructionComponent getHoveredInstruction() {
        return hoveredInstruction;
    }

    public void setHoveredInstruction(InstructionComponent hoveredInstruction) {
        this.hoveredInstruction = hoveredInstruction;
    }

    public SourceLineNumber getHoveredSourceLine() {
        return hoveredSourceLine;
    }

    public void setHoveredSourceLine(SourceLineNumber hoveredSourceLine) {
        this.hoveredSourceLine = hoveredSourceLine;
    }

    public AssemblerFrame getAssemblerFrame() {
        return assemblerFrame;
    }

    public Instruction2SourceMapping getSourceMapping() {
        return sourceMapping;
    }

    public InstructionList getInstructions() {
        return instructions;
    }

    public float draw(ImVec2 vMin, ImVec2 vMax) {
        this.instructionStartX = 58 + (instructions.getMaximumReferenceArrowDepth() * 4.F);
        this.sourceFileStartX = 58.F;

        float x = vMin.x, y = vMin.y + 1.F;

        ArrayList<InstructionComponent> sorted = new ArrayList<>(this.instructions);
        sorted.removeIf(i -> i.getInstruction().getOpcode() == -1);

        for (InstructionComponent instruction : sorted) {
            instruction.setBounds(this, x, y);
            ImVec4 bounds = instruction.getBounds();
            y += bounds.w;
        }

        for (InstructionReferenceArrow arrow : instructions.getInstructionReferenceArrowList()) {
            arrow.setControls(this);
        }

        for (InstructionComponent instruction : sorted) {
            instruction.setControls(this);
        }

        if (this.getDraggingReferenceArrow() != null) this.getDraggingReferenceArrow().setDraggingControls(this);

        for (InstructionComponent instruction : sorted) {
            instruction.draw(drawList, this);
        }

        for (InstructionReferenceArrow arrow : instructions.getInstructionReferenceArrowList()) {
            arrow.draw(drawList, this);
        }

        return Math.max(y - vMin.y, ImGui.getContentRegionAvailY());
    }

    public boolean isInstructionHighlighted(InstructionComponent component) {
        return this.highlightedInstructions == null || this.highlightedInstructions.contains(component);
    }

    public int getWindowBackgroundColor() {
        return ImColor.rgb(ImGui.getStyle().getColor(ImGuiCol.WindowBg));
    }

    public boolean isWindowHovered() {
        return ImGui.isWindowHovered();
    }
}
