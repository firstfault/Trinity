package me.f1nal.trinity.gui.windows.impl.assembler.args;

import me.f1nal.trinity.decompiler.output.colors.SupplierColoredString;
import me.f1nal.trinity.execution.labels.MethodLabel;
import me.f1nal.trinity.theme.CodeColorScheme;

public final class LabelNameArgument extends InstructionOperand {
    private final MethodLabel label;

    public LabelNameArgument(MethodLabel label) {
        this.label = label;
        getDetailsText().add(new SupplierColoredString(() -> "." + label.getName(), CodeColorScheme.LABEL));
    }

    @Override
    public InstructionOperand copy() {
        return new LabelNameArgument(label);
    }
}
