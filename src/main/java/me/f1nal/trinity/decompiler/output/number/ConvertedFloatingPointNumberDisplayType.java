package me.f1nal.trinity.decompiler.output.number;

public abstract class ConvertedFloatingPointNumberDisplayType extends NumberDisplayType {
    @Override
    protected String getTextImpl(Number number) {
        if (number instanceof Float) {
            number = Float.floatToIntBits(number.floatValue());
        } else if (number instanceof Double) {
            number = Double.doubleToLongBits(number.doubleValue());
        }
        return getText(number);
    }
}
