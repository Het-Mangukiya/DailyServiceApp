package com.dailyserviceapp.core.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    
    private final SharedPreferences sharedPreferences;
    
    public PreferenceManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(
            Constants.PREF_NAME, 
            Context.MODE_PRIVATE
        );
    }
    
    // User ID
    public void setUserId(String userId) {
        sharedPreferences.edit().putString(Constants.KEY_USER_ID, userId).apply();
    }
    
    public String getUserId() {
        return sharedPreferences.getString(Constants.KEY_USER_ID, null);
    }
    
    // User Role
    public void setUserRole(String role) {
        sharedPreferences.edit().putString(Constants.KEY_USER_ROLE, role).apply();
    }
    
    public String getUserRole() {
        return sharedPreferences.getString(Constants.KEY_USER_ROLE, null);
    }
    
    public boolean isProvider() {
        return Constants.ROLE_PROVIDER.equals(getUserRole());
    }
    
    public boolean isCustomer() {
        return Constants.ROLE_CUSTOMER.equals(getUserRole());
    }
    
    // User Email
    public void setUserEmail(String email) {
        sharedPreferences.edit().putString(Constants.KEY_USER_EMAIL, email).apply();
    }
    
    public String getUserEmail() {
        return sharedPreferences.getString(Constants.KEY_USER_EMAIL, null);
    }
    
    // User Name
    public void setUserName(String name) {
        sharedPreferences.edit().putString(Constants.KEY_USER_NAME, name).apply();
    }
    
    public String getUserName() {
        return sharedPreferences.getString(Constants.KEY_USER_NAME, null);
    }
    
    // Login Status
    public void setLoggedIn(boolean isLoggedIn) {
        sharedPreferences.edit().putBoolean(Constants.KEY_IS_LOGGED_IN, isLoggedIn).apply();
    }
    
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(Constants.KEY_IS_LOGGED_IN, false);
    }
    
    // Save user data
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
