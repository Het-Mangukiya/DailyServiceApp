package com.dailyserviceapp.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CurrencyUtilsTest {

    private static final double EPSILON = 0.0001;

    @Test
    public void parseCurrency_parsesRupeeAndCommaFormats() {
        assertEquals(1234.5, CurrencyUtils.parseCurrency("₹1,234.50"), EPSILON);
        assertEquals(750.0, CurrencyUtils.parseCurrency("750"), EPSILON);
    }

    @Test
    public void parseCurrency_invalidInputReturnsZero() {
        assertEquals(0.0, CurrencyUtils.parseCurrency(null), EPSILON);
        assertEquals(0.0, CurrencyUtils.parseCurrency("not-a-number"), EPSILON);
    }

    @Test
    public void roundAndTaxHelpers_returnExpectedValues() {
        assertEquals(10.24, CurrencyUtils.roundAmount(10.235), EPSILON);
        assertEquals(12.0, CurrencyUtils.calculatePercentage(200.0, 6.0), EPSILON);
        assertEquals(224.0, CurrencyUtils.addTax(200.0, 12.0), EPSILON);
        assertEquals(180.0, CurrencyUtils.applyDiscount(200.0, 10.0), EPSILON);
    }

    @Test
    public void formatCompactCurrency_usesKAndLNotation() {
        String thousand = CurrencyUtils.formatCompactCurrency(2_500.0);
        String lakh = CurrencyUtils.formatCompactCurrency(150_000.0);
        String small = CurrencyUtils.formatCompactCurrency(500.0);

        assertTrue(thousand.contains("K"));
        assertTrue(lakh.contains("L"));
        assertTrue(small.contains("₹"));
    }
}
