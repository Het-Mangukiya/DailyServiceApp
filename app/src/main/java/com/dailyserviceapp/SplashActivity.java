package com.dailyserviceapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.dailyserviceapp.auth.LoginActivity;
import com.dailyserviceapp.dashboard.DashboardActivity;
import com.dailyserviceapp.core.utils.PreferenceManager;

/**
 * Splash screen activity displayed on app launch.
 * Shows the DailyDrop logo and brand name for 2 seconds,
 * then navigates to either the Dashboard (if user is logged in)
 * or the Login screen (if user is not logged in).
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public class SplashActivity extends AppCompatActivity {

    /**
     * Duration in milliseconds to display the splash screen.
     */
    private static final int SPLASH_DELAY = 2000; // 2 seconds

    /**
     * Called when the activity is first created.
     * Sets up the splash screen layout, hides the action bar,
     * and schedules navigation to the next screen after a delay.
     * 
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                          being shut down, this Bundle contains the most recent data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Delay and navigate
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            navigateToNextScreen();
        }, SPLASH_DELAY);
    }

    /**
     * Determines the next screen to navigate to based on user's login status.
     * If the user is logged in (session exists), navigates to the Dashboard.
     * Otherwise, navigates to the Login screen.
     * Finishes this activity to prevent returning to splash screen on back press.
     */
    private void navigateToNextScreen() {
        PreferenceManager preferenceManager = new PreferenceManager(this);
        Intent intent;

        if (preferenceManager.isLoggedIn()) {
            // User is logged in, go to dashboard
            intent = new Intent(SplashActivity.this, DashboardActivity.class);
        } else {
            // User not logged in, go to login
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
