package com.dailyserviceapp.core.base;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.dailyserviceapp.core.utils.NetworkMonitor;
import com.dailyserviceapp.core.utils.PreferenceManager;

public abstract class BaseActivity extends AppCompatActivity {
    
    protected PreferenceManager preferenceManager;
    protected NetworkMonitor networkMonitor;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(this);
        networkMonitor = new NetworkMonitor(this);
    }
    
    /**
     * Setup toolbar with back button
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
     * Handle back button press in toolbar
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
     * Show toast message
     */
    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Show long toast message
     */
    protected void showLongToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Check if network is available
     */
    protected boolean isNetworkAvailable() {
        return networkMonitor.isNetworkAvailable();
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
