package me.f1nal.trinity.gui.windows.impl.assembler.args;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumberArgumentTest {
    @Test
    void removesOnlyTheTrailingDecimalPartFromWholeFloatingValues() {
        assertEquals("1F", NumberArgument.formatNumber(1.0F));
        assertEquals("-2D", NumberArgument.formatNumber(-2.0D));
        assertEquals("-0F", NumberArgument.formatNumber(-0.0F));
    }

    @Test
    void preservesFractionalAndIntegralNumericValues() {
        assertEquals("1.25F", NumberArgument.formatNumber(1.25F));
        assertEquals("-2.5D", NumberArgument.formatNumber(-2.5D));
        assertEquals("2147483648L", NumberArgument.formatNumber(2_147_483_648L));
        assertEquals("42", NumberArgument.formatNumber(42));
    }
}
