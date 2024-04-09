package me.f1nal.trinity.gui.windows.impl.assembler.args;

import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.theme.CodeColorScheme;

public class StringArgument extends AbstractInsnArgument {
    private final String string;

    public StringArgument(String string) {
        this.string = string;
        this.getDetailsText().add(new ColoredString(String.format("\"%s\"", string), CodeColorScheme.STRING));
    }

    public String getString() {
        return string;
    }

    @Override
    public AbstractInsnArgument copy() {
        return new StringArgument(string);
    }
}
