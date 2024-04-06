package me.f1nal.trinity.decompiler.output.component.number;

public class NumberDisplayTypeDecimal extends NumberDisplayType {
    @Override
    public String getTextImpl(Number number) {
        return Long.toString(number.longValue());
    }

    @Override
    public String getLabel() {
        return "Decimal";
    }
}
