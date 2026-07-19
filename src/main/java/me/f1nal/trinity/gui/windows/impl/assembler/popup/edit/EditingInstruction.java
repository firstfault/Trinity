package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import imgui.flag.ImGuiDataType;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.MethodInput;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.f1nal.trinity.execution.labels.MethodLabel;
import org.objectweb.asm.Type;

public class EditingInstruction {
    private final AbstractInsnNode insnNode;
    private final List<EditField<?>> editFieldList = new ArrayList<>();
    private final Trinity trinity;
    private final Map<LabelNode, LabelNode> labelMap;
    /**
     * If this instruction data is valid and can be set.
     */
    private boolean valid;

    public EditingInstruction(Trinity trinity, Map<LabelNode, LabelNode> labelMap, AbstractInsnNode insnNode) {
        this(trinity, labelMap, insnNode, true);
    }

    public EditingInstruction(Trinity trinity, Map<LabelNode, LabelNode> labelMap, AbstractInsnNode insnNode, boolean clone) {
        this.trinity = trinity;
        this.labelMap = labelMap;
        this.insnNode = clone ? insnNode.clone(labelMap) : insnNode;
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
            editFieldList.add(new EditFieldLabel(methodInput.getLabelTable(), () -> methodInput.getLabelTable().getLabel(((JumpInsnNode) insnNode).label.getLabel()),
                    (label) -> ((JumpInsnNode) insnNode).label = resolveLabel(label)));
        } else if(insnNode instanceof MethodInsnNode min) {
            editFieldList.add(new EditFieldClass(trinity, "Owner", () -> min.owner, owner -> min.owner = owner));
            editFieldList.add(new EditFieldString(512, "Name", "Method name", () -> min.name, name -> min.name = name));
            editFieldList.add(new EditFieldString(512, "Desc", "Method description", () -> min.desc, desc -> min.desc = desc));
            editFieldList.add(new EditFieldBoolean("Interface owner", () -> min.itf, value -> min.itf = value));
        } else if(insnNode instanceof FieldInsnNode fin) {
            editFieldList.add(new EditFieldClass(trinity, "Field owner", () -> fin.owner, owner -> fin.owner = owner));
            editFieldList.add(new EditFieldString(512, "Name", "Field name", () -> fin.name, name -> fin.name = name));
            editFieldList.add(new EditFieldString(512, "Desc", "Field description", () -> fin.desc, desc -> fin.desc = desc));
        } else if (insnNode instanceof LdcInsnNode ldc) {
            editFieldList.add(new EditFieldAsmConstant("Constant", () -> ldc.cst, value -> ldc.cst = value));
        } else if (insnNode instanceof InvokeDynamicInsnNode dynamic) {
            editFieldList.add(new EditFieldString(512, "Name", "Call site name", () -> dynamic.name, value -> dynamic.name = value));
            editFieldList.add(new EditFieldString(1024, "Descriptor", "()Ljava/lang/Object;", () -> dynamic.desc, value -> dynamic.desc = value));
            editFieldList.add(new EditFieldAsmHandle("Bootstrap method", () -> dynamic.bsm, value -> dynamic.bsm = value));
            editFieldList.add(new EditFieldAsmConstantList("Bootstrap arguments", () -> dynamic.bsmArgs, value -> dynamic.bsmArgs = value));
        } else if (insnNode instanceof LookupSwitchInsnNode lookup) {
            editFieldList.add(new EditFieldSwitch(lookup, methodInput.getLabelTable(), this::resolveLabel));
        } else if (insnNode instanceof TableSwitchInsnNode table) {
            editFieldList.add(new EditFieldSwitch(table, methodInput.getLabelTable(), this::resolveLabel));
        } else if (insnNode instanceof LineNumberNode line) {
            editFieldList.add(new EditFieldInteger("Line", () -> line.line, value -> line.line = value, ImGuiDataType.S32));
            editFieldList.add(new EditFieldLabel(methodInput.getLabelTable(),
                    () -> methodInput.getLabelTable().getLabel(line.start.getLabel()), label -> line.start = resolveLabel(label)));
        } else if (insnNode instanceof FrameNode frame) {
            editFieldList.add(new EditFieldFrame(frame, methodInput.getLabelTable(), this::resolveLabel));
        } else if (insnNode instanceof LabelNode labelNode) {
            MethodLabel label = methodInput.getLabelTable().getLabel(labelNode.getLabel());
            editFieldList.add(new EditFieldString(128, "Label name", "L0", label::getName,
                    value -> label.getNameProperty().set(value)));
        }

//        editFieldList.add(new EditFieldTypeAnnotations("Visible instruction type annotations",
//                () -> insnNode.visibleTypeAnnotations, value -> insnNode.visibleTypeAnnotations = value));
//        editFieldList.add(new EditFieldTypeAnnotations("Invisible instruction type annotations",
//                () -> insnNode.invisibleTypeAnnotations, value -> insnNode.invisibleTypeAnnotations = value));

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
        if (insnNode instanceof LookupSwitchInsnNode lookup && lookup.keys.size() != lookup.labels.size()) return false;
        if (insnNode instanceof TableSwitchInsnNode table
                && (table.max < table.min || table.labels.size() != table.max - table.min + 1)) return false;
        if (insnNode instanceof MultiANewArrayInsnNode multi) {
            try {
                Type type = Type.getType(multi.desc);
                if (type.getSort() != Type.ARRAY || multi.dims < 1 || multi.dims > type.getDimensions()) return false;
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
        return true;
    }

    private LabelNode resolveLabel(MethodLabel label) {
        if (label == null) return null;
        for (LabelNode node : labelMap.keySet()) {
            if (node.getLabel() == label.findOriginal()) return node;
        }
        if (insnNode instanceof JumpInsnNode jump && jump.label.getLabel() == label.findOriginal()) return jump.label;
        if (insnNode instanceof LineNumberNode line && line.start.getLabel() == label.findOriginal()) return line.start;
        if (insnNode instanceof TableSwitchInsnNode table) {
            if (table.dflt.getLabel() == label.findOriginal()) return table.dflt;
            for (LabelNode node : table.labels) if (node.getLabel() == label.findOriginal()) return node;
        }
        if (insnNode instanceof LookupSwitchInsnNode lookup) {
            if (lookup.dflt.getLabel() == label.findOriginal()) return lookup.dflt;
            for (LabelNode node : lookup.labels) if (node.getLabel() == label.findOriginal()) return node;
        }
        return null;
    }
}
