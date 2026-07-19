package me.f1nal.trinity.gui.windows.impl.assembler.args;

import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.gui.windows.impl.assembler.popup.edit.AssemblerValueCodec;
import me.f1nal.trinity.theme.CodeColorScheme;

public final class AsmValueArgument extends InstructionOperand {
    private final Object value;

    public AsmValueArgument(Object value) {
        this.value = value;
        getDetailsText().add(new ColoredString(AssemblerValueCodec.format(value), CodeColorScheme.TEXT));
    }

    @Override
    public InstructionOperand copy() {
        return new AsmValueArgument(value);
    }
}
