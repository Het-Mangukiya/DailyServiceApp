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
import java.util.concurrent.TimeUnit;

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
    private PreferenceManager preferenceManager;

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
        preferenceManager = new PreferenceManager(this);
        capturePendingInviteFromIntent(getIntent());

        long splashDelay = getIntent() != null
            && getIntent().getBooleanExtra(Constants.EXTRA_SKIP_SPLASH_DELAY, false)
            ? 0L : SPLASH_DELAY;

        // Delay and navigate
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isActivityInactive()) return;
            navigateToNextScreen();
        }, splashDelay);
    }

    /**
     * Determines the next screen to navigate to based on user's login status.
     * If the user is logged in (session exists), navigates to the Dashboard.
     * Otherwise, navigates to the Login screen.
     * Finishes this activity to prevent returning to splash screen on back press.
     */
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
            routeCustomerWithInvite(userId);
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
                    routeCustomerWithInvite(userId);
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

    private void routeCustomerWithInvite(String userId) {
        String pendingToken = safeTrim(preferenceManager.getString(Constants.KEY_PENDING_INVITE_TOKEN, ""));
        if (!pendingToken.isEmpty()) {
            claimPendingInviteAndRoute(userId, pendingToken);
            return;
        }
        routeCustomerByLinkStatus(userId);
    }

    private void routeCustomerByLinkStatus(String userId) {
        firestore.collection(Constants.COLLECTION_CUSTOMER_LINKS)
            .document(userId)
            .get()
            .addOnSuccessListener(linkDoc -> {
                if (isActivityInactive()) return;
                if (linkDoc == null || !linkDoc.exists()) {
                    openCustomerHome();
                    return;
                }

                String status = safeTrim(linkDoc.getString("status")).toUpperCase(Locale.US);
                String providerId = safeTrim(linkDoc.getString("providerId"));
                if ("ACTIVE".equals(status) && !providerId.isEmpty()) {
                    openCustomerDashboard();
                    return;
                }
                openCustomerHome();
            })
            .addOnFailureListener(e -> {
                if (isActivityInactive()) return;
                Log.w("SplashActivity", "Failed to load customer link", e);
                openCustomerHome();
            });
    }

    private void claimPendingInviteAndRoute(String userId, String token) {
        String inviteHash = sha256(token);
        if (inviteHash.isEmpty()) {
            clearPendingInvite();
            routeCustomerByLinkStatus(userId);
            return;
        }

        firestore.collection(Constants.COLLECTION_CUSTOMER_INVITES)
            .document(inviteHash)
            .get()
            .addOnSuccessListener(inviteDoc -> {
                if (isActivityInactive()) return;
                if (inviteDoc == null || !inviteDoc.exists()) {
                    clearPendingInvite();
                    routeCustomerByLinkStatus(userId);
                    return;
                }

                String status = safeTrim(inviteDoc.getString("status")).toUpperCase(Locale.US);
                String providerId = safeTrim(inviteDoc.getString("providerId"));
                String providerCustomerId = safeTrim(inviteDoc.getString("providerCustomerId"));
                Timestamp expiresAt = inviteDoc.getTimestamp("expiresAt");
                String expectedCustomerId = safeTrim(
                    preferenceManager.getString(Constants.KEY_PENDING_INVITE_CUSTOMER_ID, "")
                );

                boolean isExpired = expiresAt != null && expiresAt.toDate().getTime() < System.currentTimeMillis();
                boolean mismatchedCustomer = !expectedCustomerId.isEmpty()
                    && !providerCustomerId.isEmpty()
                    && !expectedCustomerId.equals(providerCustomerId);

                if (!"PENDING".equals(status) || providerId.isEmpty() || providerCustomerId.isEmpty()
                    || isExpired || mismatchedCustomer) {
                    clearPendingInvite();
                    routeCustomerByLinkStatus(userId);
                    return;
                }

                String providerName = safeTrim(inviteDoc.getString("providerName"));
                String customerName = safeTrim(inviteDoc.getString("customerName"));
                if (customerName.isEmpty()) {
                    customerName = safeTrim(preferenceManager.getUserName());
                }

                WriteBatch batch = firestore.batch();

                Map<String, Object> linkData = new HashMap<>();
                linkData.put("customerId", userId);
                linkData.put("providerId", providerId);
                linkData.put("providerName", providerName);
                linkData.put("providerCustomerId", providerCustomerId);
                linkData.put("customerName", customerName);
                linkData.put("status", "ACTIVE");
                linkData.put("linkedVia", "INVITE_LINK");
                linkData.put("updatedAt", FieldValue.serverTimestamp());
                linkData.put("respondedAt", FieldValue.serverTimestamp());
                linkData.put("linkedAt", FieldValue.serverTimestamp());

                Map<String, Object> inviteUpdate = new HashMap<>();
                inviteUpdate.put("providerId", providerId);
                inviteUpdate.put("providerCustomerId", providerCustomerId);
                inviteUpdate.put("status", "CLAIMED");
                inviteUpdate.put("claimedByUserId", userId);
                inviteUpdate.put("claimedAt", FieldValue.serverTimestamp());
                inviteUpdate.put("updatedAt", FieldValue.serverTimestamp());

                batch.set(
                    firestore.collection(Constants.COLLECTION_CUSTOMER_LINKS).document(userId),
                    linkData,
                    SetOptions.merge()
                );
                batch.set(
                    firestore.collection(Constants.COLLECTION_CUSTOMER_INVITES).document(inviteHash),
                    inviteUpdate,
                    SetOptions.merge()
                );

                batch.commit()
                    .addOnSuccessListener(unused -> {
                        if (isActivityInactive()) return;
                        clearPendingInvite();
                        openCustomerDashboard();
                    })
                    .addOnFailureListener(e -> {
                        if (isActivityInactive()) return;
                        Log.w("SplashActivity", "Failed to claim invite", e);
                        routeCustomerByLinkStatus(userId);
                    });
            })
            .addOnFailureListener(e -> {
                if (isActivityInactive()) return;
                Log.w("SplashActivity", "Failed to fetch invite", e);
                routeCustomerByLinkStatus(userId);
            });
    }

    private void clearPendingInvite() {
        preferenceManager.remove(Constants.KEY_PENDING_INVITE_TOKEN);
        preferenceManager.remove(Constants.KEY_PENDING_INVITE_CUSTOMER_ID);
    }

    private String sha256(String input) {
        if (input == null || input.trim().isEmpty()) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format(Locale.US, "%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e("SplashActivity", "SHA-256 not available", e);
            return "";
        }
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
                    if (!value.isEmpty()) {
                        services.add(value);
                    }
                }
            }
        }
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
        startActivity(new Intent(SplashActivity.this, com.dailyserviceapp.dashboard.ProviderDashboardActivity.class));
        finish();
    }

    private void openCustomerDashboard() {
        startActivity(new Intent(SplashActivity.this, CustomerServiceDashboardActivity.class));
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
