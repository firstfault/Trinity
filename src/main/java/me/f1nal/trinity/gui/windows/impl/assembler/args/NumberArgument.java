package me.f1nal.trinity.gui.windows.impl.assembler.args;

import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.theme.CodeColorScheme;

public class NumberArgument extends InstructionOperand {
    private final Number number;

    public NumberArgument(Number number) {
        this.number = number;
        this.getDetailsText().add(new ColoredString(number + getPrefix(number), CodeColorScheme.NUMBER));
    }

    public static String getPrefix(Number number) {
        if (number instanceof Float) {
            return "F";
        }
        if (number instanceof Double) {
            return "D";
        }
        if (number instanceof Long) {
            return "L";
        }
        return "";
    }

    @Override
    public InstructionOperand copy() {
        return new NumberArgument(this.number);
    }
}
