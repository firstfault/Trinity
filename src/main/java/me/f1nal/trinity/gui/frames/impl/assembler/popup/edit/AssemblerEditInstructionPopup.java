package me.f1nal.trinity.gui.frames.impl.assembler.popup.edit;

import imgui.ImGui;
import imgui.flag.ImGuiDataType;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiKey;
import imgui.type.ImString;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.frames.Popup;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.UnsafeUtil;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class AssemblerEditInstructionPopup extends Popup {
    private final ImString opcodeName = new ImString(32);
    private final List<String> autocompletedInstructions = new ArrayList<>();
    private EditingInstruction instruction;
    private final Consumer<EditingInstruction> callback;
    private final MethodInput methodInput;

    public AssemblerEditInstructionPopup(Trinity trinity, MethodInput methodInput, Consumer<EditingInstruction> callback) {
        super("Edit/View Instruction", trinity);
        this.callback = Objects.requireNonNull(callback);
        this.methodInput = methodInput;
    }

    @Override
    protected void renderFrame() {
        if (ImGui.inputText("Opcode", opcodeName, ImGuiInputTextFlags.AutoSelectAll)) {
            this.setAutocompletedInstructions();
        }

        if (this.instruction == null) {
            if (ImGui.isItemFocused() && !this.autocompletedInstructions.isEmpty() &&
                    ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.Enter))) {
                this.opcodeName.set(this.autocompletedInstructions.get(0));
                this.setAutocompletedInstructions();
            }
        }

        if (this.instruction == null) {
            ImGui.setKeyboardFocusHere(-1);
            this.renderOpcodeComplete();
        } else {
            for (EditField<?> editField : instruction.getEditFieldList()) {
                editField.draw();
            }
            boolean disabled = !instruction.isValid();
            if (disabled) ImGui.beginDisabled();
            if (ImGui.button("Done")) {
                this.callback.accept(this.instruction);
                this.close();
            }
            if (disabled) ImGui.endDisabled();
            ImGui.sameLine();
            if (ImGui.button("Cancel")) {
                this.close();
            }
        }
    }

    @Override
    public boolean canCloseOnEscapeNow() {
        if (!this.opcodeName.get().isEmpty()) {
            this.opcodeName.set("");
            return false;
        }
        return super.canCloseOnEscapeNow();
    }

    private void renderOpcodeComplete() {
        boolean resetOpcode = false;
        if (!opcodeName.get().isEmpty()) {
            for (String autocomplete : autocompletedInstructions) {
                String text = autocomplete.replace("\n", "");
                ImGui.textColored(CodeColorScheme.KEYWORD, text);
                if (ImGui.isItemHovered() && ImGui.isMouseClicked(0)) {
                    this.opcodeName.set(text);
                    resetOpcode = true;
                }
                if (!autocomplete.endsWith("\n")) {
                    ImGui.sameLine();
                }
            }
        } else {
            ImGui.textDisabled("Enter something to view matching opcodes.\nENTER to select first opcode.\nESCAPE to clear opcode.");
        }
        if (resetOpcode) this.setAutocompletedInstructions();
    }

    private void setAutocompletedInstructions() {
        final String search = this.opcodeName.get().toLowerCase();
        this.autocompletedInstructions.clear();
        this.instruction = null;
        float width = 0.F;
        for (String opcode : OpcodeClasses.getNamesToClasses().keySet()) {
            if (isOpcodeMatche(opcode, search)) {
                width += ImGui.calcTextSize(opcode).x;
                String autocomplete = opcode;
                if (width >= 200) {
                    autocomplete += "\n";
                    width = 0;
                }
                this.autocompletedInstructions.add(autocomplete);
            }
            if (opcode.equalsIgnoreCase(search)) {
                AbstractInsnNode insnNode;
                try {
                    insnNode = (AbstractInsnNode) UnsafeUtil.getUnsafe().allocateInstance(OpcodeClasses.getOpcodeClass(opcode));
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                }
                final int opcodeIndex = OpcodeClasses.getOpcodeIndex(opcode);
                if (opcodeIndex != -1) {
                    OpcodeClasses.setInstructionOpcode(insnNode, opcodeIndex);
                }
                EditingInstruction editingInstruction = new EditingInstruction(insnNode);
                this.addInstructionFields(editingInstruction);
                this.instruction = editingInstruction;
            }
        }
    }

    private void addInstructionFields(EditingInstruction editingInstruction) {
        AbstractInsnNode insnNode = editingInstruction.getInsnNode();
        List<EditField<?>> fields = editingInstruction.getEditFieldList();

        if (insnNode instanceof TypeInsnNode) {
            fields.add(new EditFieldDescriptor((desc) -> ((TypeInsnNode) insnNode).desc = desc));
        } else if (insnNode instanceof IntInsnNode) {
            fields.add(new EditFieldInteger("Operand", (operand) -> ((IntInsnNode) insnNode).operand = operand, ImGuiDataType.S32));
        } else if (insnNode instanceof VarInsnNode) {
            fields.add(new EditFieldVariable(methodInput.getVariableTable(), (variable) -> ((VarInsnNode) insnNode).var = variable.findIndex()));
        } else if (insnNode instanceof IincInsnNode) {
            fields.add(new EditFieldVariable(methodInput.getVariableTable(), (variable) -> ((IincInsnNode) insnNode).var = variable.findIndex()));
            fields.add(new EditFieldInteger("Increment", (incr) -> ((IincInsnNode) insnNode).incr = incr, ImGuiDataType.S32));
        } else if (insnNode instanceof MultiANewArrayInsnNode) {
            fields.add(new EditFieldDescriptor((desc) -> ((MultiANewArrayInsnNode) insnNode).desc = desc));
            fields.add(new EditFieldInteger("Dimensions", (dim) -> ((MultiANewArrayInsnNode) insnNode).dims = dim, ImGuiDataType.U8));
        } else if (insnNode instanceof JumpInsnNode) {
            fields.add(new EditFieldLabel(methodInput.getLabelTable(), (label) -> ((JumpInsnNode) insnNode).label = new LabelNode(label.findOriginal())));
        }

        for (EditField<?> editField : fields) {
            editField.setUpdateEvent(editingInstruction::update);
        }

        editingInstruction.update();
    }

    private boolean isOpcodeMatche(String opcode, String search) {
        opcode = opcode.toLowerCase();
        String[] split = search.toLowerCase().split(" ");
        for (String part : split) {
            if (!opcode.toLowerCase().contains(part)) {
                return false;
            }
        }
        return true;
    }
}
