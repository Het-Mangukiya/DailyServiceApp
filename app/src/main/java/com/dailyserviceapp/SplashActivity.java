package com.dailyserviceapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.dailyserviceapp.auth.LoginActivity;
import com.dailyserviceapp.customer.CustomerHomeActivity;
import com.dailyserviceapp.core.sync.SyncWorkScheduler;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.core.utils.PreferenceManager;
import com.dailyserviceapp.dashboard.DashboardActivity;
import com.dailyserviceapp.customer.CustomerServiceDashboardActivity;
import com.dailyserviceapp.profile.ProfileActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

/**
 * Splash screen activity with corrected routing for Customer side.
 */
@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000;
    private FirebaseFirestore firestore;
    
    @Inject
    protected PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        SyncWorkScheduler.ensurePeriodicSync(this);
        firestore = FirebaseFirestore.getInstance();
        
        capturePendingInviteFromIntent(getIntent());

        long splashDelay = getIntent() != null
            && getIntent().getBooleanExtra(Constants.EXTRA_SKIP_SPLASH_DELAY, false)
            ? 0L : SPLASH_DELAY;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isActivityInactive()) return;
            navigateToNextScreen();
        }, splashDelay);
    }

    private void navigateToNextScreen() {
        if (isActivityInactive()) return;
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
            clearPendingInvite();
            enforceProviderProfileSetup(userId);
            return;
        }

        if (Constants.ROLE_CUSTOMER.equals(role)) {
            // FIX: Always go to Customer Home first
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
                if (isActivityInactive()) return;
                String role = documentSnapshot != null && documentSnapshot.exists()
                    ? documentSnapshot.getString("role")
                    : null;

                if (Constants.ROLE_PROVIDER.equals(role)) {
                    preferenceManager.setUserRole(Constants.ROLE_PROVIDER);
                    clearPendingInvite();
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
                if (isActivityInactive()) return;
                Log.w("SplashActivity", "Failed to resolve role", e);
                preferenceManager.clearAllData();
                openLogin();
            });
    }

    private void capturePendingInviteFromIntent(Intent intent) {
        if (intent == null) return;
        Uri data = intent.getData();
        if (data == null) return;

        String token = safeTrim(data.getQueryParameter("token"));
        if (token.isEmpty()) return;

        String inviteCustomerId = safeTrim(data.getQueryParameter("cid"));
        preferenceManager.putString(Constants.KEY_PENDING_INVITE_TOKEN, token);
        if (inviteCustomerId.isEmpty()) {
            preferenceManager.remove(Constants.KEY_PENDING_INVITE_CUSTOMER_ID);
        } else {
            preferenceManager.putString(Constants.KEY_PENDING_INVITE_CUSTOMER_ID, inviteCustomerId);
        }
    }

    private void clearPendingInvite() {
        preferenceManager.remove(Constants.KEY_PENDING_INVITE_TOKEN);
        preferenceManager.remove(Constants.KEY_PENDING_INVITE_CUSTOMER_ID);
    }

    private void enforceProviderProfileSetup(String userId) {
        firestore.collection(Constants.COLLECTION_PROVIDERS)
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (isActivityInactive()) return;
                if (isProviderProfileComplete(documentSnapshot)) {
                    openDashboard();
                } else {
                    openProfileSetup();
                }
            })
            .addOnFailureListener(e -> {
                if (isActivityInactive()) return;
                Log.w("SplashActivity", "Failed to fetch provider profile", e);
                openProfileSetup();
            });
    }

    private boolean isProviderProfileComplete(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot == null || !documentSnapshot.exists()) return false;

        String businessName = safeTrim(documentSnapshot.getString("businessName"));
        String ownerName = safeTrim(documentSnapshot.getString("name"));
        String phone = safeTrim(documentSnapshot.getString("phone"));
        String address = safeTrim(documentSnapshot.getString("address"));

        List<String> services = new ArrayList<>();
        Object rawServices = documentSnapshot.get("services");
        if (rawServices instanceof List) {
            List<?> casted = (List<?>) rawServices;
            for (Object item : casted) {
                if (item instanceof String) {
                    String value = safeTrim((String) item);
                    if (!value.isEmpty()) services.add(value);
                }
            }
        }
        String serviceType = safeTrim(documentSnapshot.getString("serviceType"));
        return !businessName.isEmpty() && !ownerName.isEmpty() && !phone.isEmpty() && !address.isEmpty() && ((services != null && !services.isEmpty()) || !serviceType.isEmpty());
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

    private boolean isActivityInactive() {
        return isFinishing() || isDestroyed();
    }
}
