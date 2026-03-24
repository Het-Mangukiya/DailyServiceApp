package com.dailyserviceapp.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

public class DateUtilsTest {

    @Test
    public void getStartAndEndOfDay_setExpectedBoundaries() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MARCH, 18, 15, 45, 22);
        cal.set(Calendar.MILLISECOND, 333);
        Date input = cal.getTime();

        Date start = DateUtils.getStartOfDay(input);
        Date end = DateUtils.getEndOfDay(input);

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(start);
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, startCal.get(Calendar.MINUTE));
        assertEquals(0, startCal.get(Calendar.SECOND));
        assertEquals(0, startCal.get(Calendar.MILLISECOND));

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(end);
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, endCal.get(Calendar.MINUTE));
        assertEquals(59, endCal.get(Calendar.SECOND));
        assertEquals(999, endCal.get(Calendar.MILLISECOND));
    }

    @Test
    public void getFirstAndLastDayOfMonth_resolveCorrectDates() {
        Date first = DateUtils.getFirstDayOfMonth(Calendar.FEBRUARY, 2026);
        Date last = DateUtils.getLastDayOfMonth(Calendar.FEBRUARY, 2026);

        Calendar firstCal = Calendar.getInstance();
        firstCal.setTime(first);
        assertEquals(1, firstCal.get(Calendar.DAY_OF_MONTH));

        Calendar lastCal = Calendar.getInstance();
        lastCal.setTime(last);
        assertEquals(28, lastCal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void addDaysAndDifference_returnExpectedValues() {
        Calendar base = Calendar.getInstance();
        base.set(2026, Calendar.JANUARY, 1, 0, 0, 0);
        base.set(Calendar.MILLISECOND, 0);

        Date start = base.getTime();
        Date plusTen = DateUtils.addDays(start, 10);

        assertEquals(10, DateUtils.getDaysDifference(start, plusTen));
    }

    @Test
    public void parseDate_validAndInvalidInputs() {
        Date parsed = DateUtils.parseDate("18 Mar 2026", Constants.DATE_FORMAT_SHORT);
        assertNotNull(parsed);

        Date invalid = DateUtils.parseDate("not-a-date", Constants.DATE_FORMAT_SHORT);
        assertEquals(null, invalid);
    }

    @Test
    public void isSameDay_andIsToday_behaveAsExpected() {
        Date now = new Date();
        Date plusOneHour = new Date(now.getTime() + 60L * 60L * 1000L);
        Date tomorrow = new Date(now.getTime() + 24L * 60L * 60L * 1000L);

        assertTrue(DateUtils.isSameDay(now, plusOneHour));
        assertFalse(DateUtils.isSameDay(now, tomorrow));
        assertTrue(DateUtils.isToday(now));
        assertFalse(DateUtils.isToday(tomorrow));
    }
}
