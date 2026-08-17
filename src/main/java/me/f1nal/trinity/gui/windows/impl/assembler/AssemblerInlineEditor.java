package me.f1nal.trinity.gui.windows.impl.assembler;

import imgui.ImGui;
import imgui.ImGuiListClipper;
import imgui.ImVec4;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.callback.ImListClipperCallback;
import imgui.type.ImString;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.gui.windows.impl.assembler.line.AssemblerInstructionTable;
import me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.EditField;
import me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.EditingInstruction;
import me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.OpcodeClasses;
import me.f1nal.trinity.gui.windows.impl.assembler.inline.InlineSuggestionMatcher;
import me.f1nal.trinity.theme.CodeColorScheme;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** One-row instruction editor used for both insertion and replacement. */
public final class AssemblerInlineEditor {
    private static final int OPCODE_INPUT = -1;
    private static final int VISIBLE_SUGGESTION_ROWS = 5;
    private static final float OPCODE_MIN_WIDTH = 52.F;
    private static final float FIELD_MIN_WIDTH = 44.F;
    private static final float FIELD_MAX_WIDTH = 260.F;

    private final AssemblerFrame frame;
    private final InstructionComponent target;
    private final int insertionIndex;
    private final ImString opcode = new ImString(64);
    private EditingInstruction draft;
    private ImVec4 bounds;
    private int activeInput = OPCODE_INPUT;
    private int focusInput;
    private boolean requestFocus = true;
    private boolean suggestionsDismissed;
    private int selectedSuggestion;
    private List<String> suggestions = List.of();
    private int rankedSuggestionInput = Integer.MIN_VALUE;
    private String rankedSuggestionQuery;
    private AbstractInsnNode rankedSuggestionDraft;
    private int scrollSelectionFrames;
    private float suggestionX;
    private float suggestionY;
    private float suggestionWidth;
    private float contentWidth;
    private boolean revealRequested = true;
    private final LabelNode originalLabel;
    private final String originalLabelName;

    private AssemblerInlineEditor(AssemblerFrame frame, InstructionComponent target,
                                  int insertionIndex, int focusInput) {
        this.frame = frame;
        this.target = target;
        this.insertionIndex = insertionIndex;
        this.focusInput = focusInput;
        this.activeInput = focusInput;
        if (target != null) {
            this.opcode.set(target.getName());
            this.draft = frame.createInlineDraft(target.getInstruction());
            this.prepareFields();
            if (this.focusInput >= this.draft.getEditFieldList().size()) {
                this.focusInput = this.draft.getEditFieldList().isEmpty()
                        ? OPCODE_INPUT : this.draft.getEditFieldList().size() - 1;
                this.activeInput = this.focusInput;
            }
        }
        this.originalLabel = target != null && target.getInstruction() instanceof LabelNode label
                ? label : null;
        this.originalLabelName = originalLabel == null ? null
                : frame.getMethodInput().getLabelTable().getLabel(originalLabel.getLabel()).getName();
    }

    public static AssemblerInlineEditor insert(AssemblerFrame frame, int insertionIndex) {
        return new AssemblerInlineEditor(frame, null, insertionIndex, OPCODE_INPUT);
    }

    public static AssemblerInlineEditor edit(AssemblerFrame frame, InstructionComponent target,
                                             int operandIndex) {
        int focus = operandIndex < 0 ? OPCODE_INPUT : operandIndex;
        return new AssemblerInlineEditor(frame, target, -1, focus);
    }

    public boolean isInsertion() {
        return target == null;
    }

    public int getInsertionIndex() {
        return insertionIndex;
    }

    public boolean edits(InstructionComponent component) {
        return target == component;
    }

    public InstructionComponent getTarget() {
        return target;
    }

    public void setBounds(float x, float y, float height) {
        this.bounds = new ImVec4(x, y, 0x10000, height);
    }

    public ImVec4 getBounds() {
        return bounds;
    }

    public float getContentWidth() {
        return contentWidth;
    }

    public boolean consumeRevealRequest() {
        boolean requested = revealRequested;
        revealRequested = false;
        return requested;
    }

    public void draw(AssemblerInstructionTable table) {
        if (bounds == null) return;
        float originalX = ImGui.getCursorScreenPosX();
        float originalY = ImGui.getCursorScreenPosY();
        float x = bounds.x + table.instructionStartX + 5.F;
        float y = bounds.y;

        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 0.F);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 2.F, 0.F);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 3.F, 0.F);
        try {
            this.contentWidth = 0.F;
            float operandX = bounds.x + table.instructionOperandsStartX;
            float opcodeWidth = Math.max(OPCODE_MIN_WIDTH, operandX - x - 3.F);
            this.drawOpcodeInput(x, y, opcodeWidth);
            x = Math.max(operandX, x + opcodeWidth + 3.F);
            if (draft != null) {
                List<EditField<?>> fields = draft.getEditFieldList();
                for (int index = 0; index < fields.size(); index++) {
                    x = this.drawFieldInput(fields.get(index), index, x, y);
                }
            }
            this.drawActions(x, y);
            this.drawSuggestions();
        } finally {
            ImGui.popStyleVar(3);
            ImGui.setCursorScreenPos(originalX, originalY);
        }
    }

    private void drawOpcodeInput(float x, float y, float width) {
        ImGui.setCursorScreenPos(x, y);
        ImGui.setNextItemWidth(width);
        if (requestFocus && focusInput == OPCODE_INPUT) ImGui.setKeyboardFocusHere();
        String before = opcode.get();
        boolean escapePressed = activeInput == OPCODE_INPUT
                && ImGui.isKeyPressed(ImGuiKey.Escape, false);
        ImGui.pushStyleColor(ImGuiCol.Text, CodeColorScheme.KEYWORD);
        boolean valueChanged = ImGui.inputTextWithHint("###AssemblerInlineOpcode" + identity(),
                "opcode", opcode, ImGuiInputTextFlags.AutoSelectAll);
        ImGui.popStyleColor();
        if (escapePressed) opcode.set(before);
        boolean changed = valueChanged || ImGui.isItemEdited() || !before.equals(opcode.get());
        boolean focused = ImGui.isItemActive() || ImGui.isItemFocused();
        boolean submitted = focused && ImGui.isKeyPressed(ImGuiKey.Enter, false);
        if (focused) this.captureSuggestionAnchor(width);
        if (changed) {
            suggestionsDismissed = false;
            selectedSuggestion = 0;
            scrollSelectionFrames = 2;
            this.invalidateSuggestionRanking();
            this.selectExactOpcode(false);
        }
        if (focused || escapePressed) this.handleInputKeys(OPCODE_INPUT, submitted);
        this.updateSuggestions(OPCODE_INPUT, focused, changed);
        if (requestFocus && focusInput == OPCODE_INPUT) requestFocus = false;
    }

    private float drawFieldInput(EditField<?> field, int fieldIndex, float x, float y) {
        ImString value = field.getInlineText();
        float width = this.fieldWidth(value.get(), field.getInlineLabel());
        ImGui.setCursorScreenPos(x, y);
        ImGui.setNextItemWidth(width);
        if (!field.isValidInput()) {
            ImGui.pushStyleColor(ImGuiCol.FrameBg,
                    CodeColorScheme.setAlpha(CodeColorScheme.NOTIFY_ERROR, 42));
        }
        if (requestFocus && focusInput == fieldIndex) ImGui.setKeyboardFocusHere();
        String before = value.get();
        boolean escapePressed = activeInput == fieldIndex
                && ImGui.isKeyPressed(ImGuiKey.Escape, false);
        boolean valueChanged = ImGui.inputTextWithHint(
                "###AssemblerInlineField" + identity() + "." + fieldIndex,
                field.getInlineLabel(), value, ImGuiInputTextFlags.AutoSelectAll);
        if (escapePressed) value.set(before);
        boolean changed = valueChanged || ImGui.isItemEdited() || !before.equals(value.get());
        boolean focused = ImGui.isItemActive() || ImGui.isItemFocused();
        boolean submitted = focused && ImGui.isKeyPressed(ImGuiKey.Enter, false);
        if (focused) this.captureSuggestionAnchor(width);
        if (!field.isValidInput()) ImGui.popStyleColor();

        if (changed) {
            field.applyInlineValue(value.get());
            suggestionsDismissed = false;
            selectedSuggestion = 0;
            scrollSelectionFrames = 2;
            this.invalidateSuggestionRanking();
        }
        if (focused || escapePressed) this.handleInputKeys(fieldIndex, submitted);
        this.updateSuggestions(fieldIndex, focused, changed);
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.text(field.getInlineLabel());
            String error = field.getInlineError();
            if (error != null && !error.isBlank()) ImGui.textColored(CodeColorScheme.NOTIFY_ERROR, error);
            ImGui.endTooltip();
        }
        if (requestFocus && focusInput == fieldIndex) requestFocus = false;
        return x + width + 3.F;
    }

    private void drawActions(float x, float y) {
        boolean valid = this.isValid();
        String validationIssue = this.validationIssue();
        ImGui.setCursorScreenPos(x, y);
        ImGui.beginDisabled(!valid);
        if (ImGui.smallButton("Apply###AssemblerInlineApply" + identity())) this.commit();
        ImGui.endDisabled();
        ImGui.sameLine(0.F, 3.F);
        if (ImGui.smallButton("Cancel###AssemblerInlineCancel" + identity())) frame.cancelInlineEdit(this);
        if (validationIssue != null) {
            ImGui.sameLine(0.F, 6.F);
            ImGui.textColored(CodeColorScheme.NOTIFY_ERROR, validationIssue);
        }
        this.contentWidth = Math.max(this.contentWidth, ImGui.getItemRectMaxX() - bounds.x + 8.F);
    }

    private void handleInputKeys(int input, boolean submitted) {
        if (activeInput != input) {
            suggestionsDismissed = false;
            selectedSuggestion = 0;
        }
        activeInput = input;
        if (ImGui.isKeyPressed(ImGuiKey.DownArrow, false) && !this.visibleSuggestions().isEmpty()) {
            selectedSuggestion = Math.min(selectedSuggestion + 1, this.visibleSuggestions().size() - 1);
            scrollSelectionFrames = 2;
        }
        if (ImGui.isKeyPressed(ImGuiKey.UpArrow, false) && !this.visibleSuggestions().isEmpty()) {
            selectedSuggestion = Math.max(0, selectedSuggestion - 1);
            scrollSelectionFrames = 2;
        }
        if (ImGui.isKeyPressed(ImGuiKey.Escape, false)) {
            this.applyOnEscape(input);
            return;
        }
        if (!submitted) return;
        List<String> visible = this.visibleSuggestions();
        if (!visible.isEmpty()) {
            this.acceptSuggestion(visible.get(Math.min(selectedSuggestion, visible.size() - 1)), input);
            if (input == OPCODE_INPUT) this.focusNext(OPCODE_INPUT);
            return;
        }
        if (input == OPCODE_INPUT) {
            if (!this.selectExactOpcode(true)) return;
            this.focusNext(OPCODE_INPUT);
        } else {
            EditField<?> field = draft.getEditFieldList().get(input);
            if (!field.applyInlineValue(field.getInlineText().get())) return;
            this.focusNext(input);
        }
    }

    private void focusNext(int input) {
        int fieldCount = draft == null ? 0 : draft.getEditFieldList().size();
        if (input < fieldCount - 1) {
            focusInput = input + 1;
            activeInput = focusInput;
            requestFocus = true;
            suggestionsDismissed = false;
        } else if (this.isValid()) {
            this.commit();
        }
    }

    private void updateSuggestions(int input, boolean focused, boolean force) {
        if (!focused || activeInput != input) return;
        String query = input == OPCODE_INPUT ? opcode.get()
                : draft.getEditFieldList().get(input).getInlineText().get();
        AbstractInsnNode currentDraft = draft == null ? null : draft.getInsnNode();
        if (!force && rankedSuggestionInput == input && query.equals(rankedSuggestionQuery)
                && rankedSuggestionDraft == currentDraft) return;
        Collection<String> candidates = input == OPCODE_INPUT
                ? OpcodeClasses.getNamesToClasses().keySet()
                : this.argumentSuggestions(input);
        suggestions = InlineSuggestionMatcher.ranked(candidates, query);
        rankedSuggestionInput = input;
        rankedSuggestionQuery = query;
        rankedSuggestionDraft = currentDraft;
    }

    private Collection<String> argumentSuggestions(int fieldIndex) {
        Set<String> candidates = new LinkedHashSet<>(draft.getEditFieldList().get(fieldIndex).getInlineSuggestions());
        AbstractInsnNode node = draft.getInsnNode();
        if (node instanceof TypeInsnNode && fieldIndex == 0) candidates.addAll(projectClassNames());
        if (node instanceof MultiANewArrayInsnNode && fieldIndex == 0) {
            for (String name : projectClassNames()) candidates.add("[L" + name + ";");
        }
        if (node instanceof MethodInsnNode method) {
            ClassNode owner = resolveClass(method.owner);
            if (owner != null && fieldIndex == 1) owner.methods.forEach(member -> candidates.add(member.name));
            if (owner != null && fieldIndex == 2) owner.methods.stream()
                    .filter(member -> member.name.equals(method.name)).forEach(member -> candidates.add(member.desc));
        }
        if (node instanceof FieldInsnNode field) {
            ClassNode owner = resolveClass(field.owner);
            if (owner != null && fieldIndex == 1) owner.fields.forEach(member -> candidates.add(member.name));
            if (owner != null && fieldIndex == 2) owner.fields.stream()
                    .filter(member -> member.name.equals(field.name)).forEach(member -> candidates.add(member.desc));
        }
        return candidates;
    }

    private Collection<String> projectClassNames() {
        Set<String> names = new LinkedHashSet<>(
                Main.getTrinity().getExecution().getClassTargetMap().keySet());
        names.addAll(Main.getTrinity().getExecution().getDependencies().getClassNames());
        return names;
    }

    private ClassNode resolveClass(String owner) {
        ClassInput input = Main.getTrinity().getExecution().getClassInput(owner);
        return input != null ? input.getNode() : Main.getTrinity().getExecution().getDependencies().getClass(owner);
    }

    private void drawSuggestions() {
        List<String> visible = this.visibleSuggestions();
        if (visible.isEmpty()) return;
        float rowHeight = ImGui.getFrameHeight();
        int visibleRows = Math.min(VISIBLE_SUGGESTION_ROWS, visible.size());
        float height = visibleRows * rowHeight + 4.F;
        ImGui.setNextWindowPos(suggestionX, suggestionY);
        float scrollbarWidth = visible.size() > VISIBLE_SUGGESTION_ROWS
                ? ImGui.getStyle().getScrollbarSize() : 0.F;
        ImGui.setNextWindowSize(Math.max(suggestionWidth, 150.F) + scrollbarWidth, height);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.F);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 2.F, 2.F);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0.F, 0.F);
        ImGui.pushStyleColor(ImGuiCol.Border, CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 100));
        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.NoDocking
                | ImGuiWindowFlags.NoFocusOnAppearing;
        if (visible.size() > VISIBLE_SUGGESTION_ROWS) flags |= ImGuiWindowFlags.AlwaysVerticalScrollbar;
        if (ImGui.begin("###AssemblerInlineSuggestions" + identity(), flags)) {
            if (scrollSelectionFrames > 0) {
                float selectedTop = selectedSuggestion * rowHeight;
                float selectedBottom = selectedTop + rowHeight;
                float scrollTop = ImGui.getScrollY();
                float scrollBottom = scrollTop + visibleRows * rowHeight;
                if (selectedTop < scrollTop) ImGui.setScrollY(selectedTop);
                else if (selectedBottom > scrollBottom) {
                    ImGui.setScrollY(selectedBottom - visibleRows * rowHeight);
                }
                scrollSelectionFrames--;
            }
            ImGuiListClipper.forEach(visible.size(), new ImListClipperCallback() {
                @Override
                public void accept(int index) {
                    String candidate = visible.get(index);
                    if (ImGui.selectable(candidate + "###AssemblerInlineSuggestion" + identity() + "." + index,
                            selectedSuggestion == index)) {
                        acceptSuggestion(candidate, activeInput);
                    }
                    if (activeInput == OPCODE_INPUT) {
                        String count = Integer.toString(OpcodeClasses.getArgumentCount(candidate));
                        float countX = ImGui.getItemRectMaxX() - ImGui.calcTextSize(count).x - 7.F;
                        float countY = ImGui.getItemRectMinY()
                                + (ImGui.getItemRectSizeY() - ImGui.calcTextSize(count).y) * 0.5F;
                        ImGui.getWindowDrawList().addText(countX, countY,
                                CodeColorScheme.DISABLED, count);
                        if (ImGui.isItemHovered()) {
                            ImGui.beginTooltip();
                            ImGui.text(candidate);
                            ImGui.endTooltip();
                        }
                    }
                }
            });
            if (ImGui.isKeyPressed(ImGuiKey.Escape, false)) {
                this.applyOnEscape(activeInput);
            }
        }
        ImGui.end();
        ImGui.popStyleColor();
        ImGui.popStyleVar(3);
    }

    private void applyOnEscape(int input) {
        suggestionsDismissed = true;
        requestFocus = true;
        focusInput = input;
        if (this.isValid()) this.commit();
        else frame.cancelInlineEdit(this);
    }

    private void acceptSuggestion(String value, int input) {
        suggestionsDismissed = true;
        selectedSuggestion = 0;
        scrollSelectionFrames = 2;
        if (input == OPCODE_INPUT) {
            opcode.set(value.toLowerCase(Locale.ROOT));
            this.selectExactOpcode(true);
        } else if (draft != null && input >= 0 && input < draft.getEditFieldList().size()) {
            EditField<?> field = draft.getEditFieldList().get(input);
            field.getInlineText().set(value);
            field.applyInlineValue(value);
        }
        focusInput = input;
        activeInput = input;
        requestFocus = true;
    }

    private void invalidateSuggestionRanking() {
        rankedSuggestionInput = Integer.MIN_VALUE;
        rankedSuggestionQuery = null;
        rankedSuggestionDraft = null;
    }

    private boolean selectExactOpcode(boolean preserveFocus) {
        String name = opcode.get().trim().toLowerCase(Locale.ROOT);
        if (!OpcodeClasses.getNamesToClasses().containsKey(name)) return false;
        if (draft != null && name.equalsIgnoreCase(opcodeName(draft.getInsnNode()))) return true;

        Class<?> previousType = draft == null ? null : draft.getInsnNode().getClass();
        List<String> previousValues = draft == null ? List.of() : draft.getEditFieldList().stream()
                .map(field -> field.getInlineText().get()).toList();
        if (originalLabel != null && !name.equals("label")) this.restoreOriginalLabelName();
        AbstractInsnNode replacement = originalLabel != null && name.equals("label")
                ? originalLabel : frame.createDefaultInlineInstruction(name);
        draft = frame.createInlineDraft(replacement);
        this.prepareFields();
        if (draft.getInsnNode().getClass() == previousType) {
            for (int index = 0; index < Math.min(previousValues.size(), draft.getEditFieldList().size()); index++) {
                EditField<?> field = draft.getEditFieldList().get(index);
                String previous = previousValues.get(index);
                field.getInlineText().set(previous);
                field.applyInlineValue(previous);
            }
        }
        if (!preserveFocus) {
            focusInput = OPCODE_INPUT;
            activeInput = OPCODE_INPUT;
        }
        return true;
    }

    private void prepareFields() {
        for (EditField<?> field : draft.getEditFieldList()) {
            field.updateField();
            field.prepareInlineValue();
        }
    }

    private boolean isValid() {
        String name = opcode.get().trim().toLowerCase(Locale.ROOT);
        return draft != null && OpcodeClasses.getNamesToClasses().containsKey(name)
                && draft.isValid() && this.validationIssue() == null;
    }

    private String validationIssue() {
        return draft == null ? null : frame.validateInlineReplacement(this, draft.getInsnNode());
    }

    private void commit() {
        if (!this.isValid()) return;
        String editedLabelName = null;
        if (originalLabel != null && draft.getInsnNode() instanceof LabelNode
                && !draft.getEditFieldList().isEmpty()) {
            editedLabelName = draft.getEditFieldList().get(0).getInlineText().get();
            this.restoreOriginalLabelName();
        }
        frame.commitInlineEdit(this, draft.getInsnNode());
        if (editedLabelName != null && !editedLabelName.isBlank()) {
            frame.getMethodInput().getLabelTable().getLabel(originalLabel.getLabel())
                    .getNameProperty().set(editedLabelName);
        }
    }

    public boolean commitIfValid() {
        if (!this.isValid()) return false;
        this.commit();
        return true;
    }

    public void cancel() {
        this.restoreOriginalLabelName();
    }

    private void restoreOriginalLabelName() {
        if (originalLabel != null && originalLabelName != null) {
            frame.getMethodInput().getLabelTable().getLabel(originalLabel.getLabel())
                    .getNameProperty().set(originalLabelName);
        }
    }

    private List<String> visibleSuggestions() {
        return suggestionsDismissed ? List.of() : suggestions;
    }

    private void captureSuggestionAnchor(float width) {
        suggestionX = ImGui.getItemRectMinX();
        suggestionY = ImGui.getItemRectMaxY();
        suggestionWidth = width;
    }

    private float fieldWidth(String value, String label) {
        String measured = value == null || value.isEmpty() ? label : value;
        return Math.max(FIELD_MIN_WIDTH, Math.min(FIELD_MAX_WIDTH,
                ImGui.calcTextSize(measured).x + 10.F));
    }

    private String identity() {
        return Integer.toHexString(System.identityHashCode(frame));
    }

    private static String opcodeName(AbstractInsnNode instruction) {
        int opcode = instruction.getOpcode();
        if (opcode >= 0 && opcode < org.objectweb.asm.util.Printer.OPCODES.length) {
            return org.objectweb.asm.util.Printer.OPCODES[opcode].toLowerCase(Locale.ROOT);
        }
        if (instruction instanceof org.objectweb.asm.tree.LabelNode) return "label";
        if (instruction instanceof org.objectweb.asm.tree.LineNumberNode) return "line";
        if (instruction instanceof org.objectweb.asm.tree.FrameNode) return "frame";
        return "";
    }
}
