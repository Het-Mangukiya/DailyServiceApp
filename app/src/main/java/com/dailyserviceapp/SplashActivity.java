package com.dailyserviceapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.dailyserviceapp.auth.LoginActivity;
import com.dailyserviceapp.customer.CustomerHomeActivity;
import com.dailyserviceapp.core.sync.SyncWorkScheduler;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.core.utils.PreferenceManager;
import com.dailyserviceapp.dashboard.DashboardActivity;
import com.dailyserviceapp.profile.ProfileActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private FirebaseFirestore firestore;

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

        // Ensure background sync worker is always registered.
        SyncWorkScheduler.ensurePeriodicSync(this);
        firestore = FirebaseFirestore.getInstance();

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
        if (!preferenceManager.isLoggedIn()) {
            openLogin();
            return;
        }

        String userId = preferenceManager.getUserId();
        String role = preferenceManager.getUserRole();
        if (userId == null || userId.trim().isEmpty()) {
            preferenceManager.clearAllData();
            openLogin();
            return;
        }

        if (Constants.ROLE_PROVIDER.equals(role)) {
            enforceProviderProfileSetup(userId);
            return;
        }

        if (Constants.ROLE_CUSTOMER.equals(role)) {
            openCustomerHome();
            return;
        }

        resolveRoleAndRoute(userId, preferenceManager);
    }

    private void resolveRoleAndRoute(String userId, PreferenceManager preferenceManager) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                String role = documentSnapshot != null && documentSnapshot.exists()
                    ? documentSnapshot.getString("role")
                    : null;

                if (Constants.ROLE_PROVIDER.equals(role)) {
                    preferenceManager.setUserRole(Constants.ROLE_PROVIDER);
                    enforceProviderProfileSetup(userId);
                    return;
                }

                if (Constants.ROLE_CUSTOMER.equals(role)) {
                    preferenceManager.setUserRole(Constants.ROLE_CUSTOMER);
                    openCustomerHome();
                    return;
                }

                preferenceManager.clearAllData();
                openLogin();
            })
            .addOnFailureListener(e -> {
                preferenceManager.clearAllData();
                openLogin();
            });
    }

    private void enforceProviderProfileSetup(String userId) {
        firestore.collection(Constants.COLLECTION_PROVIDERS)
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (isProviderProfileComplete(documentSnapshot)) {
                    openDashboard();
                } else {
                    openProfileSetup();
                }
            })
            .addOnFailureListener(e -> openProfileSetup());
    }

    private boolean isProviderProfileComplete(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot == null || !documentSnapshot.exists()) return false;

        String businessName = safeTrim(documentSnapshot.getString("businessName"));
        String ownerName = safeTrim(documentSnapshot.getString("name"));
        String phone = safeTrim(documentSnapshot.getString("phone"));
        String address = safeTrim(documentSnapshot.getString("address"));

        @SuppressWarnings("unchecked")
        java.util.List<String> services = (java.util.List<String>) documentSnapshot.get("services");
        String serviceType = safeTrim(documentSnapshot.getString("serviceType"));
        boolean hasService = (services != null && !services.isEmpty()) || !serviceType.isEmpty();

        return !businessName.isEmpty()
            && !ownerName.isEmpty()
            && !phone.isEmpty()
            && !address.isEmpty()
            && hasService;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void openLogin() {
        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        finish();
    }

    private void openDashboard() {
        startActivity(new Intent(SplashActivity.this, DashboardActivity.class));
        finish();
    }

    private void openCustomerHome() {
        startActivity(new Intent(SplashActivity.this, CustomerHomeActivity.class));
        finish();
    }

    private void openProfileSetup() {
        Intent intent = new Intent(SplashActivity.this, ProfileActivity.class);
        intent.putExtra(Constants.EXTRA_FORCE_PROFILE_SETUP, true);
        startActivity(intent);
        finish();
    }
}
