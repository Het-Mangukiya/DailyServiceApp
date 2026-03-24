package com.dailyserviceapp.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

public class DateUtilsAdditionalTest {

    @Test
    public void getDaysInMonth_handlesLeapAndNonLeapYears() {
        assertEquals(29, DateUtils.getDaysInMonth(Calendar.FEBRUARY, 2024));
        assertEquals(28, DateUtils.getDaysInMonth(Calendar.FEBRUARY, 2025));
        assertEquals(31, DateUtils.getDaysInMonth(Calendar.JANUARY, 2026));
        assertEquals(30, DateUtils.getDaysInMonth(Calendar.APRIL, 2026));
    }

    @Test
    public void addMonths_advancesCalendarCorrectly() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JANUARY, 15, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date shifted = DateUtils.addMonths(cal.getTime(), 2);
        Calendar out = Calendar.getInstance();
        out.setTime(shifted);

        assertEquals(Calendar.MARCH, out.get(Calendar.MONTH));
        assertEquals(2026, out.get(Calendar.YEAR));
    }

    @Test
    public void getMonthName_returnsNonEmptyText() {
        String january = DateUtils.getMonthName(Calendar.JANUARY);
        String december = DateUtils.getMonthName(Calendar.DECEMBER);

        assertTrue(january != null && !january.trim().isEmpty());
        assertTrue(december != null && !december.trim().isEmpty());
    }
}
