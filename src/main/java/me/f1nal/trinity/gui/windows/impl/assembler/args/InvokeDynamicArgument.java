package me.f1nal.trinity.gui.windows.impl.assembler.args;

import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.theme.CodeColorScheme;

public class InvokeDynamicArgument extends InstructionOperand {
    private final String name, desc;
    public InvokeDynamicArgument(String name, String desc) {
        this.name = name;
        this.desc = desc;
        getDetailsText().add(new ColoredString(name, CodeColorScheme.METHOD_REF));
        getDetailsText().add(new ColoredString("#", CodeColorScheme.DISABLED));
        getDetailsText().add(new ColoredString(desc, CodeColorScheme.METHOD_REF));
    }

    @Override
    public InstructionOperand copy() {
        return new InvokeDynamicArgument(this.name, this.desc);
    }
}
