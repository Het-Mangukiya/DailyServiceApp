package com.dailyserviceapp.core.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyUtils {
    
    private static final DecimalFormat currencyFormat = new DecimalFormat("₹#,##0.00");
    private static final DecimalFormat simpleFormat = new DecimalFormat("#,##0.00");
    private static final NumberFormat indianFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    
    /**
     * Format amount to currency string with ₹ symbol
     */
    public static String formatCurrency(double amount) {
        return currencyFormat.format(amount);
    }
    
    /**
     * Format amount without currency symbol
     */
    public static String formatAmount(double amount) {
        return simpleFormat.format(amount);
    }
    
    /**
     * Format amount with Indian currency format
     */
    public static String formatIndianCurrency(double amount) {
        return indianFormat.format(amount);
    }
    
    /**
     * Parse currency string to double
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
     * Round amount to 2 decimal places
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
