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
}
