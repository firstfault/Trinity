package me.f1nal.trinity.gui.windows.impl.assembler.args;

import me.f1nal.trinity.decompiler.output.colors.SupplierColoredString;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.labels.MethodLabel;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerFrame;
import me.f1nal.trinity.gui.windows.impl.assembler.fields.LabelRefsField;
import me.f1nal.trinity.gui.windows.impl.assembler.fields.TextField;
import me.f1nal.trinity.theme.CodeColorScheme;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.function.BiConsumer;

public class LabelArgument extends InstructionOperand {
    private final AssemblerFrame assemblerFrame;
    private final MethodLabel label;
    private final LabelNode labelNode;
    private final MethodInput methodInput;
    private final BiConsumer<AbstractInsnNode, LabelNode> updateLabel;

    public LabelArgument(AssemblerFrame assemblerFrame, MethodInput methodInput, LabelNode label, BiConsumer<AbstractInsnNode, LabelNode> updateLabel) {
        this.assemblerFrame = assemblerFrame;
        this.labelNode = label;
        this.methodInput = methodInput;
        this.updateLabel = updateLabel;
        final MethodLabel methodLabel = methodInput.getLabelTable().getLabel(label.getLabel());
        this.getDetailsText().add(new SupplierColoredString(() -> "." + methodLabel.getName(), CodeColorScheme.LABEL));
        this.getFields().add(new TextField("Label Name", methodLabel.getNameProperty()));
        this.getFields().add(new LabelRefsField(assemblerFrame, methodLabel));
        this.label = methodLabel;
    }

    public BiConsumer<AbstractInsnNode, LabelNode> getUpdateLabel() {
        return updateLabel;
    }

    public MethodLabel getLabel() {
        return label;
    }

    public LabelNode getLabelNode() {
        return labelNode;
    }

    @Override
    public InstructionOperand copy() {
        return new LabelArgument(this.assemblerFrame, this.methodInput, this.labelNode, this.updateLabel);
    }
}
