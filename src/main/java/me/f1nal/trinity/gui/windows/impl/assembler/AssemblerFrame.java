package me.f1nal.trinity.gui.windows.impl.assembler;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiFocusedFlags;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.compile.Console;
import me.f1nal.trinity.execution.compile.SafeClassWriter;
import me.f1nal.trinity.gui.components.events.MouseClickType;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.popup.PopupMenu;
import me.f1nal.trinity.gui.components.popup.PopupMenuBar;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.gui.windows.impl.assembler.args.InstructionOperand;
import me.f1nal.trinity.gui.windows.impl.assembler.drag.InstructionPosition;
import me.f1nal.trinity.gui.windows.impl.assembler.history.*;
import me.f1nal.trinity.gui.windows.impl.assembler.line.AssemblerInstructionTable;
import me.f1nal.trinity.gui.windows.impl.assembler.line.Instruction2SourceMapping;
import me.f1nal.trinity.gui.windows.impl.assembler.line.InstructionDrag;
import me.f1nal.trinity.gui.windows.impl.assembler.line.InstructionReferenceArrow;
import me.f1nal.trinity.gui.windows.impl.assembler.popup.AssemblerBytecodeGoToIndexPopup;
import me.f1nal.trinity.gui.windows.impl.assembler.popup.AssemblerUnsavedChangesPopup;
import me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.AssemblerEditInstructionPopup;
import me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.EditField;
import me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.EditingInstruction;
import me.f1nal.trinity.gui.viewport.notifications.ICaption;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.SystemUtil;
import me.f1nal.trinity.util.animation.Animation;
import me.f1nal.trinity.util.animation.Easing;
import me.f1nal.trinity.util.history.ChangeManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.util.*;

public final class AssemblerFrame extends ClosableWindow implements ICaption {
    /**
     * Method that we are inspecting.
     */
    private final MethodInput methodInput;
    private final AssemblerDocument document;
    private final Instruction2SourceMapping sourceMapping;
    private InstructionList instructions;
    /**
     * Instruction cursor used as the insertion point.
     */
    private InstructionComponent selected;
    private final Set<InstructionComponent> selectedInstructions = new LinkedHashSet<>();
    private ChangeManager<AssemblerHistory> history;
    public InstructionDrag draggingInstruction;
    private InstructionComponent scrollTo;
    private AssemblerInstructionDecoder decoder;
    private AssemblerInstructionTable instructionTable;
    private InstructionComponent lastHoveredInstruction;
    /**
     * Dragging reference arrow.
     */
    public InstructionReferenceArrow draggingReferenceArrow;
    private PopupMenu popupMenu = new PopupMenu();
    private final PopupMenuBar popupMenuBar;
    private AssemblerHistoryFrame historyFrame = null;
    private boolean methodNotFresh, saveMethod;
    private String saveError;
    private List<String> validationWarnings = List.of();
    private boolean closePromptOpen;
    private boolean closeAfterSave;
    private boolean forceClose;
    private final Deque<DocumentSnapshot> undoSnapshots = new ArrayDeque<>();
    private final Deque<DocumentSnapshot> redoSnapshots = new ArrayDeque<>();
    private boolean externalChanges;
    private long nextExternalCheck;

    public AssemblerFrame(Trinity trinity, MethodInput methodInput, Instruction2SourceMapping sourceMapping) {
        super("Assembler: " + methodInput.getName() + methodInput.getDescriptor(), 660, 500, trinity);
        this.methodInput = methodInput;
        this.document = new AssemblerDocument(methodInput);
        this.sourceMapping = sourceMapping;
        this.setInstructions();
        this.popupMenuBar = new PopupMenuBar(this.createMenuBar());
        this.windowFlags |= ImGuiWindowFlags.MenuBar;
    }

    public PopupMenu getPopupMenu() {
        return popupMenu;
    }

    /**
     * Rebuilds the instruction view.
     */
    private void setInstructions() {
        setInstructions(true);
    }

    private void setInstructions(boolean resetHistory) {
        this.saveMethod = this.methodNotFresh = false;
        this.selected = null;
        this.selectedInstructions.clear();
        this.lastHoveredInstruction = null;
        if (resetHistory || this.history == null) this.history = new ChangeManager<>();
        this.decoder = new AssemblerInstructionDecoder(this, methodInput);
        this.instructions = this.decoder.buildInstructions(document.getInstructions());
        this.instructionTable = null;
        this.instructions.setIdsIfReset();
        this.methodNotFresh = document.isDirty();
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

        boolean keyboardShortcuts = ImGui.isWindowFocused(ImGuiFocusedFlags.RootAndChildWindows)
                && !ImGui.isAnyItemActive();
        if (keyboardShortcuts && ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(ImGuiKey.Z)) {
            this.undoHistory();
        }
        if (keyboardShortcuts && ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(ImGuiKey.Y)) {
            this.redoHistory();
        }
        if (keyboardShortcuts && ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(ImGuiKey.C)) {
            this.copySelectedInstructions();
        }
        if (keyboardShortcuts && ImGui.isKeyPressed(ImGuiKey.Escape)) {
            this.clearInstructionSelection();
        }
        this.popupMenuBar.draw();

        long now = System.nanoTime();
        if (now >= nextExternalCheck) {
            externalChanges = document.hasExternalChanges();
            nextExternalCheck = now + 1_000_000_000L;
        }
        if (externalChanges) {
            ImGui.textColored(CodeColorScheme.NOTIFY_ERROR,
                    "This method changed outside the assembler. Reload or explicitly overwrite before saving.");
            if (ImGui.smallButton("Reload external version")) {
                document.reload();
                undoSnapshots.clear();
                redoSnapshots.clear();
                setInstructions();
                externalChanges = false;
                saveError = null;
            }
            ImGui.sameLine();
            if (ImGui.smallButton("Allow overwrite")) {
                document.acceptExternalVersionForOverwrite();
                externalChanges = false;
            }
        }
        for (String warning : validationWarnings) ImGui.textColored(CodeColorScheme.NOTIFY_WARN, warning);

        ImVec2 vMin = ImGui.getWindowContentRegionMin();
        ImVec2 vMax = ImGui.getWindowContentRegionMax();
        vMin.x += ImGui.getWindowPos().x;
        vMin.y += ImGui.getWindowPos().y;
        vMax.x += ImGui.getWindowPos().x;
        vMax.y += ImGui.getWindowPos().y;

        if (instructionTable == null) {
            instructionTable = new AssemblerInstructionTable(this, this.getInstructions(), this.sourceMapping);
        }
        AssemblerInstructionTable table = instructionTable;
        final float height = table.draw(vMin, vMax);
        InstructionComponent hoveredInstruction = table.getHoveredInstruction();
        if (hoveredInstruction != null) this.lastHoveredInstruction = hoveredInstruction;
        if (keyboardShortcuts && ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(ImGuiKey.V)) {
            this.pasteInstructions(hoveredInstruction);
        }

        // Block ImGui input to the window content
        ImGui.beginDisabled();
        ImGui.invisibleButton(getId("InvisBtn"), ImGui.getContentRegionAvailX(), height);
        ImGui.endDisabled();

        this.drawSaveError();

        if (this.methodNotFresh) {
            if (ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(ImGuiKey.S)) {
                this.saveMethod = true;
            }
        }

        if (this.draggingInstruction != null && !ImGui.isMouseDown(0)) {
            if (this.draggingInstruction.getIndex() != this.instructions.indexOf(this.draggingInstruction.getComponent())) {
                this.addHistory(new InstructionDragHistory(new InstructionPosition(this.instructions, this.draggingInstruction.getComponent(), this.draggingInstruction.getIndex())));
            } else if (!undoSnapshots.isEmpty()) {
                undoSnapshots.pop();
            }
            this.instructions.queueIdReset();
            this.draggingInstruction = null;
        }

        if (this.instructions.setIdsIfReset()) {
            this.methodNotFresh = true;
        }

        if (this.saveMethod) {
            this.saveMethod = false;
            try {
                int count = this.issueSave();
                this.methodNotFresh = false;
                this.saveError = null;
                this.externalChanges = false;
                Logging.info("Saved method");
                trinity.getExecution().getXrefMap().refreshMethod(methodInput);
                trinity.getEventManager().postEvent(new EventMemberModified(this.methodInput));
                Main.getDisplayManager().addNotification(new Notification(NotificationType.SUCCESS, this, ColoredStringBuilder.create()
                        .fmt("Saved {} instructions to {}", count, methodInput.getDisplayName().getName()).get()));
                if (closeAfterSave) {
                    forceClose = true;
                    super.close();
                }
            } catch (Throwable throwable) {
                this.saveError = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
            }
        }

        this.getPopupMenu().draw();
        if (ImGui.isMouseClicked(0)
                && !ImGui.isWindowHovered(ImGuiHoveredFlags.RootAndChildWindows)) {
            this.clearInstructionSelection();
        }
    }

    private void drawSaveError() {
        if (saveError == null) return;

        final float padding = 8.F;
        final float spacing = 4.F;
        float cursorX = ImGui.getCursorPosX();
        float cursorY = ImGui.getCursorPosY();
        float screenX = ImGui.getCursorScreenPosX();
        float screenY = ImGui.getCursorScreenPosY();
        float width = Math.max(1.F, ImGui.getContentRegionAvailX());
        float textWidth = Math.max(1.F, width - padding * 2.F);
        float height = ImGui.calcTextSize(saveError, false, textWidth).y + padding * 2.F;

        ImGui.getWindowDrawList().addRectFilled(screenX, screenY, screenX + width, screenY + height,
                CodeColorScheme.setAlpha(CodeColorScheme.NOTIFY_ERROR, 32), 3.F);
        ImGui.getWindowDrawList().addRect(screenX, screenY, screenX + width, screenY + height,
                CodeColorScheme.setAlpha(CodeColorScheme.NOTIFY_ERROR, 110));

        ImGui.setCursorPos(cursorX + padding, cursorY + padding);
        ImGui.pushTextWrapPos(cursorX + width - padding);
        ImGui.textColored(CodeColorScheme.NOTIFY_ERROR, saveError);
        ImGui.popTextWrapPos();
        ImGui.setCursorPos(cursorX, cursorY + height);
        ImGui.dummy(0.F, spacing);
    }

    private PopupItemBuilder createMenuBar() {
        return PopupItemBuilder.create().
                menu("File", file -> {
                    file.menuItem("Save", "Ctrl+S", this.methodNotFresh, () -> this.saveMethod = true)
                            .separator()
                            .menuItem("Go to Method", () -> Main.getDisplayManager().openDecompilerView(this.methodInput));
                }).
                menu("Edit", edit -> {
                    edit.disabled(() -> this.selectedInstructions.isEmpty(), copy ->
                                    copy.menuItem("Copy", "Ctrl+C", this::copySelectedInstructions))
                            .menuItem("Copy All", this::copyAllInstructions)
                            .disabled(() -> this.lastHoveredInstruction == null, paste ->
                                    paste.menuItem("Paste", "Ctrl+V", this::pasteInstructions));
                }).
                menu("View", view -> {
                    view.menuItem("Hide Metadata", Main.getPreferences().isAssemblerHideMetadata(), this::toggleMetadataVisibility)
                            .separator()
                            .menuItem("Refresh", this::setInstructions)
                    ;
                }).
                menu("Bytecode", bytecode -> {
                    bytecode.menuItem("Insert First...", () -> this.openInsertDialog(0))
                            .menuItem("Insert Last...", () -> this.openInsertDialog(instructions.size()))
                            .separator()
                            .menuItem("Go to index", () -> Main.getWindowManager().addPopup(new AssemblerBytecodeGoToIndexPopup(this, trinity)))
                            .separator()
                            .menuItem("Recompute Frames", this::recomputeFramesAndMaxs)
                            .separator()
                            .menuItem("Clear All", this::clearAll)
                    ;
                }).
                menu("History", history -> {
                    history.menuItem("View History...", this::openHistoryViewer)
                            .menuItem("Undo", "Ctrl+Z", !undoSnapshots.isEmpty(), this::undoHistory)
                            .menuItem("Redo", "Ctrl+Y", !redoSnapshots.isEmpty(), this::redoHistory)
                    ;
                })
        ;
    }

    private void toggleMetadataVisibility() {
        Main.getPreferences().setAssemblerHideMetadata(!Main.getPreferences().isAssemblerHideMetadata());
        this.popupMenuBar.set(this.createMenuBar());
    }

    private void openHistoryViewer() {
        this.historyFrame = new AssemblerHistoryFrame(this);
        this.historyFrame.setVisible(true);
    }

    private void clearAll() {
        pushMutation(captureSnapshot(true));
        this.addHistory(new InstructionClearHistory(instructions));
        instructions.clear();
        selected = null;
        selectedInstructions.clear();
        lastHoveredInstruction = null;
        MethodNode method = document.getMethod();
        if (method.tryCatchBlocks != null) method.tryCatchBlocks.clear();
        method.localVariables = null;
        method.visibleLocalVariableAnnotations = null;
        method.invisibleLocalVariableAnnotations = null;
        method.maxStack = 0;
        method.maxLocals = 0;
        instructions.queueIdReset();
        decoder.rebuildReferenceArrows(instructions);
    }

    /**
     * Saves method
     */
    private int issueSave() {
        if (document.hasExternalChanges()) {
            throw new IllegalStateException("The live method changed; reload it or choose Allow overwrite first");
        }
        List<AbstractInsnNode> ordered = instructions.stream().map(InstructionComponent::getInstruction).toList();
        MethodNode candidate = document.buildCandidate(ordered);
        AssemblerValidationResult validation = AssemblerValidator.validate(methodInput.getOwningClass(), candidate);
        validationWarnings = validation.getWarnings();
        if (!validation.isValid()) throw new IllegalStateException(String.join("\n", validation.getErrors()));
        document.commit(candidate);
        return instructions.getExecutableCount();
    }

    public ChangeManager<AssemblerHistory> getHistory() {
        return history;
    }

    private void undoHistory() {
        if (undoSnapshots.isEmpty()) return;
        DocumentSnapshot snapshot = undoSnapshots.pop();
        redoSnapshots.push(captureSnapshot(snapshot.method() != null));
        restoreSnapshot(snapshot);
        if (!history.isEmpty()) history.removeLast();
    }

    private void redoHistory() {
        if (redoSnapshots.isEmpty()) return;
        DocumentSnapshot snapshot = redoSnapshots.pop();
        undoSnapshots.push(captureSnapshot(snapshot.method() != null));
        restoreSnapshot(snapshot);
    }

    private void beginMutation() {
        pushMutation(captureSnapshot(false));
    }

    private void pushMutation(DocumentSnapshot snapshot) {
        undoSnapshots.push(snapshot);
        redoSnapshots.clear();
    }

    private DocumentSnapshot captureSnapshot(boolean deep) {
        document.replaceInstructionOrder(instructions.stream().map(InstructionComponent::getInstruction).toList());
        MethodNode copy = deep ? AssemblerDocument.cloneMethod(document.getMethod()) : null;
        List<AbstractInsnNode> order = deep ? List.of() : List.of(document.getMethod().instructions.toArray());
        List<String> names = new ArrayList<>();
        for (AbstractInsnNode node : document.getMethod().instructions) {
            if (node instanceof LabelNode label) {
                names.add(methodInput.getLabelTable().getLabel(label.getLabel()).getName());
            }
        }
        return new DocumentSnapshot(copy, order, names);
    }

    private void restoreSnapshot(DocumentSnapshot snapshot) {
        if (snapshot.method() != null) document.replaceWorkingMethod(snapshot.method());
        else document.replaceInstructionOrder(snapshot.instructions());
        applyLabelNames(snapshot.labelNames());
        setInstructions(false);
    }

    private void applyLabelNames(List<String> names) {
        int index = 0;
        for (AbstractInsnNode node : document.getMethod().instructions) {
            if (node instanceof LabelNode label && index < names.size()) {
                methodInput.getLabelTable().getLabel(label.getLabel()).getNameProperty().set(names.get(index++));
            }
        }
    }

    private record DocumentSnapshot(MethodNode method, List<AbstractInsnNode> instructions, List<String> labelNames) {
    }

    @Override
    public String getTitle() {
        return super.getTitle() + ": " + instructions.getExecutableCount() + " instructions";
    }

    public void moveInstruction(InstructionComponent instruction, int delta) {
        final int index = instructions.indexOf(instruction);
        List<InstructionComponent> executable = instructions.stream()
                .filter(component -> component.getInstruction().getOpcode() >= 0).toList();
        int executableIndex = executable.indexOf(instruction);
        int targetExecutableIndex = executableIndex + delta;
        if (targetExecutableIndex < 0 || targetExecutableIndex >= executable.size()) return;
        beginMutation();
        InstructionComponent target = executable.get(targetExecutableIndex);
        int nextIndex = instructions.indexOf(target);
        if (delta < 0) {
            while (nextIndex > 0 && instructions.get(nextIndex - 1).getInstruction().getOpcode() < 0) nextIndex--;
        } else {
            nextIndex++;
        }
        if (this.moveInstructionTo(instruction, nextIndex)) {
            this.addHistory(new InstructionDragHistory(new InstructionPosition(this.instructions, instruction, index)));
        }
    }

    public boolean moveInstructionTo(InstructionComponent instruction, int position) {
        if (position < 0 || position > this.instructions.size()) return false;

        int instructionIndex = instructions.indexOf(instruction);
        if (instructionIndex < 0) return false;

        int insertion = position;
        if (insertion > instructionIndex) insertion--;
        if (insertion == instructionIndex) return false;

        instructions.remove(instructionIndex);
        instructions.add(insertion, instruction);
        this.instructions.queueIdReset();
        decoder.rebuildReferenceArrows(instructions);
        return true;
    }

    public void openInsertDialog(int index) {
        while (index > 0 && index < instructions.size()
                && instructions.get(index).getInstruction().getOpcode() >= 0
                && instructions.get(index - 1).getInstruction().getOpcode() < 0) index--;
        int insertionIndex = index;
        Main.getWindowManager().addPopup(new AssemblerEditInstructionPopup(trinity, methodInput, this::findDefaultLabel,
                document::createIdentityLabelMap, (result) -> {
            this.insertInstruction(insertionIndex, result.getInsnNode());
        }));
    }

    public void openEditDialog(int index) {
        final var popup = new AssemblerEditInstructionPopup(trinity, methodInput, this::findDefaultLabel,
                document::createIdentityLabelMap, (result) -> {
            this.setInstruction(index, result.getInsnNode());
        });

        final var component = instructions.get(index);
        final var editingInstruction = new EditingInstruction(trinity, document.createIdentityLabelMap(), component.getInstruction());

        popup.setOpcodeName(component.getName());
        editingInstruction.addInstructionFields(popup.getMethodInput());
        popup.setInstruction(editingInstruction);

        for (final EditField<?> editField : popup.getInstruction().getEditFieldList()) {
            editField.updateField();
        }

        Main.getWindowManager().addPopup(popup);
    }

    private void insertInstruction(int index, AbstractInsnNode insnNode) {
        beginMutation();
        for (LabelNode referenced : getReferencedLabels(insnNode)) {
            if (instructions.indexOfInsn(referenced) == -1) {
                int targetIndex = 0;
                while (targetIndex < instructions.size()
                        && instructions.get(targetIndex).getInstruction().getOpcode() < 0) targetIndex++;
                if (targetIndex == instructions.size()) targetIndex = instructions.size();
                instructions.add(targetIndex, decoder.translateInstruction(referenced));
                if (targetIndex < index) index++;
            }
        }
        InstructionComponent component = decoder.translateInstruction(insnNode);
        instructions.add(index, component);
        instructions.queueIdReset();
        decoder.rebuildReferenceArrows(instructions);
        this.addHistory(new InstructionInsertHistory(new InstructionPosition(this.instructions, component, instructions.indexOf(component))));
    }

    private void recomputeFramesAndMaxs() {
        try {
            DocumentSnapshot before = captureSnapshot(true);
            List<AbstractInsnNode> ordered = instructions.stream().map(InstructionComponent::getInstruction).toList();
            MethodNode candidate = document.buildCandidate(ordered);
            ClassNode owner = new ClassNode(org.objectweb.asm.Opcodes.ASM9);
            methodInput.getOwningClass().getNode().accept(owner);
            for (int i = 0; i < owner.methods.size(); i++) {
                MethodNode method = owner.methods.get(i);
                if (method.name.equals(candidate.name) && method.desc.equals(candidate.desc)) {
                    owner.methods.set(i, candidate);
                    break;
                }
            }
            SafeClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS,
                    typeName -> {
                        ClassInput input = trinity.getExecution().getClassInput(typeName);
                        return input == null ? trinity.getJrtInput().getClass(typeName) : input.getNode();
                    }, new Console());
            owner.accept(writer);
            ClassNode computedOwner = new ClassNode(org.objectweb.asm.Opcodes.ASM9);
            new ClassReader(writer.toByteArray()).accept(computedOwner, 0);
            MethodNode computed = computedOwner.methods.stream()
                    .filter(method -> method.name.equals(candidate.name) && method.desc.equals(candidate.desc))
                    .findFirst().orElseThrow(() -> new IllegalStateException("Recomputed method was not found"));
            document.replaceWorkingMethod(computed);
            methodInput.getLabelTable().reset();
            applyLabelNames(before.labelNames());
            pushMutation(before);
            setInstructions(false);
            saveError = null;
        } catch (Throwable throwable) {
            saveError = "Could not recompute frames/maxs: "
                    + (throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage());
        }
    }

    private LabelNode findDefaultLabel() {
        for (InstructionComponent component : instructions) {
            if (component.getInstruction() instanceof LabelNode label) return label;
        }
        return new LabelNode();
    }

    private static Set<LabelNode> getReferencedLabels(AbstractInsnNode instruction) {
        Set<LabelNode> labels = Collections.newSetFromMap(new IdentityHashMap<>());
        if (instruction instanceof JumpInsnNode jump) labels.add(jump.label);
        if (instruction instanceof TableSwitchInsnNode table) {
            labels.add(table.dflt);
            labels.addAll(table.labels);
        }
        if (instruction instanceof LookupSwitchInsnNode lookup) {
            labels.add(lookup.dflt);
            labels.addAll(lookup.labels);
        }
        if (instruction instanceof LineNumberNode line) labels.add(line.start);
        return labels;
    }

    private void setInstruction(final int index, final AbstractInsnNode instruction) {
        beginMutation();
        InstructionComponent oldInstruction = instructions.get(index);
        InstructionComponent component = decoder.translateInstruction(instruction);

        instructions.set(index, component);
        if (selected == oldInstruction) selected = component;
        if (selectedInstructions.remove(oldInstruction)) selectedInstructions.add(component);
        if (lastHoveredInstruction == oldInstruction) lastHoveredInstruction = component;
        instructions.queueIdReset();
        decoder.rebuildReferenceArrows(instructions);
        this.addHistory(new InstructionSetHistory(oldInstruction, new InstructionPosition(this.instructions, component, instructions.indexOf(component))));
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

    public void toggleInstructionSelection(InstructionComponent instruction) {
        if (!selectedInstructions.remove(instruction)) {
            selectedInstructions.add(instruction);
            selected = instruction;
        } else if (selected == instruction) {
            selected = selectedInstructions.stream().reduce((first, second) -> second).orElse(null);
        }
    }

    public void clearInstructionSelection() {
        selected = null;
        selectedInstructions.clear();
    }

    public boolean isInstructionSelected(InstructionComponent instruction) {
        return selectedInstructions.contains(instruction);
    }

    public List<InstructionComponent> getSelectedInstructions() {
        return instructions.stream().filter(selectedInstructions::contains).toList();
    }

    public InstructionList getInstructions() {
        return instructions;
    }

    public void deleteInstruction(InstructionComponent instruction) {
        for (InstructionReferenceArrow arrow : instructions.getInstructionReferenceArrowList()) {
            if (arrow.getTo() == instruction) {
                saveError = "Cannot delete this instruction: label " + arrow.getLabel().getName()
                        + " is still referenced by " + arrow.getFrom().getName();
                return;
            }
        }
        if (instruction.getInstruction() instanceof LabelNode label && isLabelReferenced(label)) {
            saveError = "Cannot delete label " + methodInput.getLabelTable().getLabel(label.getLabel()).getName()
                    + ": it is still referenced by code metadata";
            return;
        }
        beginMutation();
        this.selectedInstructions.remove(instruction);
        if (this.lastHoveredInstruction == instruction) this.lastHoveredInstruction = null;
        if (this.selected == instruction) {
            this.selected = selectedInstructions.stream().reduce((first, second) -> second).orElse(null);
        }
        this.addHistory(new InstructionDeleteHistory(new InstructionPosition(this.instructions, instruction, instructions.indexOf(instruction))));
        this.instructions.remove(instruction);
        this.instructions.removeReferenceArrowsFrom(instruction);
        this.instructions.queueIdReset();
        decoder.rebuildReferenceArrows(instructions);
    }

    public void retargetReference(InstructionReferenceArrow arrow, InstructionComponent target) {
        beginMutation();
        int targetIndex = instructions.indexOf(target);
        LabelNode label = null;
        for (int i = targetIndex - 1; i >= 0; i--) {
            AbstractInsnNode previous = instructions.get(i).getInstruction();
            if (previous.getOpcode() >= 0) break;
            if (previous instanceof LabelNode found) {
                label = found;
                break;
            }
        }
        if (label == null) {
            label = new LabelNode();
            instructions.add(targetIndex, decoder.translateInstruction(label));
        }
        InstructionComponent source = arrow.getFrom();
        int sourceIndex = instructions.indexOf(source);
        AbstractInsnNode replacement = source.getInstruction().clone(document.createIdentityLabelMap());
        arrow.updateLabel(replacement, label);
        instructions.set(sourceIndex, decoder.translateInstruction(replacement));
        instructions.queueIdReset();
        decoder.rebuildReferenceArrows(instructions);
    }

    private boolean isLabelReferenced(LabelNode target) {
        document.replaceInstructionOrder(instructions.stream().map(InstructionComponent::getInstruction).toList());
        MethodNode method = document.getMethod();
        for (AbstractInsnNode node : method.instructions) {
            if (node == target) continue;
            if (node instanceof JumpInsnNode jump && jump.label == target) return true;
            if (node instanceof TableSwitchInsnNode table
                    && (table.dflt == target || table.labels.contains(target))) return true;
            if (node instanceof LookupSwitchInsnNode lookup
                    && (lookup.dflt == target || lookup.labels.contains(target))) return true;
            if (node instanceof LineNumberNode line && line.start == target) return true;
            if (node instanceof FrameNode frame
                    && (frame.local != null && frame.local.contains(target)
                    || frame.stack != null && frame.stack.contains(target))) return true;
        }
        if (method.tryCatchBlocks != null) for (TryCatchBlockNode block : method.tryCatchBlocks) {
            if (block.start == target || block.end == target || block.handler == target) return true;
        }
        if (method.localVariables != null) for (LocalVariableNode local : method.localVariables) {
            if (local.start == target || local.end == target) return true;
        }
        if (method.visibleLocalVariableAnnotations != null) for (LocalVariableAnnotationNode annotation : method.visibleLocalVariableAnnotations) {
            if (annotation.start.contains(target) || annotation.end.contains(target)) return true;
        }
        if (method.invisibleLocalVariableAnnotations != null) for (LocalVariableAnnotationNode annotation : method.invisibleLocalVariableAnnotations) {
            if (annotation.start.contains(target) || annotation.end.contains(target)) return true;
        }
        return false;
    }

    private void addHistory(AssemblerHistory history) {
        history.getBrowserViewerNode().addMouseClickHandler((clickType) -> {
            if (clickType == MouseClickType.RIGHT_CLICK) {
                Main.getDisplayManager().showPopup(PopupItemBuilder.create().menuItem("Undo latest", this::undoHistory));
            }
        });
        this.history.add(history);
    }

    public void copySelectedInstructions() {
        List<InstructionComponent> selection = this.getSelectedInstructions();
        if (selection.isEmpty()) return;
        this.copyInstructions(selection);
        this.clearInstructionSelection();
    }

    public void copySelectionOrInstruction(InstructionComponent instruction) {
        if (selectedInstructions.contains(instruction)) this.copySelectedInstructions();
        else this.copyInstructions(List.of(instruction));
    }

    public void copyAllInstructions() {
        this.copyInstructions(this.instructions);
    }

    private void copyInstructions(Collection<InstructionComponent> components) {
        List<AbstractInsnNode> nodes = components.stream().map(InstructionComponent::getInstruction).toList();
        SystemUtil.copyToClipboard(AssemblerClipboardCodec.format(nodes,
                label -> methodInput.getLabelTable().getLabel(label.getLabel()).getName()));
    }

    public void pasteInstructions() {
        this.pasteInstructions(lastHoveredInstruction);
    }

    public void pasteInstructions(InstructionComponent target) {
        if (target == null || !instructions.contains(target)) {
            saveError = "Hover an instruction to choose where pasted bytecode should be inserted";
            return;
        }
        String clipboard = SystemUtil.getClipboard();
        if (clipboard == null || clipboard.isBlank()) {
            saveError = "The clipboard does not contain assembler instructions";
            return;
        }

        final AssemblerClipboardCodec.ParsedInstructions parsed;
        try {
            parsed = AssemblerClipboardCodec.parse(clipboard, this::findLabelByName);
        } catch (IllegalArgumentException exception) {
            saveError = "Could not paste instructions: " + exception.getMessage();
            return;
        }
        if (parsed.instructions().isEmpty()) {
            saveError = "The clipboard does not contain assembler instructions";
            return;
        }

        beginMutation();
        this.applyPastedLabelNames(parsed);
        int insertionIndex = instructions.indexOf(target);
        List<InstructionComponent> pasted = new ArrayList<>(parsed.instructions().size());
        for (AbstractInsnNode instruction : parsed.instructions()) {
            InstructionComponent component = decoder.translateInstruction(instruction);
            instructions.add(insertionIndex++, component);
            pasted.add(component);
        }
        instructions.queueIdReset();
        decoder.rebuildReferenceArrows(instructions);
        this.addHistory(new InstructionPasteHistory(instructions, pasted));

        this.clearInstructionSelection();
        saveError = null;
    }

    private LabelNode findLabelByName(String name) {
        for (InstructionComponent component : instructions) {
            if (component.getInstruction() instanceof LabelNode label
                    && methodInput.getLabelTable().getLabel(label.getLabel()).getName().equals(name)) {
                return label;
            }
        }
        return null;
    }

    private void applyPastedLabelNames(AssemblerClipboardCodec.ParsedInstructions parsed) {
        Set<String> usedNames = new HashSet<>();
        for (InstructionComponent component : instructions) {
            if (component.getInstruction() instanceof LabelNode label) {
                usedNames.add(methodInput.getLabelTable().getLabel(label.getLabel()).getName());
            }
        }
        for (AbstractInsnNode instruction : parsed.instructions()) {
            if (!(instruction instanceof LabelNode label)) continue;
            String requested = parsed.labelNames().get(label);
            if (requested == null) continue;
            String unique = requested;
            int suffix = 2;
            while (!usedNames.add(unique)) unique = requested + "_" + suffix++;
            methodInput.getLabelTable().getLabel(label.getLabel()).getNameProperty().set(unique);
        }
    }

    public void duplicateInstruction(InstructionComponent instruction) {
        beginMutation();
        AbstractInsnNode copiedNode = instruction.getInstruction() instanceof LabelNode
                ? new LabelNode() : instruction.getInstruction().clone(document.createIdentityLabelMap());
        final InstructionComponent copy = decoder.translateInstruction(copiedNode);
        this.addHistory(new InstructionDuplicateHistory(new InstructionPosition(this.instructions, instruction, instructions.indexOf(instruction)), copy));
        this.instructions.add(instructions.indexOf(instruction), copy);
        this.instructions.queueIdReset();
        decoder.rebuildReferenceArrows(instructions);
    }

    public boolean isDraggingInstruction(InstructionComponent instructionComponent) {
        return draggingInstruction != null && draggingInstruction.getComponent() == instructionComponent;
    }

    @Override
    public String getCaption() {
        return "Assembler";
    }

    @Override
    public boolean isAlreadyOpen(ClosableWindow otherWindow) {
        if (otherWindow instanceof AssemblerFrame) {
            return ((AssemblerFrame) otherWindow).methodInput == this.methodInput;
        }

        return false;
    }

    public void beginDragMutation() {
        beginMutation();
    }

    @Override
    public void close() {
        if (forceClose || !methodNotFresh) {
            super.close();
            return;
        }
        if (closePromptOpen) return;
        closePromptOpen = true;
        Main.getWindowManager().addPopup(new AssemblerUnsavedChangesPopup(trinity,
                () -> {
                    closePromptOpen = false;
                    closeAfterSave = true;
                    saveMethod = true;
                },
                () -> {
                    closePromptOpen = false;
                    forceClose = true;
                    AssemblerFrame.super.close();
                },
                () -> closePromptOpen = false));
    }
}
