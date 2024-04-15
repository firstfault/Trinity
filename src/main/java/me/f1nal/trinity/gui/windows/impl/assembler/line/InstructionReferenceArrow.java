package me.f1nal.trinity.gui.windows.impl.assembler.line;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.ImVec4;
import imgui.flag.ImGuiKey;
import me.f1nal.trinity.execution.labels.MethodLabel;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionComponent;
import me.f1nal.trinity.theme.CodeColorScheme;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Graphical component for a jump inside a routine.
 */
public class InstructionReferenceArrow {
    private final InstructionComponent from;
    private InstructionComponent to;
    private final int color;
    private final MethodLabel label;
    private final ImVec4[] points = new ImVec4[3];
    private final BiConsumer<AbstractInsnNode, LabelNode> updateLabel;
    private int depth;

    public InstructionReferenceArrow(InstructionComponent from, InstructionComponent to, int color, MethodLabel label, BiConsumer<AbstractInsnNode, LabelNode> updateLabel) {
        this.from = from;
        this.to = to;
        this.color = color;
        this.label = label;
        this.updateLabel = updateLabel;
    }

    public void setTo(InstructionComponent to) {
        this.to = to;
    }

    public MethodLabel getLabel() {
        return label;
    }

    public int getColor() {
        return color;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    public void setDraggingControls(AssemblerInstructionTable table) {
        final boolean escape = ImGui.isKeyDown(ImGui.getKeyIndex(ImGuiKey.Escape));
        boolean clearDrag = false;

        if (table.draggingArrowNow) System.out.println("SWTF");
        if (!table.draggingArrowNow && (ImGui.isMouseClicked(0) || escape)) {
            final InstructionComponent target = escape ? null : table.getHoveredInstruction();

            if (target != null && target != this.from) {
                this.setTo(target);
            }

            clearDrag = true;
        }

        this.setControls(table);

        if (clearDrag) {
            table.setHoveredReferenceArrow(null);
            table.setDraggingReferenceArrow(null);
        }
    }

    public void setControls(AssemblerInstructionTable table) {
        boolean dragging = table.getDraggingReferenceArrow() == this;

        InstructionComponent fromInsn = this.from;
        InstructionComponent toInsn = (dragging ? Objects.requireNonNullElse(table.getHoveredInstruction() == this.from ? null : table.getHoveredInstruction(), this.to) : this.to);

        if (dragging) {
            table.setHoveredReferenceArrow(this);
        }

        ImVec4 from = fromInsn.getBounds();
        ImVec4 to = toInsn.getBounds();

        float lineWidth = this.getLineWidth();
        float dx = this.getDepthInPixels();
        float lx = from.x + table.instructionStartX - lineWidth - dx;
        float fp = (from.y+to.w/2.F);
        float tp = to.y + (to.w/2.3F);

        points[0] = new ImVec4(lx, fp, lx, tp);
        points[1] = new ImVec4(lx, fp, lx + lineWidth+dx, fp);
        points[2] = new ImVec4(lx, tp, lx + lineWidth+dx, tp);

        if (table.getHoveredReferenceArrow() == null && table.isWindowHovered()) {
            ImVec2 mousePos = ImGui.getMousePos();

            final float minCollision = 5.F;

            for (ImVec4 point : points) {
                float minX = point.x;
                float minY = point.y;
                float maxX = point.z;
                float maxY = point.w;

                if (minY > maxY) {
                    minY = maxY;
                    maxY = point.y;
                }

                if (mousePos.x >= minX - minCollision && mousePos.x <= maxX + minCollision &&
                        mousePos.y >= minY - minCollision && mousePos.y <= maxY + minCollision) {
                    table.setHoveredReferenceArrow(this);
                    break;
                }
            }
        }

        if (table.getHoveredReferenceArrow() == this) {
            table.setHighlightedInstructions(List.of(toInsn, fromInsn));

            if (table.getDraggingReferenceArrow() != this && ImGui.isMouseClicked(0)) {
                table.setDraggingReferenceArrow(this);
                table.draggingArrowNow = true;
            }
        }
    }

    public void draw(ImDrawList drawList, AssemblerInstructionTable table) {
        int color = table.getHoveredReferenceArrow() == this ? CodeColorScheme.DISABLED : getColor();

        if (!table.isInstructionHighlighted(this.to) && !table.isInstructionHighlighted(this.from)) {
            // todo
        }

        for (ImVec4 point : points) {
            drawList.addLine(point.x, point.y, point.z, point.w, color);
        }

        float lineWidth = this.getLineWidth();
        float lx = points[0].x + this.getDepthInPixels(), tp = points[2].y;

        float arrowWidth = 4.F;
        float arrowOpen = 4.F;

        drawList.addLine(lx + lineWidth - arrowWidth, tp - arrowOpen, lx + lineWidth, tp, color, 1.F);
        drawList.addLine(lx + lineWidth - arrowWidth, tp + arrowOpen, lx + lineWidth, tp, color, 1.F);
    }

    private float getLineWidth() {
        return 12.F;
    }

    private float getDepthInPixels() {
        return this.getDepth() * 4.F;
    }

    public InstructionComponent getFrom() {
        return from;
    }

    public InstructionComponent getTo() {
        return to;
    }

    public void updateLabel(AbstractInsnNode newInstruction, LabelNode newLabel) {
        this.updateLabel.accept(newInstruction, newLabel);
    }
}
