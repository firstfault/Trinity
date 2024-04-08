package me.f1nal.trinity.decompiler.output.number;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.frames.impl.constant.search.ConstantSearchType;
import me.f1nal.trinity.gui.frames.impl.constant.search.ConstantSearchTypeNumber;

import java.util.ArrayList;

public abstract class NumberDisplayType {
    public String getText(Number number) {
        return getTextImpl(number);
    }
    protected abstract String getTextImpl(Number number);
    public abstract String getLabel();

    public ConstantSearchType getConstantSearchType(Trinity trinity, Number number) {
        if (number instanceof Float) {
            return new ConstantSearchTypeNumber.ConstantSearchTypeFloat(trinity, number.floatValue());
        }
        if (number instanceof Double) {
            return new ConstantSearchTypeNumber.ConstantSearchTypeDouble(trinity, number.doubleValue());
        }
        if (number instanceof Long) {
            return new ConstantSearchTypeNumber.ConstantSearchTypeLong(trinity, number.longValue());
        }
        return new ConstantSearchTypeNumber.ConstantSearchTypeInteger(trinity, number.intValue());
    }
}
