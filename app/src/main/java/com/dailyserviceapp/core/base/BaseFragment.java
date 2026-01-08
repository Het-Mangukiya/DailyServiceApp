package com.dailyserviceapp.core.base;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dailyserviceapp.core.utils.NetworkMonitor;
import com.dailyserviceapp.core.utils.PreferenceManager;

public abstract class BaseFragment extends Fragment {
    
    protected PreferenceManager preferenceManager;
    protected NetworkMonitor networkMonitor;
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() != null) {
            preferenceManager = new PreferenceManager(getContext());
            networkMonitor = new NetworkMonitor(getContext());
        }
    }
    
    /**
     * Show toast message
     */
    protected void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Show long toast message
     */
    protected void showLongToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Check if network is available
     */
    protected boolean isNetworkAvailable() {
        return networkMonitor != null && networkMonitor.isNetworkAvailable();
    }
    
    /**
     * Show network error message
     */
    protected void showNetworkError() {
        showToast("No internet connection");
    }
    
    /**
     * Get current user ID from preferences
     */
    protected String getCurrentUserId() {
        return preferenceManager != null ? preferenceManager.getUserId() : null;
    }
    
    /**
     * Get current user role
     */
    protected String getCurrentUserRole() {
        return preferenceManager != null ? preferenceManager.getUserRole() : null;
    }
    
    /**
     * Check if user is provider
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
