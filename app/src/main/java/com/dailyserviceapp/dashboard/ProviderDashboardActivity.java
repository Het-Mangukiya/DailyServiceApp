package com.dailyserviceapp.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.dailyserviceapp.R;
import com.dailyserviceapp.billing.BillListActivity;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.databinding.ActivityProviderDashboardBinding;
import com.dailyserviceapp.profile.ProfileActivity;
import com.dailyserviceapp.provider.JoinRequestsActivity;
import com.dailyserviceapp.service.ServiceEntryActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.Date;

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
    
    private FirebaseFirestore firestore;
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
        
        firestore = FirebaseFirestore.getInstance();
        
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
            // This is the main landing page, exit app
            finishAffinity();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void loadDashboardData() {
        showLoading(true);
        
        // Load all data in parallel
        loadTodaysSummary();
        loadPaymentOverview();
        loadMonthlyOverview();
    }
    
    /**
     * Calculate today's deliveries and earnings from service_entries collection.
     * 
     * Logic:
     * - Query all service entries for this provider where delivered = true
     * - Filter by today's date
     * - Count deliveries
     * - Sum (rate × quantity) for today's earnings
     */
    private void loadTodaysSummary() {
        // Get today's date range
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        
        Timestamp startOfDay = new Timestamp(today.getTime());
        Timestamp endExclusive = new Timestamp(tomorrow.getTime());
        
        // Get total customer count for "X / Total" display
        firestore.collection("customers")
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("status", "ACTIVE")
            .get()
            .addOnSuccessListener(customerSnapshot -> {
                int totalCustomers = customerSnapshot.size();
                
                // Build customer rate map for fallback
                java.util.Map<String, Double> customerRates = new java.util.HashMap<>();
                for (QueryDocumentSnapshot customerDoc : customerSnapshot) {
                    String customerId = customerDoc.getId();
                    Double rate = customerDoc.getDouble("ratePerUnit");
                    if (rate != null) {
                        customerRates.put(customerId, rate);
                    }
                }
                
                loadTodaysEntriesOptimized(totalCustomers, customerRates, startOfDay, endExclusive);
            })
            .addOnFailureListener(e -> {
                android.util.Log.e(TAG, "Error loading customers", e);
                runOnUiThread(() -> {
                    txtTodayDelivered.setText("0 / 0");
                    txtTodayEarnings.setText(CurrencyUtils.formatIndianCurrency(DEFAULT_AMOUNT));
                    checkLoadingComplete();
                });
            });
    }
    
    /**
     * Calculate payment overview from customers and payments collections.
     * 
     * Logic:
     * - Total Lent = Sum of all customer lentAmount fields
     * - Total Received = Sum of all payment amounts
     * - Pending Amount = Total Lent - Total Received
     */
    private void loadPaymentOverview() {
        // Calculate total lent from customers
        firestore.collection(COLLECTION_CUSTOMERS)
            .whereEqualTo(FIELD_PROVIDER_ID, providerId)
            .whereEqualTo(FIELD_STATUS, STATUS_ACTIVE)
            .get()
            .addOnSuccessListener(customerSnapshot -> {
                double totalLent = 0.0;
                
                for (QueryDocumentSnapshot doc : customerSnapshot) {
                    Double lentAmount = doc.getDouble("lentAmount");
                    if (lentAmount != null) {
                        totalLent += lentAmount;
                    }
                }
                
                double finalTotalLent = totalLent;
                
                // Calculate total received from payments
                firestore.collection(COLLECTION_PAYMENTS)
                    .whereEqualTo(FIELD_PROVIDER_ID, providerId)
                    .get()
                    .addOnSuccessListener(paymentSnapshot -> {
                        double totalReceived = 0.0;
                        
                        for (QueryDocumentSnapshot doc : paymentSnapshot) {
                            Double amount = doc.getDouble("amount");
                            if (amount != null) {
                                totalReceived += amount;
                            }
                        }
                        
                        // Calculate pending
                        double finalTotalReceived = totalReceived;
                        double pendingAmount = finalTotalLent - finalTotalReceived;
                        
                        // Update UI
                        runOnUiThread(() -> {
                            String lentText = CurrencyUtils.formatIndianCurrency(finalTotalLent);
                            String receivedText = CurrencyUtils.formatIndianCurrency(finalTotalReceived);
                            String pendingText = CurrencyUtils.formatIndianCurrency(pendingAmount);
                            txtTotalLent.setText(lentText);
                            txtTotalReceived.setText(receivedText);
                            txtPendingAmount.setText(pendingText);
                            cacheProviderValue(Constants.PREF_PROVIDER_TOTAL_LENT, lentText);
                            cacheProviderValue(Constants.PREF_PROVIDER_TOTAL_RECEIVED, receivedText);
                            cacheProviderValue(Constants.PREF_PROVIDER_PENDING_AMOUNT, pendingText);
                            checkLoadingComplete();
                        });
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e(TAG, "Error loading payments", e);
                        runOnUiThread(() -> {
                            txtTotalLent.setText(CurrencyUtils.formatIndianCurrency(finalTotalLent));
                            txtTotalReceived.setText(CurrencyUtils.formatIndianCurrency(DEFAULT_AMOUNT));
                            txtPendingAmount.setText(CurrencyUtils.formatIndianCurrency(finalTotalLent));
                            checkLoadingComplete();
                        });
                    });
            })
            .addOnFailureListener(e -> {
                android.util.Log.e(TAG, "Error loading customers for payment", e);
                runOnUiThread(() -> {
                    txtTotalLent.setText(CurrencyUtils.formatIndianCurrency(DEFAULT_AMOUNT));
                    txtTotalReceived.setText(CurrencyUtils.formatIndianCurrency(DEFAULT_AMOUNT));
                    txtPendingAmount.setText(CurrencyUtils.formatIndianCurrency(DEFAULT_AMOUNT));
                    checkLoadingComplete();
                });
            });
    }
    
    /**
     * Calculate monthly overview from service_entries collection.
     * 
     * Logic:
     * - Query all service entries for current month
     * - Count total deliveries
     * - Sum (rate × quantity) for monthly earnings
     */
    private void loadMonthlyOverview() {
        // Get current month date range
        Calendar monthStart = Calendar.getInstance();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        monthStart.set(Calendar.HOUR_OF_DAY, 0);
        monthStart.set(Calendar.MINUTE, 0);
        monthStart.set(Calendar.SECOND, 0);
        monthStart.set(Calendar.MILLISECOND, 0);
        
        Timestamp startOfMonth = new Timestamp(monthStart.getTime());
        Calendar nextMonth = (Calendar) monthStart.clone();
        nextMonth.add(Calendar.MONTH, 1);
        Timestamp endExclusive = new Timestamp(nextMonth.getTime());
        
        // Get customer rates for fallback
        firestore.collection(COLLECTION_CUSTOMERS)
            .whereEqualTo(FIELD_PROVIDER_ID, providerId)
            .whereEqualTo(FIELD_STATUS, STATUS_ACTIVE)
            .get()
            .addOnSuccessListener(customerSnapshot -> {
                // Build customer rate map for fallback
                java.util.Map<String, Double> customerRates = new java.util.HashMap<>();
                for (QueryDocumentSnapshot customerDoc : customerSnapshot) {
                    String customerId = customerDoc.getId();
                    Double rate = customerDoc.getDouble("ratePerUnit");
                    if (rate != null) {
                        customerRates.put(customerId, rate);
                    }
                }
                
                loadMonthlyEntriesOptimized(customerRates, startOfMonth, endExclusive);
        })
        .addOnFailureListener(e -> {
            android.util.Log.e(TAG, "Error loading customers for monthly", e);
            runOnUiThread(() -> {
                txtMonthlyDeliveries.setText("0");
                txtMonthlyEarnings.setText(CurrencyUtils.formatIndianCurrency(DEFAULT_AMOUNT));
                checkLoadingComplete();
            });
        });
    }

    private void loadTodaysEntriesOptimized(int totalCustomers, java.util.Map<String, Double> customerRates,
                                            Timestamp startOfDay, Timestamp endExclusive) {
        firestore.collection(COLLECTION_SERVICE_ENTRIES)
            .whereEqualTo(FIELD_PROVIDER_ID, providerId)
            .whereEqualTo("delivered", true)
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThan("date", endExclusive)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                int deliveredCount = 0;
                double todayEarnings = 0.0;

                for (QueryDocumentSnapshot doc : querySnapshot) {
                    deliveredCount++;
                    Double rate = doc.getDouble("rate");
                    Double quantity = doc.getDouble("quantity");
                    String customerId = doc.getString("customerId");

                    if ((rate == null || rate == 0.0) && customerId != null) {
                        rate = customerRates.get(customerId);
                    }
                    if (rate != null && quantity != null) {
                        todayEarnings += (rate * quantity);
                    }
                }

                int finalDeliveredCount = deliveredCount;
                double finalTodayEarnings = todayEarnings;
                runOnUiThread(() -> {
                    String deliveredText = finalDeliveredCount + " / " + totalCustomers;
                    String earningsText = CurrencyUtils.formatIndianCurrency(finalTodayEarnings);
                    txtTodayDelivered.setText(deliveredText);
                    txtTodayEarnings.setText(earningsText);
                    cacheProviderValue(Constants.PREF_PROVIDER_TODAY_DELIVERED, deliveredText);
                    cacheProviderValue(Constants.PREF_PROVIDER_TODAY_EARNINGS, earningsText);
                    checkLoadingComplete();
                });
            })
            .addOnFailureListener(e -> {
                android.util.Log.w(TAG, "Optimized today query failed, falling back", e);
                loadTodaysEntriesFallback(totalCustomers, customerRates, startOfDay, endExclusive);
            });
    }

    private void loadTodaysEntriesFallback(int totalCustomers, java.util.Map<String, Double> customerRates,
                                           Timestamp startOfDay, Timestamp endExclusive) {
        firestore.collection(COLLECTION_SERVICE_ENTRIES)
            .whereEqualTo(FIELD_PROVIDER_ID, providerId)
            .whereEqualTo("delivered", true)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                int deliveredCount = 0;
                double todayEarnings = 0.0;

                long startTime = startOfDay.toDate().getTime();
                long endTime = endExclusive.toDate().getTime();

                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Timestamp entryDate = doc.getTimestamp("date");
                    if (entryDate == null) continue;
                    long entryTime = entryDate.toDate().getTime();
                    if (entryTime >= startTime && entryTime < endTime) {
                        deliveredCount++;
                        Double rate = doc.getDouble("rate");
                        Double quantity = doc.getDouble("quantity");
                        String customerId = doc.getString("customerId");

                        if ((rate == null || rate == 0.0) && customerId != null) {
                            rate = customerRates.get(customerId);
                        }
                        if (rate != null && quantity != null) {
                            todayEarnings += (rate * quantity);
                        }
                    }
                }

                int finalDeliveredCount = deliveredCount;
                double finalTodayEarnings = todayEarnings;
                runOnUiThread(() -> {
                    String deliveredText = finalDeliveredCount + " / " + totalCustomers;
                    String earningsText = CurrencyUtils.formatIndianCurrency(finalTodayEarnings);
                    txtTodayDelivered.setText(deliveredText);
                    txtTodayEarnings.setText(earningsText);
                    cacheProviderValue(Constants.PREF_PROVIDER_TODAY_DELIVERED, deliveredText);
                    cacheProviderValue(Constants.PREF_PROVIDER_TODAY_EARNINGS, earningsText);
                    checkLoadingComplete();
                });
            })
            .addOnFailureListener(e -> {
                android.util.Log.e(TAG, "Error loading today's summary", e);
                runOnUiThread(() -> {
                    txtTodayDelivered.setText("0 / " + totalCustomers);
                    txtTodayEarnings.setText(CurrencyUtils.formatIndianCurrency(DEFAULT_AMOUNT));
                    checkLoadingComplete();
                });
            });
    }

    private void loadMonthlyEntriesOptimized(java.util.Map<String, Double> customerRates,
                                             Timestamp startOfMonth, Timestamp endExclusive) {
        firestore.collection(COLLECTION_SERVICE_ENTRIES)
            .whereEqualTo(FIELD_PROVIDER_ID, providerId)
            .whereEqualTo("delivered", true)
            .whereGreaterThanOrEqualTo("date", startOfMonth)
            .whereLessThan("date", endExclusive)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                int monthlyDeliveries = 0;
                double monthlyEarnings = 0.0;

                for (QueryDocumentSnapshot doc : querySnapshot) {
                    monthlyDeliveries++;
                    Double rate = doc.getDouble("rate");
                    Double quantity = doc.getDouble("quantity");
                    String customerId = doc.getString("customerId");

                    if ((rate == null || rate == 0.0) && customerId != null) {
                        rate = customerRates.get(customerId);
                    }
                    if (rate != null && quantity != null) {
                        monthlyEarnings += (rate * quantity);
                    }
                }

                int finalMonthlyDeliveries = monthlyDeliveries;
                double finalMonthlyEarnings = monthlyEarnings;
                runOnUiThread(() -> {
                    String deliveriesText = String.valueOf(finalMonthlyDeliveries);
                    String earningsText = CurrencyUtils.formatIndianCurrency(finalMonthlyEarnings);
                    txtMonthlyDeliveries.setText(deliveriesText);
                    txtMonthlyEarnings.setText(earningsText);
                    cacheProviderValue(Constants.PREF_PROVIDER_MONTHLY_DELIVERIES, deliveriesText);
                    cacheProviderValue(Constants.PREF_PROVIDER_MONTHLY_EARNINGS, earningsText);
                    checkLoadingComplete();
                });
            })
            .addOnFailureListener(e -> {
                android.util.Log.w(TAG, "Optimized monthly query failed, falling back", e);
                loadMonthlyEntriesFallback(customerRates, startOfMonth, endExclusive);
            });
    }

    private void loadMonthlyEntriesFallback(java.util.Map<String, Double> customerRates,
                                            Timestamp startOfMonth, Timestamp endExclusive) {
        firestore.collection(COLLECTION_SERVICE_ENTRIES)
            .whereEqualTo(FIELD_PROVIDER_ID, providerId)
            .whereEqualTo("delivered", true)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                int monthlyDeliveries = 0;
                double monthlyEarnings = 0.0;

                long startTime = startOfMonth.toDate().getTime();
                long endTime = endExclusive.toDate().getTime();

                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Timestamp entryDate = doc.getTimestamp("date");
                    if (entryDate == null) continue;
                    long entryTime = entryDate.toDate().getTime();
                    if (entryTime >= startTime && entryTime < endTime) {
                        monthlyDeliveries++;
                        Double rate = doc.getDouble("rate");
                        Double quantity = doc.getDouble("quantity");
                        String customerId = doc.getString("customerId");

                        if ((rate == null || rate == 0.0) && customerId != null) {
                            rate = customerRates.get(customerId);
                        }
                        if (rate != null && quantity != null) {
                            monthlyEarnings += (rate * quantity);
                        }
                    }
                }

                int finalMonthlyDeliveries = monthlyDeliveries;
                double finalMonthlyEarnings = monthlyEarnings;
                runOnUiThread(() -> {
                    String deliveriesText = String.valueOf(finalMonthlyDeliveries);
                    String earningsText = CurrencyUtils.formatIndianCurrency(finalMonthlyEarnings);
                    txtMonthlyDeliveries.setText(deliveriesText);
                    txtMonthlyEarnings.setText(earningsText);
                    cacheProviderValue(Constants.PREF_PROVIDER_MONTHLY_DELIVERIES, deliveriesText);
                    cacheProviderValue(Constants.PREF_PROVIDER_MONTHLY_EARNINGS, earningsText);
                    checkLoadingComplete();
                });
            })
            .addOnFailureListener(e -> {
                android.util.Log.e(TAG, "Error loading service entries for monthly", e);
                runOnUiThread(() -> {
                    txtMonthlyDeliveries.setText("0");
                    txtMonthlyEarnings.setText(CurrencyUtils.formatIndianCurrency(DEFAULT_AMOUNT));
                    checkLoadingComplete();
                });
            });
    }
    
    private int loadingTasks = 0;
    private final int TOTAL_TASKS = 3; // Today, Payment, Monthly

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
    
    private void checkLoadingComplete() {
        loadingTasks++;
        if (loadingTasks >= TOTAL_TASKS) {
            showLoading(false);
        }
    }
    
    private void logout() {
        // Clear session and navigate to login
        com.dailyserviceapp.core.utils.PreferenceManager prefManager = 
            new com.dailyserviceapp.core.utils.PreferenceManager(this);
        prefManager.clearAllData();
        navigateToLogin();
        finishAffinity();
    }
    
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this screen
        loadingTasks = 0;
        loadDashboardData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
