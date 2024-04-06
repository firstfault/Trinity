package me.f1nal.trinity.gui.frames.impl.assembler;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.components.events.MouseClickType;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.popup.PopupMenu;
import me.f1nal.trinity.gui.components.popup.PopupMenuBar;
import me.f1nal.trinity.gui.frames.ClosableWindow;
import me.f1nal.trinity.gui.frames.impl.assembler.args.AbstractInsnArgument;
import me.f1nal.trinity.gui.frames.impl.assembler.drag.InstructionPosition;
import me.f1nal.trinity.gui.frames.impl.assembler.history.*;
import me.f1nal.trinity.gui.frames.impl.assembler.line.AssemblerInstructionTable;
import me.f1nal.trinity.gui.frames.impl.assembler.line.Instruction2SourceMapping;
import me.f1nal.trinity.gui.frames.impl.assembler.line.InstructionDrag;
import me.f1nal.trinity.gui.frames.impl.assembler.line.InstructionReferenceArrow;
import me.f1nal.trinity.gui.frames.impl.assembler.popup.AssemblerBytecodeGoToIndexPopup;
import me.f1nal.trinity.gui.frames.impl.assembler.popup.edit.AssemblerEditInstructionPopup;
import me.f1nal.trinity.gui.viewport.notifications.ICaption;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.util.ModifyPriority;
import me.f1nal.trinity.util.SystemUtil;
import me.f1nal.trinity.util.animation.Animation;
import me.f1nal.trinity.util.animation.Easing;
import me.f1nal.trinity.util.history.ChangeManager;
import org.lwjgl.glfw.GLFW;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

import java.util.*;

public final class AssemblerFrame extends ClosableWindow implements ICaption {
    /**
     * Method that we are inspecting.
     */
    private final MethodInput methodInput;
    private final Instruction2SourceMapping sourceMapping;
    private InstructionList instructions;
    /**
     * Instruction selected for editing.
     */
    private InstructionComponent selected;
    private ChangeManager<AssemblerHistory> history;
    public InstructionDrag draggingInstruction;
    private InstructionComponent scrollTo;
    private AssemblerInstructionDecoder decoder;
    /**
     * Dragging reference arrow.
     */
    public InstructionReferenceArrow draggingReferenceArrow;
    private PopupMenu popupMenu = new PopupMenu();
    private final PopupMenuBar popupMenuBar = new PopupMenuBar(this.createMenuBar());
    private AssemblerHistoryFrame historyFrame = null;
    private boolean methodNotFresh, saveMethod;

    public AssemblerFrame(Trinity trinity, MethodInput methodInput, Instruction2SourceMapping sourceMapping) {
        super("Assembler: " + methodInput.getName() + methodInput.getDescriptor(), 660, 500, trinity);
        this.methodInput = methodInput;
        this.sourceMapping = sourceMapping;
        this.setInstructions();
        this.windowFlags |= ImGuiWindowFlags.MenuBar;
    }

    public PopupMenu getPopupMenu() {
        return popupMenu;
    }

    /**
     * Rebuilds the instruction view.
     */
    private void setInstructions() {
        this.saveMethod = this.methodNotFresh = false;
        this.selected = null;
        this.history = new ChangeManager<>();
        this.decoder = new AssemblerInstructionDecoder(this, methodInput);
        this.instructions = this.decoder.buildInstructions(methodInput.getInstructions());
        this.instructions.setIdsIfReset();
    }

    @Override
    public void render() {
        this.windowFlags &= ~ImGuiWindowFlags.UnsavedDocument;
        if (this.methodNotFresh) this.windowFlags |= ImGuiWindowFlags.UnsavedDocument;

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.F, 0.F);
        super.render();
        ImGui.popStyleVar();
    }

    @Override
    protected void renderFrame() {
        if (historyFrame != null) historyFrame.render();

        if (ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.Z))) {
            this.undoHistory();
        }

        this.popupMenuBar.draw();

        ImVec2 vMin = ImGui.getWindowContentRegionMin();
        ImVec2 vMax = ImGui.getWindowContentRegionMax();
        vMin.x += ImGui.getWindowPos().x;
        vMin.y += ImGui.getWindowPos().y;
        vMax.x += ImGui.getWindowPos().x;
        vMax.y += ImGui.getWindowPos().y;

        AssemblerInstructionTable table = new AssemblerInstructionTable(this, this.getInstructions(), this.sourceMapping);
        final float height = table.draw(vMin, vMax);

        // Block ImGui input to the window content
        ImGui.beginDisabled();
        ImGui.invisibleButton(getId("InvisBtn"), ImGui.getContentRegionAvailX(), height);
        ImGui.endDisabled();

        if (this.methodNotFresh) {
            if (ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(GLFW.GLFW_KEY_S)) {
                this.saveMethod = true;
            }
        }

        if (this.draggingInstruction != null && !ImGui.isMouseDown(0)) {
            if (this.draggingInstruction.getIndex() != this.instructions.indexOf(this.draggingInstruction.getComponent())) {
                this.addHistory(new InstructionDragHistory(new InstructionPosition(this.instructions, this.draggingInstruction.getComponent(), this.draggingInstruction.getIndex())));
            }
            this.instructions.queueIdReset();
            this.draggingInstruction = null;
        }

        if (this.instructions.setIdsIfReset()) {
            this.methodNotFresh = true;
        }

        if (this.saveMethod) {
            this.saveMethod = this.methodNotFresh = false;
            Logging.info("Saved method");
            int count = this.issueSave();
            this.methodInput.getOwningClass().notifyModified(ModifyPriority.HIGH);
            Main.getDisplayManager().addNotification(new Notification(NotificationType.SUCCESS, this, ColoredStringBuilder.create()
                    .fmt("Saved {} instructions to {}", count, methodInput.getDisplayName()).get()));
        }

        this.getPopupMenu().draw();
    }

    private PopupItemBuilder createMenuBar() {
        return PopupItemBuilder.create().
                menu("File", file -> {
                    file.menuItem("Save", "Ctrl+S", this.methodNotFresh, () -> this.saveMethod = true);
                }).
                menu("View", view -> {
                    view.menuItem("Refresh", this::setInstructions)
                    ;
                }).
                menu("Bytecode", bytecode -> {
                    bytecode.menuItem("Insert First...", () -> this.openInsertDialog(0))
                            .menuItem("Insert Last...", () -> this.openInsertDialog(instructions.size()))
                            .separator()
                            .menuItem("Go to index", () -> Main.getDisplayManager().addPopup(new AssemblerBytecodeGoToIndexPopup(this, trinity)))
                            .separator()
                            .menuItem("Clear All", this::clearAll)
                    ;
                }).
                menu("History", history -> {
                    history.menuItem("View History...", this::openHistoryViewer)
                            .predicate(() -> !this.history.isEmpty(),
                                    p -> p.menuItem("Undo", "Ctrl+Z", this::undoHistory))
                    ;
                })
        ;
    }

    private void openHistoryViewer() {
        this.historyFrame = new AssemblerHistoryFrame(this);
        this.historyFrame.setVisible(true);
    }

    private void clearAll() {
        this.addHistory(new InstructionClearHistory(instructions));
        instructions.clear();
        instructions.queueIdReset();
    }

    /**
     * Saves method
     */
    private int issueSave() {
        InsnList insnList = methodInput.getInstructions();
        // Need to do this otherwise instruction's prev and next do not get set
        AbstractInsnNode[] copy = insnList.toArray();
        for (AbstractInsnNode insnNode : copy) {
            insnList.remove(insnNode);
        }

        int count = 0;
        // Map of newly created instructions
        Map<InstructionComponent, AbstractInsnNode> mappedInstructions = new HashMap<>();
        for (InstructionComponent instruction : instructions) {
            AbstractInsnNode insnNode = instruction.getInstruction();

            if (insnList.contains(insnNode)) {
                throw new RuntimeException("What the heck");
            }

            insnList.add(insnNode);
            mappedInstructions.put(instruction, insnNode);
            ++count;
        }
        // Link labels
        for (InstructionReferenceArrow arrow : instructions.getInstructionReferenceArrowList()) {
            Label label = Objects.requireNonNull(arrow.getLabel().findOriginal(), "Jump label");
            AbstractInsnNode targetInstruction = Objects.requireNonNull(mappedInstructions.get(arrow.getTo()), "Jump target");
            AbstractInsnNode invokerInstruction = Objects.requireNonNull(mappedInstructions.get(arrow.getFrom()), "Jump invoker");

            LabelNode labelNode = new LabelNode(label);
            insnList.insertBefore(targetInstruction, labelNode);
            arrow.updateLabel(invokerInstruction, labelNode);
        }
        return count;
    }

    private void renderHistory() {
        Stack<AssemblerHistory> stack = history.getStack();
        for (int i = stack.size() - 1; i >= 0; i--) {
            AssemblerHistory history = stack.get(i);
            ImGui.selectable("###HistoryId" + i);
            String popupId = "HistoryPopup" + i;
            if (ImGui.isItemHovered() && ImGui.isMouseClicked(1)) {
                ImGui.openPopup(popupId);
            }
            if (ImGui.beginPopup(popupId)) {
                boolean firstInstruction = i == stack.size() - 1;
                if (ImGui.menuItem("Undo", (firstInstruction ? "Ctrl+Z" : ""))) {
                    this.history.undo(history);
                }
//                if (ImGui.menuItem("Redo", (firstInstruction ? "Ctrl+Shift+Z" : ""))) {
//                    this.history.redo(history);
//                }
                ImGui.endPopup();
            }
            ImGui.sameLine();
            drawArgumentDetails(history.getText());
        }
    }

    public ChangeManager<AssemblerHistory> getHistory() {
        return history;
    }

    private void undoHistory() {
        if (history.isEmpty()) {
            return;
        }
        this.history.undo(history.getStack().peek());
    }

    @Override
    public String getTitle() {
        return super.getTitle() + ": " + instructions.size() + " instructions";
    }

    public void moveInstruction(InstructionComponent instruction, int delta) {
        final int index = instructions.indexOf(instruction), nextIndex = index + delta;
        if (this.moveInstructionTo(instruction, nextIndex)) {
            this.addHistory(new InstructionDragHistory(new InstructionPosition(this.instructions, instruction, index)));
        }
    }

    public boolean moveInstructionTo(InstructionComponent instruction, int position) {
        if (position >= 0 && position < this.instructions.size()) {
            this.instructions.remove(instruction);
            this.instructions.add(position, instruction);
            this.instructions.queueIdReset();
            return true;
        }
        return false;
    }

    public void openInsertDialog(int index) {
        Main.getDisplayManager().addPopup(new AssemblerEditInstructionPopup(trinity, methodInput, (result) -> {
            this.insertInstruction(index, result.getInsnNode());
        }));
    }

    private void insertInstruction(int index, AbstractInsnNode insnNode) {
        InstructionComponent component = decoder.translateInstruction(insnNode);
        instructions.add(index, component);
        instructions.queueIdReset();
        this.addHistory(new InstructionInsertHistory(new InstructionPosition(this.instructions, component, instructions.indexOf(component))));
    }

    private static String getInstructionRawText(InstructionComponent instruction) {
        boolean space = false;
        StringBuilder sb = new StringBuilder();
        sb.append(instruction.getName());
        for (AbstractInsnArgument argument : instruction.getArguments()) {
            for (ColoredString coloredString : argument.getDetailsText()) {
                if (!space) {
                    sb.append(' ');
                    space = true;
                }
                sb.append(coloredString.getText());
            }
        }
        return sb.toString();
    }

    public void setInstructionView(InstructionComponent component) {
        component.setSelectableAnimation(new Animation(Easing.LINEAR, 1000L, 1.F));
        this.selected = component;
        this.scrollTo = component;
    }

    private void drawArgumentDetails(List<ColoredString> text) {
        ColoredString.drawText(text);
    }

    public void setSelected(InstructionComponent selected) {
        this.selected = selected;
    }

    public InstructionList getInstructions() {
        return instructions;
    }

    public void deleteInstruction(InstructionComponent instruction) {
        if (this.selected == instruction) {
            this.selected = null;
        }
        this.addHistory(new InstructionDeleteHistory(new InstructionPosition(this.instructions, instruction, instructions.indexOf(instruction))));
        this.instructions.remove(instruction);
        this.instructions.queueIdReset();
    }

    private void addHistory(AssemblerHistory history) {
        history.getBrowserViewerNode().addMouseClickHandler((clickType) -> {
            if (clickType == MouseClickType.RIGHT_CLICK) {
                Main.getDisplayManager().showPopup(PopupItemBuilder.create().menuItem("Undo", () -> this.history.undo(history)));
            }
        });
        this.history.add(history);
    }

    public void copyTextInstruction(InstructionComponent instruction) {
        SystemUtil.copyToClipboard(getInstructionRawText(instruction));
    }

    public void duplicateInstruction(InstructionComponent instruction) {
        final InstructionComponent copy = instruction.copy();
        this.addHistory(new InstructionDuplicateHistory(new InstructionPosition(this.instructions, instruction, instructions.indexOf(instruction)), copy));
        this.instructions.add(instructions.indexOf(instruction), copy);
        this.instructions.queueIdReset();
    }

    public boolean isDraggingInstruction(InstructionComponent instructionComponent) {
        return draggingInstruction != null && draggingInstruction.getComponent() == instructionComponent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssemblerFrame that = (AssemblerFrame) o;
        return Objects.equals(methodInput, that.methodInput);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodInput);
    }

    @Override
    public String getCaption() {
        return "Assembler";
    }
}
