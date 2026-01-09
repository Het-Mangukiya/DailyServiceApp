package com.dailyserviceapp.core.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for currency formatting and parsing operations.
 * Provides methods to format amounts with Indian Rupee (₹) symbol,
 * parse currency strings, and handle currency conversions.
 * 
 * <p>Supports Indian number formatting with proper comma placement
 * according to Indian numbering system (lakhs and crores).</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public class CurrencyUtils {
    
    /** Locale for Indian currency formatting */
    private static final Locale INDIAN_LOCALE = new Locale.Builder().setLanguage("en").setRegion("IN").build();
    
    /**
     * Formats a numeric amount as currency string with ₹ symbol.
     * Example: 1234.56 → "₹1,234.56"
     * Thread-safe: creates new formatter instance for each call.
     * 
     * @param amount The amount to format
     * @return Formatted currency string with ₹ symbol
     */
    public static String formatCurrency(double amount) {
        DecimalFormat formatter = new DecimalFormat("₹#,##0.00");
        return formatter.format(amount);
    }
    
    /**
     * Formats a numeric amount without currency symbol.
     * Example: 1234.56 → "1,234.56"
     * Thread-safe: creates new formatter instance for each call.
     * 
     * @param amount The amount to format
     * @return Formatted number string with comma separators
     */
    public static String formatAmount(double amount) {
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        return formatter.format(amount);
    }
    
    /**
     * Formats amount using Indian currency formatting rules.
     * Uses locale-specific formatting with proper comma placement.
     * Thread-safe: creates new formatter instance for each call.
     * 
     * @param amount The amount to format
     * @return Formatted currency string in Indian format
     */
    public static String formatIndianCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(INDIAN_LOCALE);
        return formatter.format(amount);
    }
    
    /**
     * Parses a currency string and extracts the numeric value.
     * Removes currency symbols, commas, and spaces before parsing.
     * Returns 0.0 if parsing fails or input is invalid.
     * 
     * @param currencyString The currency string to parse (e.g., "₹1,234.56")
     * @return The numeric value, or 0.0 if parsing fails
     */
    public static double parseCurrency(String currencyString) {
        if (currencyString == null || currencyString.isEmpty()) {
            return 0.0;
        }
        
        try {
            // Remove currency symbols, commas, and spaces
            String cleanString = currencyString
                .replaceAll("[₹$,\\s]", "")
                .trim();
            return Double.parseDouble(cleanString);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * Rounds an amount to 2 decimal places.
     * Uses standard rounding rules (0.5 rounds up).
     * 
     * @param amount The amount to round
     * @return The rounded amount with 2 decimal places
     */
    public static double roundAmount(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
    
    /**
     * Calculate percentage
     */
    public static double calculatePercentage(double amount, double percentage) {
        return roundAmount((amount * percentage) / 100.0);
    }
    
    /**
     * Calculate discount amount
     */
    public static double calculateDiscount(double originalAmount, double discountPercentage) {
        return calculatePercentage(originalAmount, discountPercentage);
    }
    
    /**
     * Apply discount to amount
     */
    public static double applyDiscount(double amount, double discountPercentage) {
        double discountAmount = calculateDiscount(amount, discountPercentage);
        return roundAmount(amount - discountAmount);
    }
    
    /**
     * Calculate tax amount
     */
    public static double calculateTax(double amount, double taxPercentage) {
        return calculatePercentage(amount, taxPercentage);
    }
    
    /**
     * Add tax to amount
     */
    public static double addTax(double amount, double taxPercentage) {
        double taxAmount = calculateTax(amount, taxPercentage);
        return roundAmount(amount + taxAmount);
    }
    
    /**
     * Format amount for display in lists (compact format)
     */
    public static String formatCompactCurrency(double amount) {
        if (amount >= 100000) {
            return String.format(Locale.getDefault(), "₹%.1fL", amount / 100000);
        } else if (amount >= 1000) {
            return String.format(Locale.getDefault(), "₹%.1fK", amount / 1000);
        } else {
            return formatCurrency(amount);
        }
    }
    
    /**
     * Check if amount is valid (non-negative)
     */
    public static boolean isValidAmount(double amount) {
        return amount >= 0;
    }
    
    private CurrencyUtils() {
        // Private constructor to prevent instantiation
    }
}
