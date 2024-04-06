package me.f1nal.trinity.gui.frames.impl.assembler.popup.edit;

import org.objectweb.asm.tree.AbstractInsnNode;

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

    List<EditField<?>> getEditFieldList() {
        return editFieldList;
    }

    public void update() {
        this.valid = this.computeValid();
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
