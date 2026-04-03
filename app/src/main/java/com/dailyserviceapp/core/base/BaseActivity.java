package com.dailyserviceapp.core.base;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.utils.NetworkMonitor;
import com.dailyserviceapp.core.utils.PreferenceManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

/**
 * Base Activity class providing common functionality for all activities.
 * Handles network monitoring, UI setup, and user preferences.
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
@AndroidEntryPoint
public abstract class BaseActivity extends AppCompatActivity {
    
    /** Preference manager for accessing user data */
    @Inject
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
        // preferenceManager is injected by Hilt
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
            applyToolbarInsets(toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
                getSupportActionBar().setDisplayHomeAsUpEnabled(showBackButton);
            }
        }
    }

    protected void applyToolbarInsets(Toolbar toolbar) {
        if (toolbar == null) return;

        final int initialTopPadding = toolbar.getPaddingTop();
        final int initialLeftPadding = toolbar.getPaddingLeft();
        final int initialRightPadding = toolbar.getPaddingRight();
        final int initialBottomPadding = toolbar.getPaddingBottom();
        final int initialHeight = toolbar.getLayoutParams() != null ? toolbar.getLayoutParams().height : 0;

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (View view, WindowInsetsCompat insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (view.getLayoutParams() != null && initialHeight > 0) {
                view.getLayoutParams().height = initialHeight + systemBars.top;
                view.setLayoutParams(view.getLayoutParams());
            }
            view.setPadding(
                initialLeftPadding,
                initialTopPadding + systemBars.top,
                initialRightPadding,
                initialBottomPadding
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(toolbar);
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
    
    /**
     * Navigate to login screen and clear activity stack
     */
    protected void navigateToLogin() {
        Intent intent = new Intent(this, com.dailyserviceapp.auth.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    /**
     * Perform logout: clear preferences and Firebase session
     * Note: Activities using Google Sign-In should override to add googleSignInClient.signOut()
     */
    protected void performLogout() {
        preferenceManager.clearAllData();
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(
            this,
            new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        );
        googleSignInClient.signOut().addOnCompleteListener(this, task -> navigateToLogin());
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkMonitor != null) {
            networkMonitor.unregisterNetworkCallback();
        }
    }
}
