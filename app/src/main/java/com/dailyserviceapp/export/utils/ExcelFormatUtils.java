package com.dailyserviceapp.export.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for formatting Excel cells and handling data conversions.
 */
public class ExcelFormatUtils {

    private static final Locale LOCALE_IN = Locale.forLanguageTag("en-IN");
    private static final String EMPTY_CELL = "";
    private static final String DATE_PATTERN = "dd MMM yyyy";
    private static final String DATE_TIME_PATTERN = "dd MMM yyyy, hh:mm a";

    private ExcelFormatUtils() {
        // Utility class
    }

    /**
     * Converts date to a user-friendly Excel date cell value.
     */
    @NonNull
    public static String toDateCellValue(@Nullable Date date) {
        if (date == null) {
            return EMPTY_CELL;
        }
        return new SimpleDateFormat(DATE_PATTERN, LOCALE_IN).format(date);
    }

    /**
     * Converts date to date-time formatted Excel cell value.
     */
    @NonNull
    public static String toDateTimeCellValue(@Nullable Date date) {
        if (date == null) {
            return EMPTY_CELL;
        }
        return new SimpleDateFormat(DATE_TIME_PATTERN, LOCALE_IN).format(date);
    }

    /**
     * Converts amount to currency-formatted Excel cell value.
     */
    @NonNull
    public static String toCurrencyCellValue(@Nullable Double amount) {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(LOCALE_IN);
        return numberFormat.format(amount != null ? amount : 0.0);
    }

    /**
     * Converts any object to a non-null string cell value.
     */
    @NonNull
    public static String toCellValue(@Nullable Object value) {
        if (value == null) {
            return EMPTY_CELL;
        }
        if (value instanceof Date) {
            return toDateCellValue((Date) value);
        }
        if (value instanceof Double || value instanceof Float) {
            return toCurrencyCellValue(((Number) value).doubleValue());
        }
        return String.valueOf(value);
    }

    /**
     * Builds a row with formatted cell values from heterogeneous inputs.
     */
    @NonNull
    public static List<String> buildRow(@Nullable Object... values) {
        if (values == null || values.length == 0) {
            return new ArrayList<>();
        }
        List<String> row = new ArrayList<>(values.length);
        for (Object value : values) {
            row.add(toCellValue(value));
        }
        return row;
    }

    /**
     * Builds an immutable header row from text labels.
     */
    @NonNull
    public static List<String> buildHeaderRow(@Nullable String... headers) {
        if (headers == null || headers.length == 0) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(headers));
    }
}
