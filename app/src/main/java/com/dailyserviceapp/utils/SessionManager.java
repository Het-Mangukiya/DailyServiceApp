package com.dailyserviceapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages user session data using SharedPreferences.
 * Handles login state, user information storage and retrieval,
 * and logout functionality for the DailyDrop application.
 * 
 * <p>This class provides a simple interface to persist user data
 * across app sessions without requiring a database for session management.</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public class SessionManager {
    /** SharedPreferences file name for storing session data */
    private static final String PREF_NAME = "DailyDropSession";
    
    /** Key for storing login status */
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    
    /** Key for storing user ID */
    private static final String KEY_USER_ID = "userId";
    
    /** Key for storing user email */
    private static final String KEY_USER_EMAIL = "userEmail";
    
    /** Key for storing user name */
    private static final String KEY_USER_NAME = "userName";
    
    /** Key for storing user role (Customer/Provider) */
    private static final String KEY_USER_ROLE = "userRole";

    /** SharedPreferences instance for reading data */
    private final SharedPreferences preferences;
    
    /** Editor for writing data to SharedPreferences */
    private final SharedPreferences.Editor editor;

    /**
     * Constructs a new SessionManager instance.
     * Initializes SharedPreferences with private mode.
     * 
     * @param context The application context used to access SharedPreferences
     */
    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    /**
     * Saves user session data after successful login.
     * Stores user ID, email, name, role, and sets login status to true.
     * Changes are applied asynchronously.
     * 
     * @param userId The unique identifier for the user
     * @param email The user's email address
     * @param name The user's full name
     * @param role The user's role (e.g., "ROLE_CUSTOMER" or "ROLE_PROVIDER")
     */
    public void saveUserSession(String userId, String email, String name, String role) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    /**
     * Checks if a user is currently logged in.
     * 
     * @return true if user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Retrieves the logged-in user's ID.
     * 
     * @return The user ID if logged in, null otherwise
     */
    public String getUserId() {
        return preferences.getString(KEY_USER_ID, null);
    }

    /**
     * Retrieves the logged-in user's email address.
     * 
     * @return The user email if logged in, null otherwise
     */
    public String getUserEmail() {
        return preferences.getString(KEY_USER_EMAIL, null);
    }

    /**
     * Retrieves the logged-in user's full name.
     * 
     * @return The user name if logged in, null otherwise
     */
    public String getUserName() {
        return preferences.getString(KEY_USER_NAME, null);
    }

    /**
     * Retrieves the logged-in user's role.
     * 
     * @return The user role if logged in, null otherwise
     */
    public String getUserRole() {
        return preferences.getString(KEY_USER_ROLE, null);
    }

    /**
     * Logs out the current user by clearing all session data.
     * Removes all stored user information from SharedPreferences.
     * Changes are applied asynchronously.
     */
    public void logout() {
        editor.clear();
        editor.apply();
    }
}
