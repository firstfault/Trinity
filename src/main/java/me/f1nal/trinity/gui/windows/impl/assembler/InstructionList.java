package me.f1nal.trinity.gui.windows.impl.assembler;

import me.f1nal.trinity.execution.labels.MethodLabel;
import me.f1nal.trinity.gui.windows.impl.assembler.line.InstructionReferenceArrow;
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

    public InstructionReferenceArrow getReferenceArrow(MethodLabel label) {
        return instructionReferenceArrowList.stream().filter(instructionReferenceArrow -> instructionReferenceArrow.getLabel().equals(label)).findFirst().orElse(null);
    }

    public List<InstructionReferenceArrow> getInstructionReferenceArrowList() {
        return instructionReferenceArrowList;
    }

    public int indexOfInsn(AbstractInsnNode insn) {
        for (int i = 0; i < this.size(); i++) {
            if (this.get(i).getInstruction() == insn) {
                return i;
            }
        }
        return -1;
    }

    public void removeReferenceArrowsFrom(InstructionComponent instruction) {
        instructionReferenceArrowList.removeIf(r -> r.getFrom() == instruction);
    }
}
