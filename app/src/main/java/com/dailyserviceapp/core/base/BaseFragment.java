package com.dailyserviceapp.core.base;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dailyserviceapp.core.utils.NetworkMonitor;
import com.dailyserviceapp.core.utils.PreferenceManager;

/**
 * Base Fragment for all fragments in the DailyDrop application.
 * Provides common functionality including preference management,
 * network monitoring, and utility methods for UI feedback.
 * 
 * <p>All fragments should extend this base class to inherit
 * common features like user session management and network checking.</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public abstract class BaseFragment extends Fragment {
    
    /** Preference manager for accessing user data */
    protected PreferenceManager preferenceManager;
    
    /** Network monitor for checking connectivity */
    protected NetworkMonitor networkMonitor;
    
    /**
     * Initializes base fragment components after view creation.
     * 
     * @param view The created view
     * @param savedInstanceState Saved state bundle
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() != null) {
            preferenceManager = new PreferenceManager(getContext());
            networkMonitor = new NetworkMonitor(getContext());
        }
    }
    
    /**
     * Shows a short toast message.
     * 
     * @param message The message to display
     */
    protected void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Shows a long toast message.
     * 
     * @param message The message to display
     */
    protected void showLongToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Checks if network connection is available.
     * 
     * @return true if network is available, false otherwise
     */
    protected boolean isNetworkAvailable() {
        return networkMonitor != null && networkMonitor.isNetworkAvailable();
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
        return preferenceManager != null ? preferenceManager.getUserId() : null;
    }
    
    /**
     * Gets the current user role.
     * 
     * @return The user role (PROVIDER or CUSTOMER), or null if not logged in
     */
    protected String getCurrentUserRole() {
        return preferenceManager != null ? preferenceManager.getUserRole() : null;
    }
    
    /**
     * Checks if the current user is a provider.
     * 
     * @return true if user is a provider, false otherwise
     */
    protected boolean isProvider() {
        return preferenceManager != null && preferenceManager.isProvider();
    }
    
    /**
     * Check if user is customer
     */
    protected boolean isCustomer() {
        return preferenceManager != null && preferenceManager.isCustomer();
    }
    
    /**
     * Check if user is logged in
     */
    protected boolean isLoggedIn() {
        return preferenceManager != null && preferenceManager.isLoggedIn();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (networkMonitor != null) {
            networkMonitor.unregisterNetworkCallback();
        }
    }
}
