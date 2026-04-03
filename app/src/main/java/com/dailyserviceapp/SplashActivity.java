package com.dailyserviceapp;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

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
 * Splash screen activity with premium brand animation and preserved routing.
 */
@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1450;
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

        startSplashAnimation();

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

    private void startSplashAnimation() {
        View logoPlate = findViewById(R.id.logoPlate);
        View logoGlow = findViewById(R.id.logoGlow);
        View logoSweep = findViewById(R.id.logoSweep);
        View checkBadge = findViewById(R.id.checkBadge);
        View brandTitle = findViewById(R.id.brandTitle);
        View brandSubtitle = findViewById(R.id.brandSubtitle);

        if (logoPlate == null) return;

        logoPlate.post(() -> {
            float riseDistance = logoPlate.getResources().getDisplayMetrics().density * 18f;

            logoPlate.setAlpha(0.15f);
            logoPlate.setScaleX(0.78f);
            logoPlate.setScaleY(0.78f);
            logoPlate.setTranslationY(riseDistance);

            if (logoGlow != null) {
                logoGlow.setAlpha(0f);
                logoGlow.setScaleX(0.78f);
                logoGlow.setScaleY(0.78f);
            }

            if (brandTitle != null) {
                brandTitle.setAlpha(0f);
                brandTitle.setTranslationY(riseDistance * 0.75f);
            }

            if (brandSubtitle != null) {
                brandSubtitle.setAlpha(0f);
                brandSubtitle.setTranslationY(riseDistance);
            }

            if (checkBadge != null) {
                checkBadge.setAlpha(0f);
                checkBadge.setScaleX(0.2f);
                checkBadge.setScaleY(0.2f);
            }

            AnimatorSet plateEntrance = new AnimatorSet();
            plateEntrance.playTogether(
                ObjectAnimator.ofFloat(logoPlate, View.ALPHA, 0.15f, 1f),
                ObjectAnimator.ofFloat(logoPlate, View.SCALE_X, 0.78f, 1f),
                ObjectAnimator.ofFloat(logoPlate, View.SCALE_Y, 0.78f, 1f),
                ObjectAnimator.ofFloat(logoPlate, View.TRANSLATION_Y, riseDistance, 0f)
            );
            plateEntrance.setDuration(520L);
            plateEntrance.setStartDelay(80L);
            plateEntrance.setInterpolator(new DecelerateInterpolator(1.7f));
            plateEntrance.start();

            if (logoGlow != null) {
                AnimatorSet glowPulse = new AnimatorSet();
                glowPulse.playTogether(
                    ObjectAnimator.ofFloat(logoGlow, View.ALPHA, 0f, 0.55f, 0.2f),
                    ObjectAnimator.ofFloat(logoGlow, View.SCALE_X, 0.78f, 1.08f, 1f),
                    ObjectAnimator.ofFloat(logoGlow, View.SCALE_Y, 0.78f, 1.08f, 1f)
                );
                glowPulse.setDuration(760L);
                glowPulse.setInterpolator(new DecelerateInterpolator(1.3f));
                glowPulse.start();
            }

            if (brandTitle != null) {
                AnimatorSet titleIn = new AnimatorSet();
                titleIn.playTogether(
                    ObjectAnimator.ofFloat(brandTitle, View.ALPHA, 0f, 1f),
                    ObjectAnimator.ofFloat(brandTitle, View.TRANSLATION_Y, riseDistance * 0.75f, 0f)
                );
                titleIn.setDuration(380L);
                titleIn.setStartDelay(150L);
                titleIn.setInterpolator(new DecelerateInterpolator(1.5f));
                titleIn.start();
            }

            if (brandSubtitle != null) {
                AnimatorSet subtitleIn = new AnimatorSet();
                subtitleIn.playTogether(
                    ObjectAnimator.ofFloat(brandSubtitle, View.ALPHA, 0f, 1f),
                    ObjectAnimator.ofFloat(brandSubtitle, View.TRANSLATION_Y, riseDistance, 0f)
                );
                subtitleIn.setDuration(360L);
                subtitleIn.setStartDelay(220L);
                subtitleIn.setInterpolator(new DecelerateInterpolator(1.4f));
                subtitleIn.start();
            }

            if (logoSweep != null) {
                float sweepStart = -logoSweep.getWidth() * 1.4f;
                float sweepEnd = logoPlate.getWidth() + (logoSweep.getWidth() * 1.1f);
                logoSweep.setTranslationX(sweepStart);

                AnimatorSet sweepAcross = new AnimatorSet();
                sweepAcross.playTogether(
                    ObjectAnimator.ofFloat(logoSweep, View.TRANSLATION_X, sweepStart, sweepEnd),
                    ObjectAnimator.ofFloat(logoSweep, View.ALPHA, 0f, 0.7f, 0f)
                );
                sweepAcross.setDuration(430L);
                sweepAcross.setStartDelay(250L);
                sweepAcross.setInterpolator(new DecelerateInterpolator(1.2f));
                sweepAcross.start();
            }

            if (checkBadge != null) {
                AnimatorSet badgePop = new AnimatorSet();
                badgePop.playTogether(
                    ObjectAnimator.ofFloat(checkBadge, View.ALPHA, 0f, 1f),
                    ObjectAnimator.ofFloat(checkBadge, View.SCALE_X, 0.2f, 1f),
                    ObjectAnimator.ofFloat(checkBadge, View.SCALE_Y, 0.2f, 1f)
                );
                badgePop.setDuration(260L);
                badgePop.setStartDelay(430L);
                badgePop.setInterpolator(new OvershootInterpolator(2.2f));
                badgePop.start();
            }
        });
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

    private void clearPendingInvite() {
        preferenceManager.remove(Constants.KEY_PENDING_INVITE_TOKEN);
        preferenceManager.remove(Constants.KEY_PENDING_INVITE_CUSTOMER_ID);
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
        launchAndFinish(new Intent(SplashActivity.this, LoginActivity.class));
    }

    private void openDashboard() {
        launchAndFinish(new Intent(SplashActivity.this, DashboardActivity.class));
    }

    private void openCustomerHome() {
        launchAndFinish(new Intent(SplashActivity.this, CustomerHomeActivity.class));
    }

    private void openCustomerDashboard() {
        launchAndFinish(new Intent(SplashActivity.this, CustomerServiceDashboardActivity.class));
    }

    private void openProfileSetup() {
        Intent intent = new Intent(SplashActivity.this, ProfileActivity.class);
        intent.putExtra(Constants.EXTRA_FORCE_PROFILE_SETUP, true);
        launchAndFinish(intent);
    }

    private void launchAndFinish(Intent intent) {
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private boolean isActivityInactive() {
        return isFinishing() || isDestroyed();
    }
}
