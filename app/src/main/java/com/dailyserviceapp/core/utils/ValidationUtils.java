package com.dailyserviceapp.core.utils;

import android.text.TextUtils;
import android.util.Patterns;

/**
 * Utility class for input validation operations.
 * Provides methods to validate email addresses, passwords, phone numbers,
 * and other user input data for the DailyDrop application.
 * 
 * <p>All methods are static and the class should not be instantiated.</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public class ValidationUtils {
    
    /**
     * Validates an email address using Android's Patterns utility.
     * 
     * @param email The email address to validate
     * @return true if the email is valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    
    /**
     * Validates a password against security requirements.
     * Password must be at least 8 characters long and contain
     * both letters and digits.
     * 
     * @param password The password to validate
     * @return true if password meets requirements, false otherwise
     */
    public static boolean isValidPassword(String password) {
        if (TextUtils.isEmpty(password) || password.length() < Constants.MIN_PASSWORD_LENGTH) {
            return false;
        }
        
        // Check for at least one letter and one digit
        boolean hasLetter = false;
        boolean hasDigit = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
            if (hasLetter && hasDigit) break;
        }
        
        return hasLetter && hasDigit;
    }
    
    /**
     * Generates a user-friendly message indicating password strength.
     * Evaluates password length, character types, and complexity.
     * 
     * @param password The password to evaluate
     * @return A descriptive message about password strength
     */
    public static String getPasswordStrengthMessage(String password) {
        if (TextUtils.isEmpty(password)) {
            return "Password is required";
        }
        
        if (password.length() < Constants.MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + Constants.MIN_PASSWORD_LENGTH + " characters";
        }
        
        boolean hasLetter = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        
        if (!hasLetter || !hasDigit) {
            return "Password must contain letters and numbers";
        }
        
        if (hasSpecial) {
            return "Strong password";
        }
        
        return "Good password";
    }
    
    /**
     * Validates a phone number.
     * Accepts 10-digit Indian phone numbers with optional formatting.
     * 
     * @param phone The phone number to validate
     * @return true if phone number is valid (10 digits), false otherwise
     */
    public static boolean isValidPhone(String phone) {
        if (TextUtils.isEmpty(phone)) return false;
        
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        return cleanPhone.length() == Constants.MAX_PHONE_LENGTH;
    }
    
    /**
     * Validate name (not empty and at least 2 characters)
     */
    public static boolean isValidName(String name) {
        return !TextUtils.isEmpty(name) && name.trim().length() >= 2;
    }
    
    /**
     * Validate rate (positive number)
     */
    public static boolean isValidRate(double rate) {
        return rate > 0;
    }
    
    /**
     * Validate rate string
     */
    public static boolean isValidRateString(String rateString) {
        if (TextUtils.isEmpty(rateString)) return false;
        
        try {
            double rate = Double.parseDouble(rateString);
            return rate > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validate quantity (positive integer)
     */
    public static boolean isValidQuantity(int quantity) {
        return quantity > 0;
    }
    
    /**
     * Validate quantity string
     */
    public static boolean isValidQuantityString(String quantityString) {
        if (TextUtils.isEmpty(quantityString)) return false;
        
        try {
            int quantity = Integer.parseInt(quantityString);
            return quantity > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validate address (not empty)
     */
    public static boolean isValidAddress(String address) {
        return !TextUtils.isEmpty(address) && address.trim().length() >= 5;
    }
    
    /**
     * Format phone number for display
     */
    public static String formatPhoneNumber(String phone) {
        if (TextUtils.isEmpty(phone)) return "";
        
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() != 10) return phone;
        
        return String.format("+91 %s %s %s", 
            cleanPhone.substring(0, 5),
            cleanPhone.substring(5, 8),
            cleanPhone.substring(8));
    }
    
    /**
     * Clean phone number (remove all non-digit characters)
     */
    public static String cleanPhoneNumber(String phone) {
        if (TextUtils.isEmpty(phone)) return "";
        return phone.replaceAll("[^0-9]", "");
    }
    
    /**
     * Validate amount (non-negative number)
     */
    public static boolean isValidAmount(double amount) {
        return amount >= 0;
    }
    
    /**
     * Validate amount string
     */
    public static boolean isValidAmountString(String amountString) {
        if (TextUtils.isEmpty(amountString)) return false;
        
        try {
            double amount = Double.parseDouble(amountString);
            return amount >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private ValidationUtils() {
        // Private constructor to prevent instantiation
    }
}
