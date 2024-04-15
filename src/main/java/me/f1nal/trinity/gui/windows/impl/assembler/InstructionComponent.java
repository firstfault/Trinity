package me.f1nal.trinity.gui.windows.impl.assembler;

import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec4;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.execution.labels.MethodLabel;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.impl.assembler.action.*;
import me.f1nal.trinity.gui.windows.impl.assembler.args.AbstractInsnArgument;
import me.f1nal.trinity.gui.windows.impl.assembler.args.LabelArgument;
import me.f1nal.trinity.gui.windows.impl.assembler.line.AssemblerInstructionTable;
import me.f1nal.trinity.gui.windows.impl.assembler.line.InstructionDrag;
import me.f1nal.trinity.gui.windows.impl.assembler.line.InstructionReferenceArrow;
import me.f1nal.trinity.gui.windows.impl.assembler.line.SourceLineNumber;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.animation.Animation;
import org.lwjgl.glfw.GLFW;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstructionComponent {
    private static final List<InstructionAction> INSTRUCTION_ACTIONS = List.of(
            new DeleteInstructionAction(),
            new DuplicateInstructionAction(),
            new EditInstructionAction(),
            new InsertInstructionAction()
    );


    private final String name;
    private final List<AbstractInsnArgument> arguments = new ArrayList<>();
    private final AbstractInsnNode instruction;
    private int id;
    private Animation selectableAnimation;
    private ImVec4 bounds;
    private int color;

    public InstructionComponent(String name, AbstractInsnNode instruction) {
        this.name = name;
        this.instruction = instruction;
        this.color = getColor(instruction);
    }

    public void setBounds(AssemblerInstructionTable table, float x, float y) {
        float height = 3.F + Main.getDisplayManager().getFontManager().getFontSize();
        this.bounds = new ImVec4(x, y, 0x10000, height);
    }

    public ImVec4 getBounds() {
        return bounds;
    }

    public void setControls(AssemblerInstructionTable table) {
        if (table.isWindowHovered() && (table.getDraggingReferenceArrow() != null || table.getHoveredReferenceArrow() == null)) {
            float x = this.bounds.x;
            float y = this.bounds.y;

            if (ImGui.isMouseHoveringRect(x, y, x + 0x10000, y + this.bounds.w)) {
                table.setHoveredInstruction(this);
            }

            if (table.getHoveredInstruction() == this && ImGui.isMouseHoveringRect(bounds.x + table.sourceFileStartX, bounds.y, bounds.x + table.instructionStartX - 40, bounds.y + bounds.w)) {
                table.setHoveredSourceLine(table.getSourceMapping().getSourceComponent(this.getInstruction()));
            } else if (table.getHoveredInstruction() == this && ImGui.isMouseClicked(0)) {
                table.setDraggingInstruction(this);
            }

            if (table.isDraggingInstruction(this)) {
                table.setHighlightedInstructions(Collections.singletonList(this));
            }
        }
    }

    public void draw(ImDrawList drawList, AssemblerInstructionTable table) {
        float x = this.bounds.x, y = this.bounds.y;
        if (table.getHoveredInstruction() == this) {
            if (ImGui.isMouseClicked(1)) {
                table.getAssemblerFrame().getPopupMenu().show(this.createPopup(table.getAssemblerFrame()));
            }
            drawList.addRectFilled(x, y, x + 0x10000, y + this.bounds.w - 1,  ImColor.rgba(70, 70, 70, 33));

            for (InstructionAction instructionAction : INSTRUCTION_ACTIONS) {
                if(ImGui.isKeyPressed(instructionAction.getKey())) {
                    instructionAction.execute(table.getAssemblerFrame(), this);
                }
            }
        }

        if (table.getAssemblerFrame().isDraggingInstruction(this)) {
            InstructionDrag dragging = table.getDraggingInstruction();
            float deltaY = dragging.getMousePos().y - ImGui.getMousePosY();
            float height = this.bounds.w - 0.5F;
            int dragDelta = (int) (Math.abs(deltaY) / height);
            table.getAssemblerFrame().moveInstructionTo(this, dragging.getIndex() - (deltaY > 0.F ? dragDelta : -dragDelta));
        }

        drawList.addText(x + 5, y, CodeColorScheme.DISABLED, "+" + this.id);
        drawList.addText(x + table.instructionStartX + 5.F, y, this.color, this.getName());

        if (this.instruction instanceof MethodInsnNode) {
            float lx = x + table.instructionStartX - 5.F, tp = y + (this.bounds.w / 2.2F);
            float arrowWidth = 4F;
            float arrowOpen = 4.F;

            drawList.addTriangleFilled(lx + arrowWidth, tp - arrowOpen, lx, tp, lx + arrowWidth, tp + arrowOpen, getColor());
        }

        SourceLineNumber sourceComponent = table.getSourceMapping().getSourceComponent(this.getInstruction());
        if (sourceComponent != null) this.drawSourceLine(drawList, table, sourceComponent, x, y);

        float lineY = y + this.bounds.w - 1;
        drawList.addLine(0, lineY, 0x10004, lineY, ImColor.rgba(45, 45, 49, 130));

        this.drawArguments(table);

        if (!table.isInstructionHighlighted(this)) {
            drawList.addRectFilled(x, y, x + 0x10000, y + this.bounds.w - 1, CodeColorScheme.setAlpha(table.getWindowBackgroundColor(), 155));
        }
    }

    private void drawArguments(AssemblerInstructionTable table) {
        float y = this.bounds.y;
        float x = this.bounds.x + table.instructionOperandsStartX;

        List<AbstractInsnArgument> arguments = new ArrayList<>(this.getArguments());
        List<MethodLabel> addedLabels = new ArrayList<>();

        for (InstructionReferenceArrow arrow : table.getInstructions().getInstructionReferenceArrowList()) {
            if (arrow.getTo() == this) {
                if (addedLabels.contains(arrow.getLabel())) {
                    continue;
                }
                addedLabels.add(arrow.getLabel());
                arguments.add(new LabelArgument(table.getAssemblerFrame(), arrow.getLabel().getTable().getMethodInput(), new LabelNode(arrow.getLabel().findOriginal()), null));
            }
        }

        for (AbstractInsnArgument argument : arguments) {
            for (ColoredString string : argument.getDetailsText()) {
                ImGui.getWindowDrawList().addText(x, y, string.getColor(), string.getText());
                x += ImGui.calcTextSize(string.getText()).x;
            }

            x += 3.F;
        }
    }

    private PopupItemBuilder createPopup(AssemblerFrame af) {
        PopupItemBuilder popup = PopupItemBuilder.create();

        if (getId() != 0) popup.menuItem("Move Up", () -> af.moveInstruction(this, -1));
        if (getId() != af.getInstructions().size() - 1) popup.menuItem("Move Down", () -> af.moveInstruction(this, 1));

        popup.separator();

        for (InstructionAction instructionAction : INSTRUCTION_ACTIONS) {
            popup.menuItem(instructionAction.getName(), GLFW.glfwGetKeyName(instructionAction.getKey(), 0), false, () -> instructionAction.execute(af, this));
        }

        popup.separator();

        popup.menuItem("Copy Text", () -> af.copyTextInstruction(this));
        return popup;
    }

    private void drawSourceLine(ImDrawList drawList, AssemblerInstructionTable table, SourceLineNumber sourceComponent, float x, float y) {
        float rectY = y + 7.5F;
        float rect = 6.F;
        float startClass = table.sourceFileStartX;

        drawList.addRectFilled(x + startClass - rect, rectY - rect, x + startClass + rect, rectY + rect, CodeColorScheme.CLASS_REF, 1.F);
        drawList.addText(x + startClass + 12.F, y, table.getHoveredSourceLine() != sourceComponent ? CodeColorScheme.DISABLED : CodeColorScheme.TEXT, table.getSourceMapping().getClassWithLine(this.getInstruction()));
    }

    private static int getColor(AbstractInsnNode instruction) {
        if (instruction instanceof JumpInsnNode) {
            return CodeColorScheme.KEYWORD_JUMP;
        }
        if (instruction instanceof MethodInsnNode || instruction instanceof InvokeDynamicInsnNode) {
            return CodeColorScheme.KEYWORD_CALL;
        }
        return CodeColorScheme.KEYWORD_DATA;
    }

    public String getName() {
        return name;
    }

    public List<AbstractInsnArgument> getArguments() {
        return arguments;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public AbstractInsnNode getInstruction() {
        return instruction;
    }

    public InstructionComponent copy() {
        InstructionComponent component = new InstructionComponent(this.getName(), this.getInstruction().clone(null));
        for (AbstractInsnArgument argument : this.getArguments()) {
            component.getArguments().add(argument.copy());
        }
        return component;
    }

    public List<ColoredString> asText() {
        List<ColoredString> text = new ArrayList<>();
        text.add(new ColoredString(this.getName(), CodeColorScheme.KEYWORD));
        boolean space = true;
        for (AbstractInsnArgument argument : this.getArguments()) {
            if (space && !argument.getDetailsText().isEmpty()) {
                text.add(new ColoredString(" ", CodeColorScheme.KEYWORD));
                space = false;
            }
            text.addAll(argument.getDetailsText());
        }
        return text;
    }

    public void setSelectableAnimation(Animation selectableAnimation) {
        this.selectableAnimation = selectableAnimation;
    }

    public float getSelectableAnimation() {
        if (this.selectableAnimation == null) {
            return 0.F;
        }
        selectableAnimation.run(0.F);
        float value = selectableAnimation.getValue();
        if (value == 0.F) {
            this.selectableAnimation = null;
            return 0.F;
        }
        return value;
    }

    public int getColor() {
        return color;
    }
}
