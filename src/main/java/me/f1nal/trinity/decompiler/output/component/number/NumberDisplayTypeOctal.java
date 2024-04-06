package me.f1nal.trinity.decompiler.output.component.number;

public class NumberDisplayTypeOctal extends ConvertedFloatingPointNumberDisplayType {
    @Override
    public String getTextImpl(Number number) {
        return "0" + Long.toOctalString(number.longValue());
    }

    @Override
    public String getLabel() {
        return "Octal";
    }
}
