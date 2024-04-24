package me.f1nal.trinity.gui.windows.impl.assembler.args;

import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.theme.CodeColorScheme;

public class NumberArgument extends InstructionOperand {
    private final Number number;

    public NumberArgument(Number number) {
        this.number = number;
        this.getDetailsText().add(new ColoredString(formatNumber(number), CodeColorScheme.NUMBER));
    }

    private static String formatNumber(Number number) {
        final String suffix = getSuffix(number);
        String text = String.valueOf(number);

        if (!suffix.isEmpty()) {
            if (text.endsWith(".0")) {
                text = text.substring(text.length() - 1);
            } else if (!(number instanceof Long) && !text.contains(".")) {
                text += ".";
            }
        }

        return text + suffix;
    }

    public static String getSuffix(Number number) {
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
