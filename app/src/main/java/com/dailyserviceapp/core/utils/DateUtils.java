package com.dailyserviceapp.core.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for date and time operations.
 * Provides methods for date formatting, parsing, and manipulation
 * using predefined date formats from Constants.
 * 
 * <p>Supports multiple date formats: full (with time), short (date only),
 * and month-year display. All methods are static and thread-safe.</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public class DateUtils {
    
    /** SimpleDateFormat for full date-time display (dd MMM yyyy, hh:mm a) */
    private static final SimpleDateFormat fullFormat = new SimpleDateFormat(Constants.DATE_FORMAT_FULL, Locale.getDefault());
    
    /** SimpleDateFormat for short date display (dd MMM yyyy) */
    private static final SimpleDateFormat shortFormat = new SimpleDateFormat(Constants.DATE_FORMAT_SHORT, Locale.getDefault());
    
    /** SimpleDateFormat for month and year display (MMM yyyy) */
    private static final SimpleDateFormat monthYearFormat = new SimpleDateFormat(Constants.DATE_FORMAT_MONTH_YEAR, Locale.getDefault());
    
    /**
     * Gets the current date and time.
     * 
     * @return Current Date object
     */
    public static Date getCurrentDate() {
        return new Date();
    }
    
    /**
     * Formats a date to full format with date and time.
     * Example output: "08 Jan 2026, 02:30 PM"
     * 
     * @param date The date to format, or null
     * @return Formatted date string, or empty string if date is null
     */
    public static String formatFullDate(Date date) {
        if (date == null) return "";
        return fullFormat.format(date);
    }
    
    /**
     * Formats a date to short format with date only.
     * Example output: "08 Jan 2026"
     * 
     * @param date The date to format, or null
     * @return Formatted date string, or empty string if date is null
     */
    public static String formatShortDate(Date date) {
        if (date == null) return "";
        return shortFormat.format(date);
    }
    
    /**
     * Formats a date to show only month and year.
     * Example output: "Jan 2026"
     * 
     * @param date The date to format, or null
     * @return Formatted month-year string, or empty string if date is null
     */
    public static String formatMonthYear(Date date) {
        if (date == null) return "";
        return monthYearFormat.format(date);
    }
    
    /**
     * Parses a date string using the specified format.
     * Returns null if parsing fails or input is invalid.
     * 
     * @param dateString The date string to parse
     * @param format The expected date format pattern
     * @return Parsed Date object, or null if parsing fails
     */
    public static Date parseDate(String dateString, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
            return sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Gets the start of day (00:00:00.000) for a given date.
     * 
     * @param date The date to process
     * @return New Date set to midnight (start of day)
     */
    public static Date getStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
    
    /**
     * Gets the end of day (23:59:59.999) for a given date.
     * 
     * @param date The date to process
     * @return New Date set to last millisecond of the day
     */
    public static Date getEndOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }
    
    /**
     * Get first day of month
     */
    public static Date getFirstDayOfMonth(int month, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return getStartOfDay(calendar.getTime());
    }
    
    /**
     * Get last day of month
     */
    public static Date getLastDayOfMonth(int month, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        return getEndOfDay(calendar.getTime());
    }
    
    /**
     * Get current month (0-based)
     */
    public static int getCurrentMonth() {
        return Calendar.getInstance().get(Calendar.MONTH);
    }
    
    /**
     * Get current year
     */
    public static int getCurrentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }
    
    /**
     * Get number of days in month
     */
    public static int getDaysInMonth(int month, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
    
    /**
     * Check if two dates are on the same day
     */
    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) return false;
        
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }
    
    /**
     * Check if date is today
     */
    public static boolean isToday(Date date) {
        return isSameDay(date, new Date());
    }
    
    /**
     * Add days to date
     */
    public static Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }
    
    /**
     * Add months to date
     */
    public static Date addMonths(Date date, int months) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, months);
        return calendar.getTime();
    }
    
    /**
     * Get days difference between two dates
     */
    public static long getDaysDifference(Date date1, Date date2) {
        long diff = date2.getTime() - date1.getTime();
        return diff / (24 * 60 * 60 * 1000);
    }
    
    /**
     * Get month name from month number (0-based)
     */
    public static String getMonthName(int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, month);
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
        return monthFormat.format(calendar.getTime());
    }
    
    private DateUtils() {
        // Private constructor to prevent instantiation
    }
}
