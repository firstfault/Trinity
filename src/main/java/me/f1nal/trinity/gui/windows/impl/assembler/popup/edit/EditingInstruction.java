package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import imgui.flag.ImGuiDataType;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.MethodInput;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EditingInstruction {
    private final AbstractInsnNode insnNode;
    private final List<EditField<?>> editFieldList = new ArrayList<>();
    private final Trinity trinity;
    /**
     * If this instruction data is valid and can be set.
     */
    private boolean valid;

    public EditingInstruction(Trinity trinity, Map<LabelNode, LabelNode> labelMap, AbstractInsnNode insnNode) {
        this.trinity = trinity;
        this.insnNode = insnNode.clone(labelMap);
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
        } else if(insnNode instanceof MethodInsnNode min) {
            editFieldList.add(new EditFieldClass(trinity, "Owner", () -> min.owner, owner -> min.owner = owner));
            editFieldList.add(new EditFieldString(512, "Name", "Method name", () -> min.name, name -> min.name = name));
            editFieldList.add(new EditFieldString(512, "Desc", "Method description", () -> min.desc, desc -> min.desc = desc));
        } else if(insnNode instanceof FieldInsnNode fin) {
            editFieldList.add(new EditFieldClass(trinity, "Field owner", () -> fin.owner, owner -> fin.owner = owner));
            editFieldList.add(new EditFieldString(512, "Name", "Field name", () -> fin.name, name -> fin.name = name));
            editFieldList.add(new EditFieldString(512, "Desc", "Field description", () -> fin.desc, desc -> fin.desc = desc));
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
