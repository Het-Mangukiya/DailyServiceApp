package com.dailyserviceapp.core.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manager class for handling SharedPreferences operations.
 * Provides convenient methods for storing and retrieving user data,
 * login status, and role information from SharedPreferences.
 * 
 * <p>This is a wrapper around SharedPreferences that uses constants
 * defined in {@link Constants} for consistent key management.</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public class PreferenceManager {
    
    /** SharedPreferences instance for data persistence */
    private final SharedPreferences sharedPreferences;
    
    /**
     * Constructs a PreferenceManager with the application context.
     * Initializes SharedPreferences with MODE_PRIVATE.
     * 
     * @param context The application context
     */
    public PreferenceManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(
            Constants.PREF_NAME, 
            Context.MODE_PRIVATE
        );
    }
    
    /**
     * Stores the user ID in SharedPreferences.
     * 
     * @param userId The user ID to store
     */
    public void setUserId(String userId) {
        sharedPreferences.edit().putString(Constants.KEY_USER_ID, userId).apply();
    }
    
    /**
     * Retrieves the stored user ID.
     * 
     * @return The user ID, or null if not set
     */
    public String getUserId() {
        return sharedPreferences.getString(Constants.KEY_USER_ID, null);
    }
    
    /**
     * Stores the user role in SharedPreferences.
     * 
     * @param role The user role (PROVIDER or CUSTOMER)
     */
    public void setUserRole(String role) {
        sharedPreferences.edit().putString(Constants.KEY_USER_ROLE, role).apply();
    }
    
    /**
     * Retrieves the stored user role.
     * 
     * @return The user role, or null if not set
     */
    public String getUserRole() {
        return sharedPreferences.getString(Constants.KEY_USER_ROLE, null);
    }
    
    /**
     * Checks if the current user is a provider.
     * 
     * @return true if user role is PROVIDER, false otherwise
     */
    public boolean isProvider() {
        return Constants.ROLE_PROVIDER.equals(getUserRole());
    }
    
    /**
     * Checks if the current user is a customer.
     * 
     * @return true if user role is CUSTOMER, false otherwise
     */
    public boolean isCustomer() {
        return Constants.ROLE_CUSTOMER.equals(getUserRole());
    }
    
    /**
     * Stores the user email in SharedPreferences.
     * 
     * @param email The user email to store
     */
    public void setUserEmail(String email) {
        sharedPreferences.edit().putString(Constants.KEY_USER_EMAIL, email).apply();
    }
    
    /**
     * Retrieves the stored user email.
     * 
     * @return The user email, or null if not set
     */
    public String getUserEmail() {
        return sharedPreferences.getString(Constants.KEY_USER_EMAIL, null);
    }
    
    /**
     * Stores the user name in SharedPreferences.
     * 
     * @param name The user name to store
     */
    public void setUserName(String name) {
        sharedPreferences.edit().putString(Constants.KEY_USER_NAME, name).apply();
    }
    
    /**
     * Retrieves the stored user name.
     * 
     * @return The user name, or null if not set
     */
    public String getUserName() {
        return sharedPreferences.getString(Constants.KEY_USER_NAME, null);
    }
    
    /**
     * Sets the user login status.
     * 
     * @param isLoggedIn true if user is logged in, false otherwise
     */
    public void setLoggedIn(boolean isLoggedIn) {
        sharedPreferences.edit().putBoolean(Constants.KEY_IS_LOGGED_IN, isLoggedIn).apply();
    }
    
    /**
     * Checks if a user is currently logged in.
     * 
     * @return true if user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(Constants.KEY_IS_LOGGED_IN, false);
    }
    
    /**
     * Saves all user data at once in a single transaction.
     * Sets login status to true automatically.
     * 
     * @param userId The user ID
     * @param email The user email
     * @param name The user name
     * @param role The user role (PROVIDER or CUSTOMER)
     */
    public void saveUserData(String userId, String email, String name, String role) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.KEY_USER_ID, userId);
        editor.putString(Constants.KEY_USER_EMAIL, email);
        editor.putString(Constants.KEY_USER_NAME, name);
        editor.putString(Constants.KEY_USER_ROLE, role);
        editor.putBoolean(Constants.KEY_IS_LOGGED_IN, true);
        editor.apply();
    }
    
    // Clear all data (logout)
    public void clearAllData() {
        sharedPreferences.edit().clear().apply();
    }
    
    // Generic methods
    public void putString(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }
    
    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }
    
    public void putInt(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }
    
    public int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }
    
    public void putBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }
    
    public void putLong(String key, long value) {
        sharedPreferences.edit().putLong(key, value).apply();
    }
    
    public long getLong(String key, long defaultValue) {
        return sharedPreferences.getLong(key, defaultValue);
    }
    
    public void remove(String key) {
        sharedPreferences.edit().remove(key).apply();
    }
}
