package me.f1nal.trinity.gui.frames.impl.assembler.popup.edit;

import imgui.flag.ImGuiDataType;
import me.f1nal.trinity.execution.MethodInput;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

public class EditingInstruction {
    private final AbstractInsnNode insnNode;
    private final List<EditField<?>> editFieldList = new ArrayList<>();
    /**
     * If this instruction data is valid and can be set.
     */
    private boolean valid;

    public EditingInstruction(AbstractInsnNode insnNode) {
        this.insnNode = insnNode;
    }

    public AbstractInsnNode getInsnNode() {
        return insnNode;
    }

    public boolean isValid() {
        return valid;
    }

    public List<EditField<?>> getEditFieldList() {
        return editFieldList;
    }

    public void update() {
        this.valid = this.computeValid();
    }

    public void addInstructionFields(MethodInput methodInput) {
        if (insnNode instanceof TypeInsnNode) {
            editFieldList.add(new EditFieldDescriptor(() -> ((TypeInsnNode) insnNode).desc, (desc) -> ((TypeInsnNode) insnNode).desc = desc));
        } else if (insnNode instanceof IntInsnNode) {
            editFieldList.add(new EditFieldInteger("Operand", () -> ((IntInsnNode) insnNode).operand, (operand) -> ((IntInsnNode) insnNode).operand = operand, ImGuiDataType.S32));
        } else if (insnNode instanceof VarInsnNode) {
            editFieldList.add(new EditFieldVariable(methodInput.getVariableTable(), () -> methodInput.getVariableTable().getVariable(((VarInsnNode) insnNode).var), (variable) -> ((VarInsnNode) insnNode).var = variable.findIndex()));
        } else if (insnNode instanceof IincInsnNode) {
            editFieldList.add(new EditFieldVariable(methodInput.getVariableTable(), () -> methodInput.getVariableTable().getVariable(((IincInsnNode) insnNode).var), (variable) -> ((IincInsnNode) insnNode).var = variable.findIndex()));
            editFieldList.add(new EditFieldInteger("Increment", () -> ((IincInsnNode) insnNode).incr, (incr) -> ((IincInsnNode) insnNode).incr = incr, ImGuiDataType.S32));
        } else if (insnNode instanceof MultiANewArrayInsnNode) {
            editFieldList.add(new EditFieldDescriptor(() -> ((MultiANewArrayInsnNode) insnNode).desc, (desc) -> ((MultiANewArrayInsnNode) insnNode).desc = desc));
            editFieldList.add(new EditFieldInteger("Dimensions", () -> ((MultiANewArrayInsnNode) insnNode).dims, (dim) -> ((MultiANewArrayInsnNode) insnNode).dims = dim, ImGuiDataType.U8));
        } else if (insnNode instanceof JumpInsnNode) {
            editFieldList.add(new EditFieldLabel(methodInput.getLabelTable(), () -> methodInput.getLabelTable().getLabel(((JumpInsnNode) insnNode).label.getLabel()), (label) -> ((JumpInsnNode) insnNode).label = new LabelNode(label.findOriginal())));
        }

        for (EditField<?> editField : editFieldList) {
            editField.setUpdateEvent(this::update);
        }

        this.update();
    }

    private boolean computeValid() {
        for (EditField<?> editField : editFieldList) {
            if (!editField.isValidInput()) {
                return false;
            }
        }
        return true;
    }
}
