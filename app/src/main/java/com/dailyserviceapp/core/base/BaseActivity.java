package com.dailyserviceapp.core.base;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.dailyserviceapp.core.utils.NetworkMonitor;
import com.dailyserviceapp.core.utils.PreferenceManager;

/**
 * Base Activity for all activities in the DailyDrop application.
 * Provides common functionality including preference management,
 * network monitoring, toolbar setup, and utility methods.
 * 
 * <p>All activities should extend this base class to inherit
 * common features like user session management, network checking,
 * and consistent UI patterns.</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public abstract class BaseActivity extends AppCompatActivity {
    
    /** Preference manager for accessing user data */
    protected PreferenceManager preferenceManager;
    
    /** Network monitor for checking connectivity */
    protected NetworkMonitor networkMonitor;
    
    /**
     * Initializes base activity components including preference manager
     * and network monitor.
     * 
     * @param savedInstanceState Saved state bundle
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(this);
        networkMonitor = new NetworkMonitor(this);
    }
    
    /**
     * Sets up the toolbar with title and optional back button.
     * 
     * @param toolbar The toolbar to configure
     * @param title The title to display in the toolbar
     * @param showBackButton Whether to show the back navigation button
     */
    protected void setupToolbar(Toolbar toolbar, String title, boolean showBackButton) {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
                getSupportActionBar().setDisplayHomeAsUpEnabled(showBackButton);
            }
        }
    }
    
    /**
     * Handles toolbar menu item selection, specifically back button.
     * 
     * @param item The selected menu item
     * @return true if item selection was handled
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Shows a short toast message.
     * 
     * @param message The message to display
     */
    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Shows a long toast message.
     * 
     * @param message The message to display
     */
    protected void showLongToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Checks if network connection is available.
     * 
     * @return true if network is available, false otherwise
     */
    protected boolean isNetworkAvailable() {
        return networkMonitor.isNetworkAvailable();
    }
    
    /**
     * Shows a standard network error message.
     */
    protected void showNetworkError() {
        showToast("No internet connection");
    }
    
    /**
     * Gets the current user ID from preferences.
     * 
     * @return The user ID, or null if not logged in
     */
    protected String getCurrentUserId() {
        return preferenceManager.getUserId();
    }
    
    /**
     * Get current user role
     */
    protected String getCurrentUserRole() {
        return preferenceManager.getUserRole();
    }
    
    /**
     * Check if user is provider
     */
    protected boolean isProvider() {
        return preferenceManager.isProvider();
    }
    
    /**
     * Check if user is customer
     */
    protected boolean isCustomer() {
        return preferenceManager.isCustomer();
    }
    
    /**
     * Check if user is logged in
     */
    protected boolean isLoggedIn() {
        return preferenceManager.isLoggedIn();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkMonitor != null) {
            networkMonitor.unregisterNetworkCallback();
        }
    }
}
