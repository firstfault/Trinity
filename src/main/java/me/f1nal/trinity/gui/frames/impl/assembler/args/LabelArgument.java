package me.f1nal.trinity.gui.frames.impl.assembler.args;

import me.f1nal.trinity.decompiler.output.colors.SupplierColoredString;
import me.f1nal.trinity.execution.labels.MethodLabel;
import me.f1nal.trinity.gui.frames.impl.assembler.AssemblerFrame;
import me.f1nal.trinity.gui.frames.impl.assembler.fields.LabelRefsField;
import me.f1nal.trinity.gui.frames.impl.assembler.fields.TextField;
import me.f1nal.trinity.theme.CodeColorScheme;

public class LabelArgument extends AbstractInsnArgument {
    private final AssemblerFrame assemblerFrame;
    private final MethodLabel label;

    public LabelArgument(AssemblerFrame assemblerFrame, MethodLabel label, String suffix) {
        this.assemblerFrame = assemblerFrame;
        this.label = label;
        this.getDetailsText().add(new SupplierColoredString(() -> suffix + label.getNameProperty().get(), CodeColorScheme.LABEL));
        this.getFields().add(new TextField("Label Name", label.getNameProperty()));
        this.getFields().add(new LabelRefsField(assemblerFrame, label));
    }

    public LabelArgument(AssemblerFrame assemblerFrame, MethodLabel label) {
        this(assemblerFrame, label, ".");
    }

    @Override
    public AbstractInsnArgument copy() {
        return new LabelArgument(this.assemblerFrame, this.label);
    }
}
