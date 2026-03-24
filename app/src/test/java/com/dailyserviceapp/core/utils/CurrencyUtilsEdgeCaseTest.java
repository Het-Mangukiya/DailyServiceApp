package com.dailyserviceapp.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CurrencyUtilsEdgeCaseTest {

    private static final double EPSILON = 0.0001;

    @Test
    public void formatCompactCurrency_thresholds_areCorrect() {
        String belowThousand = CurrencyUtils.formatCompactCurrency(999.99);
        String thousand = CurrencyUtils.formatCompactCurrency(1000.0);
        String lakh = CurrencyUtils.formatCompactCurrency(100000.0);

        assertTrue(belowThousand.contains("₹"));
        assertFalse(belowThousand.contains("K"));
        assertTrue(thousand.contains("K"));
        assertTrue(lakh.contains("L"));
    }

    @Test
    public void parseCurrency_handlesSymbolCommaAndWhitespace() {
        assertEquals(123456.78, CurrencyUtils.parseCurrency("  ₹1,23,456.78  "), EPSILON);
        assertEquals(0.0, CurrencyUtils.parseCurrency(""), EPSILON);
    }

    @Test
    public void discountAndTax_helpersKeepTwoDecimals() {
        assertEquals(12.35, CurrencyUtils.calculateDiscount(123.45, 10.0), EPSILON);
        assertEquals(111.10, CurrencyUtils.applyDiscount(123.45, 10.0), EPSILON);
        assertEquals(148.14, CurrencyUtils.addTax(123.45, 20.0), EPSILON);
    }

    @Test
    public void amountValidation_nonNegativeOnly() {
        assertTrue(CurrencyUtils.isValidAmount(0.0));
        assertTrue(CurrencyUtils.isValidAmount(1.0));
        assertFalse(CurrencyUtils.isValidAmount(-0.01));
    }
}
