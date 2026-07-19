package me.f1nal.trinity.gui.windows.impl.assembler.line;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.ImVec4;
import imgui.flag.ImGuiKey;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
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
        final boolean escape = ImGui.isKeyDown(ImGuiKey.Escape);
        boolean clearDrag = false;

        if (!table.draggingArrowNow && (ImGui.isMouseClicked(0) || escape)) {
            final InstructionComponent target = escape ? null : table.getHoveredInstruction();

            if (target != null && target != this.from) {
                table.getAssemblerFrame().retargetReference(this, target);
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

    public void drawOffscreenEndpoints(ImDrawList drawList, AssemblerInstructionTable table) {
        drawOffscreenEndpoint(drawList, table, this.from, "start");
        drawOffscreenEndpoint(drawList, table, this.to, "end");
    }

    private void drawOffscreenEndpoint(ImDrawList drawList, AssemblerInstructionTable table,
                                       InstructionComponent endpoint, String endpointName) {
        ImVec4 bounds = endpoint.getBounds();
        boolean above = bounds.y + bounds.w <= table.getViewportMinY();
        boolean below = bounds.y >= table.getViewportMaxY();
        if (!above && !below) return;

        float x = table.getViewportMinX();
        float y = above ? table.getViewportMinY() : table.getViewportMaxY() - bounds.w;
        float maxX = table.getViewportMaxX();
        int borderColor = CodeColorScheme.DISABLED;

        drawList.addRectFilled(x, y, maxX, y + bounds.w,
                CodeColorScheme.setAlpha(table.getWindowBackgroundColor(), 245));

        float textX = x + 5.F;
        String prefix = endpointName + "  +" + endpoint.getId() + "  ";
        drawList.addText(textX, y, borderColor, prefix);
        textX += ImGui.calcTextSize(prefix).x;

        List<ColoredString> text = endpoint.asText();
        for (int i = 0; i < text.size(); i++) {
            ColoredString part = text.get(i);
            drawList.addText(textX, y, i == 0 ? endpoint.getColor() : part.getColor(), part.getText());
            textX += ImGui.calcTextSize(part.getText()).x;
        }
    }

    public boolean isVisible() {
        if (points[0] == null) return false;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (ImVec4 point : points) {
            minX = Math.min(minX, Math.min(point.x, point.z));
            minY = Math.min(minY, Math.min(point.y, point.w));
            maxX = Math.max(maxX, Math.max(point.x, point.z));
            maxY = Math.max(maxY, Math.max(point.y, point.w));
        }
        return ImGui.isRectVisible(minX - 5.F, minY - 5.F, maxX + 5.F, maxY + 5.F);
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
