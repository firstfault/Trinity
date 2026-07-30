package me.f1nal.trinity.gui.windows.impl.constant.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConstantSearchTypeNumberTest {
    @Test
    void longSearchPreservesValuesOutsideTheIntegerRange() {
        long expected = (long) Integer.MAX_VALUE + 42L;
        ConstantSearchTypeNumber.ConstantSearchTypeLong search =
                new ConstantSearchTypeNumber.ConstantSearchTypeLong(null, expected);

        assertEquals(expected + "L", search.getSearchDescription());
        assertEquals(expected + "L", search.convertConstantToText(expected));
        assertNull(search.convertConstantToText((long) (int) expected));
    }

    @Test
    void longSearchPreservesNegativeSixtyFourBitValues() {
        ConstantSearchTypeNumber.ConstantSearchTypeLong search =
                new ConstantSearchTypeNumber.ConstantSearchTypeLong(null, Long.MIN_VALUE);

        assertEquals(Long.MIN_VALUE + "L", search.convertConstantToText(Long.MIN_VALUE));
        assertNull(search.convertConstantToText(-1L));
    }

    @Test
    void longSearchDoesNotMatchOtherNumericTypes() {
        ConstantSearchTypeNumber.ConstantSearchTypeLong search =
                new ConstantSearchTypeNumber.ConstantSearchTypeLong(null, 7L);

        assertNull(search.convertConstantToText(7));
    }

    @Test
    void anyNumericTypePreservesLongValuesOutsideTheIntegerRange() {
        long expected = (long) Integer.MAX_VALUE + 42L;
        ConstantSearchTypeNumber.ConstantSearchTypeDecimal search =
                new ConstantSearchTypeNumber.ConstantSearchTypeDecimal(null, expected);

        assertEquals(expected + "L", search.convertConstantToText(expected));
        assertNull(search.convertConstantToText((int) expected));
    }

    @Test
    void anyNumericTypeRejectsFractionalValuesInsteadOfTruncatingThem() {
        ConstantSearchTypeNumber.ConstantSearchTypeDecimal search =
                new ConstantSearchTypeNumber.ConstantSearchTypeDecimal(null, 7L);

        assertEquals("7", search.convertConstantToText(7));
        assertEquals("7L", search.convertConstantToText(7L));
        assertEquals("7F", search.convertConstantToText(7.0F));
        assertEquals("7D", search.convertConstantToText(7.0D));
        assertNull(search.convertConstantToText(7.25F));
        assertNull(search.convertConstantToText(7.75D));
    }

    @Test
    void anyNumericTypeDoesNotRoundLargeIntegersThroughFloatingPoint() {
        long expected = 9_007_199_254_740_993L;
        ConstantSearchTypeNumber.ConstantSearchTypeDecimal search =
                new ConstantSearchTypeNumber.ConstantSearchTypeDecimal(null, expected);

        assertEquals(expected + "L", search.convertConstantToText(expected));
        assertNull(search.convertConstantToText((double) expected));
        assertNull(search.convertConstantToText((float) expected));
    }

    @Test
    void anyNumericTypeRejectsNonFiniteFloatingValues() {
        ConstantSearchTypeNumber.ConstantSearchTypeDecimal search =
                new ConstantSearchTypeNumber.ConstantSearchTypeDecimal(null, 0L);

        assertNull(search.convertConstantToText(Double.NaN));
        assertNull(search.convertConstantToText(Double.POSITIVE_INFINITY));
        assertNull(search.convertConstantToText(Float.NEGATIVE_INFINITY));
    }
}
