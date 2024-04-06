package me.f1nal.trinity.gui.frames.impl.assembler;

import me.f1nal.trinity.gui.frames.impl.assembler.line.InstructionReferenceArrow;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.ArrayList;
import java.util.List;

public class InstructionList extends ArrayList<InstructionComponent> {
    private final List<InstructionReferenceArrow> instructionReferenceArrowList = new ArrayList<>();
    private int maximumReferenceArrowDepth;

    private boolean resetIds = true;

    public InstructionList(List<InstructionComponent> instructions) {
        super(instructions);
        if (instructions instanceof InstructionList) this.instructionReferenceArrowList.addAll(((InstructionList) instructions).getInstructionReferenceArrowList());
    }

    public void setMaximumReferenceArrowDepth(int maximumReferenceArrowDepth) {
        this.maximumReferenceArrowDepth = maximumReferenceArrowDepth;
    }

    public int getMaximumReferenceArrowDepth() {
        return maximumReferenceArrowDepth;
    }

    public InstructionList() {
    }

    public void queueIdReset() {
        this.resetIds = true;
    }

    @Override
    public boolean add(InstructionComponent instructionComponent) {
        if (contains(instructionComponent)) {
            throw new RuntimeException("Duplicate component in instruction list");
        }
        return super.add(instructionComponent);
    }

    public boolean setIdsIfReset() {
        if (this.resetIds) {
            this.resetIds = false;
            this.setInstructionIds();
            return true;
        }
        return false;
    }

    private void setInstructionIds() {
        for (int i = 0; i < size(); i++) {
            get(i).setId(i);
        }
    }

    public List<InstructionReferenceArrow> getInstructionReferenceArrowList() {
        return instructionReferenceArrowList;
    }

    public int indexOfInsn(AbstractInsnNode insn) {
        int index = 0;
        for (InstructionComponent ic : this) {
            if (ic.getInstruction() == insn) {
                return index;
            }
            ++index;
        }
        return -1;
    }
}
