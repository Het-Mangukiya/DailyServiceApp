package com.dailyserviceapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.dailyserviceapp.auth.LoginActivity;
import com.dailyserviceapp.dashboard.DashboardActivity;
import com.dailyserviceapp.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 seconds

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

    private void navigateToNextScreen() {
        SessionManager sessionManager = new SessionManager(this);
        Intent intent;

        if (sessionManager.isLoggedIn()) {
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
