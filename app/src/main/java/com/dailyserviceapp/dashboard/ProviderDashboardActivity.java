package com.dailyserviceapp.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.dailyserviceapp.R;
import com.dailyserviceapp.billing.BillListActivity;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.AvatarUtils;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.databinding.ActivityProviderDashboardBinding;
import com.dailyserviceapp.databinding.NavHeaderBinding;
import com.dailyserviceapp.maps.RouteOptimizationActivity;
import com.dailyserviceapp.profile.ProfileActivity;
import com.dailyserviceapp.provider.JoinRequestsActivity;
import com.dailyserviceapp.provider.ProviderComplaintsActivity;
import com.dailyserviceapp.provider.QuantityRequestsActivity;
import com.dailyserviceapp.service.ServiceEntryActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import androidx.lifecycle.ViewModelProvider;

import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

/**
 * Provider Dashboard Activity - Professional analytics dashboard for service providers.
 * 
 * Shows real-time:
 * - Today's deliveries and earnings
 * - Payment overview (total lent, received, pending)
 * - Monthly statistics
 * - Quick action buttons
 * 
 * All calculations are data-driven from Firestore collections:
 * - serviceEntries: For deliveries and earnings
 * - customers: For lent amounts
 * - payments: For received amounts
 * 
 * @author DailyDrop Team
 * @version 2.0
 * @since 2026-02-02
 */
@AndroidEntryPoint
public class ProviderDashboardActivity extends BaseActivity {

    private ActivityProviderDashboardBinding binding;
    
    // Constants for better maintainability
    private static final String TAG = "ProviderDashboard";
    private static final String FIELD_PROVIDER_ID = "providerId";
    private static final String FIELD_STATUS = "status";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String COLLECTION_SERVICE_ENTRIES = "serviceEntries";
    private static final String COLLECTION_CUSTOMERS = "customers";
    private static final String COLLECTION_PAYMENTS = "payments";
    private static final double DEFAULT_AMOUNT = 0.0;
    
    private MaterialToolbar toolbar;
    private ProgressBar progressBar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    
    // Today's Summary
    private TextView txtTodayDelivered;
    private TextView txtTodayEarnings;
    
    // Payment Overview
    private TextView txtTotalLent;
    private TextView txtTotalReceived;
    private TextView txtPendingAmount;
    
    // Monthly Overview
    private TextView txtMonthlyEarnings;
    private TextView txtMonthlyDeliveries;
    
    // Quick Actions
    private MaterialButton btnServiceEntry;
    private MaterialButton btnBills;
    private MaterialButton btnCustomers;
    private MaterialButton btnJoinRequests;
    
    @Inject
    FirebaseFirestore firestore;
    private String providerId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProviderDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        if (!isLoggedIn()) {
            navigateToLogin();
            return;
        }
        
        providerId = getCurrentUserId();
        if (providerId == null || providerId.isEmpty()) {
            showToast("User ID not found. Please login again.");
            navigateToLogin();
            return;
        }
        
        // firestore is injected by Hilt
        
        initializeViews();
        setupListeners();
        loadCachedProviderMetrics();
        loadDashboardData();
    }
    
    private void initializeViews() {
        toolbar = binding.toolbar;
        progressBar = binding.progressBar;
        drawerLayout = binding.drawerLayout;
        navigationView = binding.navigationView;
        
        // Today's Summary
        txtTodayDelivered = binding.txtTodayDelivered;
        txtTodayEarnings = binding.txtTodayEarnings;
        
        // Payment Overview
        txtTotalLent = binding.txtTotalLent;
        txtTotalReceived = binding.txtTotalReceived;
        txtPendingAmount = binding.txtPendingAmount;
        
        // Monthly Overview
        txtMonthlyEarnings = binding.txtMonthlyEarnings;
        txtMonthlyDeliveries = binding.txtMonthlyDeliveries;
        
        // Quick Actions
        btnServiceEntry = binding.btnServiceEntry;
        btnBills = binding.btnBills;
        btnCustomers = binding.btnCustomers;
        btnJoinRequests = binding.btnJoinRequests;

        // Pending Quantity Requests card
        binding.cardPendingRequests.setOnClickListener(v ->
            startActivity(new Intent(this, QuantityRequestsActivity.class)));
        binding.btnViewRequests.setOnClickListener(v ->
            startActivity(new Intent(this, QuantityRequestsActivity.class)));
        
        // Setup toolbar with drawer
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        
        // Setup drawer toggle
        drawerToggle = new ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        
        // Setup navigation menu
        setupNavigationMenu();
        setupNavigationHeader();
    }

    private void setupNavigationHeader() {
        View headerView = navigationView.getHeaderView(0);
        if (headerView == null) {
            return;
        }

        NavHeaderBinding headerBinding = NavHeaderBinding.bind(headerView);
        bindProviderHeader(headerBinding, null, null);

        firestore.collection(Constants.COLLECTION_PROVIDERS)
            .document(providerId)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot == null || !snapshot.exists()) {
                    return;
                }

                String profileName = snapshot.getString("businessName");
                if (profileName == null || profileName.trim().isEmpty()) {
                    profileName = snapshot.getString("name");
                }

                String profileEmail = snapshot.getString("email");
                bindProviderHeader(headerBinding, profileName, profileEmail);
            });
    }

    private void bindProviderHeader(NavHeaderBinding headerBinding, String profileName, String profileEmail) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        String authName = currentUser != null ? currentUser.getDisplayName() : null;
        String authEmail = currentUser != null ? currentUser.getEmail() : null;

        String prefName = preferenceManager != null ? preferenceManager.getUserName() : null;
        String prefEmail = preferenceManager != null ? preferenceManager.getUserEmail() : null;

        String displayName = AvatarUtils.resolveDisplayName(
            firstNonEmpty(profileName, authName, prefName),
            firstNonEmpty(profileEmail, authEmail, prefEmail),
            getString(R.string.default_user_name)
        );
        String displayEmail = firstNonEmpty(profileEmail, authEmail, prefEmail, getString(R.string.default_user_email));
        String initials = AvatarUtils.getInitials(displayName);

        headerBinding.userName.setText(displayName);
        headerBinding.userEmail.setText(displayEmail);
        headerBinding.userInitial.setText(initials);
        headerBinding.userInitial.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        headerBinding.userInitial.setContentDescription(getString(R.string.profile_initial_for, displayName));
        headerBinding.avatarCard.setCardBackgroundColor(AvatarUtils.getAvatarColor(displayName));

        View.OnClickListener openProfileListener = v -> startActivity(new Intent(this, ProfileActivity.class));
        headerBinding.avatarCard.setOnClickListener(openProfileListener);
        headerBinding.userInitial.setOnClickListener(openProfileListener);
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
    
    private void setupListeners() {
        btnServiceEntry.setOnClickListener(v -> {
            startActivity(new Intent(this, ServiceEntryActivity.class));
        });
        
        btnBills.setOnClickListener(v -> {
            startActivity(new Intent(this, BillListActivity.class));
        });
        
        btnCustomers.setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
        });

        btnJoinRequests.setOnClickListener(v -> {
            startActivity(new Intent(this, JoinRequestsActivity.class));
        });
    }
    
    private void setupNavigationMenu() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_dashboard) {
                // Already on dashboard, just close drawer
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (itemId == R.id.nav_service_entry) {
                startActivity(new Intent(this, ServiceEntryActivity.class));
            } else if (itemId == R.id.nav_customers) {
                startActivity(new Intent(this, DashboardActivity.class));
            } else if (itemId == R.id.nav_join_requests) {
                startActivity(new Intent(this, JoinRequestsActivity.class));
            } else if (itemId == R.id.nav_complaints) {
                startActivity(new Intent(this, ProviderComplaintsActivity.class));
            } else if (itemId == R.id.nav_quantity_requests) {
                startActivity(new Intent(this, QuantityRequestsActivity.class));
            } else if (itemId == R.id.nav_route) {
                startActivity(new Intent(this, RouteOptimizationActivity.class));
            } else if (itemId == R.id.nav_bills) {
                startActivity(new Intent(this, BillListActivity.class));
            } else if (itemId == R.id.nav_reports) {
                startActivity(new Intent(this, com.dailyserviceapp.reports.ReportsActivity.class));
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (itemId == R.id.nav_qr_code) {
                startActivity(new Intent(this, com.dailyserviceapp.qr.QRCodeActivity.class));
            } else if (itemId == R.id.nav_logout) {
                logout();
            }
            
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private ProviderDashboardViewModel viewModel;

    private void loadDashboardData() {
        if (viewModel == null) {
            viewModel = new ViewModelProvider(this).get(ProviderDashboardViewModel.class);
            setupObservers();
        }
        viewModel.loadDashboardData(providerId);
    }

    private void setupObservers() {
        viewModel.isLoading().observe(this, this::showLoading);
        
        viewModel.todayDelivered().observe(this, text -> {
            txtTodayDelivered.setText(text);
            cacheProviderValue(Constants.PREF_PROVIDER_TODAY_DELIVERED, text);
        });
        
        viewModel.todayEarnings().observe(this, text -> {
            txtTodayEarnings.setText(text);
            cacheProviderValue(Constants.PREF_PROVIDER_TODAY_EARNINGS, text);
        });
        
        viewModel.totalLent().observe(this, text -> {
            txtTotalLent.setText(text);
            cacheProviderValue(Constants.PREF_PROVIDER_TOTAL_LENT, text);
        });
        
        viewModel.totalReceived().observe(this, text -> {
            txtTotalReceived.setText(text);
            cacheProviderValue(Constants.PREF_PROVIDER_TOTAL_RECEIVED, text);
        });
        
        viewModel.pendingAmount().observe(this, text -> {
            txtPendingAmount.setText(text);
            cacheProviderValue(Constants.PREF_PROVIDER_PENDING_AMOUNT, text);
        });
        
        viewModel.monthlyEarnings().observe(this, text -> {
            txtMonthlyEarnings.setText(text);
            cacheProviderValue(Constants.PREF_PROVIDER_MONTHLY_EARNINGS, text);
        });
        
        viewModel.monthlyDeliveries().observe(this, text -> {
            txtMonthlyDeliveries.setText(text);
            cacheProviderValue(Constants.PREF_PROVIDER_MONTHLY_DELIVERIES, text);
        });
    }

    private void loadCachedProviderMetrics() {
        String todayDelivered = preferenceManager.getString(prefKey(Constants.PREF_PROVIDER_TODAY_DELIVERED), null);
        if (todayDelivered != null && txtTodayDelivered != null) {
            txtTodayDelivered.setText(todayDelivered);
        }

        String todayEarnings = preferenceManager.getString(prefKey(Constants.PREF_PROVIDER_TODAY_EARNINGS), null);
        if (todayEarnings != null && txtTodayEarnings != null) {
            txtTodayEarnings.setText(todayEarnings);
        }

        String totalLent = preferenceManager.getString(prefKey(Constants.PREF_PROVIDER_TOTAL_LENT), null);
        if (totalLent != null && txtTotalLent != null) {
            txtTotalLent.setText(totalLent);
        }

        String totalReceived = preferenceManager.getString(prefKey(Constants.PREF_PROVIDER_TOTAL_RECEIVED), null);
        if (totalReceived != null && txtTotalReceived != null) {
            txtTotalReceived.setText(totalReceived);
        }

        String pendingAmount = preferenceManager.getString(prefKey(Constants.PREF_PROVIDER_PENDING_AMOUNT), null);
        if (pendingAmount != null && txtPendingAmount != null) {
            txtPendingAmount.setText(pendingAmount);
        }

        String monthlyEarnings = preferenceManager.getString(prefKey(Constants.PREF_PROVIDER_MONTHLY_EARNINGS), null);
        if (monthlyEarnings != null && txtMonthlyEarnings != null) {
            txtMonthlyEarnings.setText(monthlyEarnings);
        }

        String monthlyDeliveries = preferenceManager.getString(prefKey(Constants.PREF_PROVIDER_MONTHLY_DELIVERIES), null);
        if (monthlyDeliveries != null && txtMonthlyDeliveries != null) {
            txtMonthlyDeliveries.setText(monthlyDeliveries);
        }
    }

    private void cacheProviderValue(String key, String value) {
        if (value == null) return;
        preferenceManager.putString(prefKey(key), value);
    }

    private String prefKey(String key) {
        if (providerId == null || providerId.isEmpty()) {
            return key;
        }
        return key + "_" + providerId;
    }
    
    private void logout() {
        // Clear session and navigate to login
        preferenceManager.clearAllData();
        navigateToLogin();
        finishAffinity();
    }
    
    private void showLoading(boolean show) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void runOnSafeUi(Runnable action) {
        if (action == null) return;
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            action.run();
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this screen
        loadDashboardData();
        loadPendingQuantityRequests();
    }

    private void loadPendingQuantityRequests() {
        if (providerId == null || providerId.isEmpty()) return;
        firestore.collection(Constants.COLLECTION_QUANTITY_REQUESTS)
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("status", "PENDING")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (isFinishing() || isDestroyed() || binding == null) return;
                int count = querySnapshot != null ? querySnapshot.size() : 0;
                if (count > 0) {
                    binding.cardPendingRequests.setVisibility(android.view.View.VISIBLE);
                    binding.txtPendingRequestCount.setText(
                        getString(R.string.pending_qty_requests_count, count));
                } else {
                    binding.cardPendingRequests.setVisibility(android.view.View.GONE);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load pending quantity requests", e);
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
