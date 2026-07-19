package me.f1nal.trinity.gui.windows.impl.assembler.line;

import imgui.*;
import imgui.flag.ImGuiCol;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerFrame;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionComponent;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionList;
import me.f1nal.trinity.gui.windows.impl.assembler.args.InstructionOperand;
import org.objectweb.asm.tree.LabelNode;

import java.util.ArrayList;
import java.util.List;

public class AssemblerInstructionTable {
    private final AssemblerFrame assemblerFrame;
    private final InstructionList instructions;
    private ImDrawList drawList;
    private final Instruction2SourceMapping sourceMapping;
    public float sourceFileStartX;
    private SourceLineNumber hoveredSourceLine;
    private InstructionComponent hoveredInstruction;
    private InstructionOperand hoveredOperand;
    private InstructionReferenceArrow hoveredReferenceArrow;
    public float instructionStartX;
    public float instructionOperandsStartX;
    private float viewportMinX;
    private float viewportMinY;
    private float viewportMaxX;
    private float viewportMaxY;
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

    public void setHoveredOperand(InstructionOperand hoveredOperand) {
        this.hoveredOperand = hoveredOperand;
    }

    public InstructionOperand getHoveredOperand() {
        return hoveredOperand;
    }

    public void setDraggingInstruction(InstructionComponent draggingInstruction) {
        assemblerFrame.beginDragMutation();
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
        this.drawList = ImGui.getWindowDrawList();
        this.hoveredSourceLine = null;
        this.hoveredInstruction = null;
        this.hoveredOperand = null;
        this.hoveredReferenceArrow = null;
        this.highlightedInstructions = null;
        this.draggingArrowNow = false;
        float fontSize = Main.getPreferences().getDefaultFont().getSize();
        float spacing = fontSize * 0.5F;

        this.instructionStartX = 58 + (instructions.getMaximumReferenceArrowDepth() * 4.F) + spacing;
        this.instructionOperandsStartX = this.instructionStartX + fontSize * 7;
        this.sourceFileStartX = 58.F + fontSize * 2.F;

        float x = vMin.x, y = vMin.y + 1.F;
        this.viewportMinX = vMin.x + ImGui.getScrollX();
        this.viewportMinY = vMin.y + ImGui.getScrollY();
        this.viewportMaxX = vMax.x + ImGui.getScrollX();
        this.viewportMaxY = vMax.y + ImGui.getScrollY();

        boolean hideMetadata = Main.getPreferences().isAssemblerHideMetadata();
        List<InstructionComponent> visible = new ArrayList<>();
        for (InstructionComponent component : instructions) {
            if (component.getInstruction() instanceof LabelNode) continue;
            if (!hideMetadata || component.getInstruction().getOpcode() >= 0) visible.add(component);
        }

        y = vMin.y + 1.F;
        for (InstructionComponent instruction : visible) {
            instruction.setBounds(this, x, y);
            ImVec4 bounds = instruction.getBounds();
            y += bounds.w;
        }

        for (InstructionReferenceArrow arrow : instructions.getInstructionReferenceArrowList()) {
            arrow.setControls(this);
        }

        for (InstructionComponent instruction : visible) {
            ImVec4 bounds = instruction.getBounds();
            if (ImGui.isRectVisible(bounds.x, bounds.y, bounds.x + 0x10000, bounds.y + bounds.w)) {
                instruction.setControls(this);
            }
        }

        if (this.getDraggingReferenceArrow() != null) this.getDraggingReferenceArrow().setDraggingControls(this);

        for (InstructionComponent instruction : visible) {
            ImVec4 bounds = instruction.getBounds();
            if (ImGui.isRectVisible(bounds.x, bounds.y, bounds.x + 0x10000, bounds.y + bounds.w)) {
                instruction.draw(drawList, this);
            }
        }

        for (InstructionReferenceArrow arrow : instructions.getInstructionReferenceArrowList()) {
            if (arrow.isVisible()) arrow.draw(drawList, this);
        }
        if (this.hoveredReferenceArrow != null) {
            this.hoveredReferenceArrow.drawOffscreenEndpoints(drawList, this);
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

    public float getViewportMinX() {
        return viewportMinX;
    }

    public float getViewportMinY() {
        return viewportMinY;
    }

    public float getViewportMaxX() {
        return viewportMaxX;
    }

    public float getViewportMaxY() {
        return viewportMaxY;
    }

}
