package me.f1nal.trinity.decompiler.output.component.number;

public class NumberDisplayTypeBinary extends ConvertedFloatingPointNumberDisplayType {
    @Override
    public String getTextImpl(Number number) {
        return "0b" + Long.toBinaryString(number.longValue());
    }

    @Override
    public String getLabel() {
        return "Binary";
    }
}
