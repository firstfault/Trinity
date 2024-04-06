package me.f1nal.trinity.decompiler.output.component.number;

public class NumberDisplayTypeHex extends NumberDisplayType {
    @Override
    public String getTextImpl(Number number) {
        String hexString;
        if (number instanceof Double) {
            hexString = Double.toHexString(number.doubleValue());
        } else if (number instanceof Float) {
            hexString = Float.toHexString(number.floatValue());
        } else {
            hexString = Long.toHexString(number.longValue());
        }
        return "0x" + hexString.toUpperCase();
    }

    @Override
    public String getLabel() {
        return "Hex";
    }
}
