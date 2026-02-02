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
import com.dailyserviceapp.core.utils.CurrencyUtils;
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
    
    private FirebaseFirestore firestore;
    private String providerId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_dashboard);
        
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
        loadDashboardData();
    }
    
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        
        // Today's Summary
        txtTodayDelivered = findViewById(R.id.txtTodayDelivered);
        txtTodayEarnings = findViewById(R.id.txtTodayEarnings);
        
        // Payment Overview
        txtTotalLent = findViewById(R.id.txtTotalLent);
        txtTotalReceived = findViewById(R.id.txtTotalReceived);
        txtPendingAmount = findViewById(R.id.txtPendingAmount);
        
        // Monthly Overview
        txtMonthlyEarnings = findViewById(R.id.txtMonthlyEarnings);
        txtMonthlyDeliveries = findViewById(R.id.txtMonthlyDeliveries);
        
        // Quick Actions
        btnServiceEntry = findViewById(R.id.btnServiceEntry);
        btnBills = findViewById(R.id.btnBills);
        btnCustomers = findViewById(R.id.btnCustomers);
        
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
            } else if (itemId == R.id.nav_bills) {
                startActivity(new Intent(this, BillListActivity.class));
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
        
        Calendar todayEnd = (Calendar) today.clone();
        todayEnd.set(Calendar.HOUR_OF_DAY, 23);
        todayEnd.set(Calendar.MINUTE, 59);
        todayEnd.set(Calendar.SECOND, 59);
        
        Timestamp startOfDay = new Timestamp(today.getTime());
        Timestamp endOfDay = new Timestamp(todayEnd.getTime());
        
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
                
                // Query service entries for today
                firestore.collection("serviceEntries")
                    .whereEqualTo("providerId", providerId)
                    .whereEqualTo("delivered", true)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        int deliveredCount = 0;
                        double todayEarnings = 0.0;
                        
                        android.util.Log.d(TAG, "=== TODAY'S EARNINGS DEBUG ===");
                        android.util.Log.d(TAG, "Total service entries: " + querySnapshot.size());
                        android.util.Log.d(TAG, "Customer rates available: " + customerRates.size());
                        android.util.Log.d(TAG, "Today range: " + startOfDay.toDate() + " to " + endOfDay.toDate());
                        
                        // Filter by today's date in memory
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Timestamp entryDate = doc.getTimestamp("date");
                            if (entryDate != null) {
                                Date entryDateObj = entryDate.toDate();
                                
                                // Check if entry is today
                                if (!entryDateObj.before(startOfDay.toDate()) && 
                                    !entryDateObj.after(endOfDay.toDate())) {
                                    deliveredCount++;
                                    
                                    // Calculate earnings (rate × quantity)
                                    Double rate = doc.getDouble("rate");
                                    Double quantity = doc.getDouble("quantity");
                                    String customerId = doc.getString("customerId");
                                    
                                    android.util.Log.d(TAG, "Entry - customerId: " + customerId + ", rate: " + rate + ", quantity: " + quantity);
                                    
                                    // Fallback to customer rate if entry doesn't have rate or rate is 0
                                    if ((rate == null || rate == 0.0) && customerId != null) {
                                        rate = customerRates.get(customerId);
                                        android.util.Log.d(TAG, "Using fallback rate: " + rate);
                                    }
                                    
                                    if (rate != null && quantity != null) {
                                        double earning = rate * quantity;
                                        todayEarnings += earning;
                                        android.util.Log.d(TAG, "✓ Earning: ₹" + earning + " (rate: " + rate + " × qty: " + quantity + ")");
                                    } else {
                                        android.util.Log.w(TAG, "✗ Skipped - rate or quantity null");
                                    }
                                }
                            }
                        }
                        
                        // Update UI
                        int finalDeliveredCount = deliveredCount;
                        double finalTodayEarnings = todayEarnings;
                        android.util.Log.d(TAG, "=== FINAL TODAY'S RESULT ===");
                        android.util.Log.d(TAG, "Delivered: " + finalDeliveredCount + " / " + totalCustomers);
                        android.util.Log.d(TAG, "Total Earnings: ₹" + finalTodayEarnings);
                        
                        runOnUiThread(() -> {
                            txtTodayDelivered.setText(finalDeliveredCount + " / " + totalCustomers);
                            txtTodayEarnings.setText(CurrencyUtils.formatIndianCurrency(finalTodayEarnings));
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
                            txtTotalLent.setText(CurrencyUtils.formatIndianCurrency(finalTotalLent));
                            txtTotalReceived.setText(CurrencyUtils.formatIndianCurrency(finalTotalReceived));
                            txtPendingAmount.setText(CurrencyUtils.formatIndianCurrency(pendingAmount));
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
        
        Calendar monthEnd = Calendar.getInstance();
        monthEnd.set(Calendar.DAY_OF_MONTH, monthEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
        monthEnd.set(Calendar.HOUR_OF_DAY, 23);
        monthEnd.set(Calendar.MINUTE, 59);
        monthEnd.set(Calendar.SECOND, 59);
        
        Timestamp startOfMonth = new Timestamp(monthStart.getTime());
        Timestamp endOfMonth = new Timestamp(monthEnd.getTime());
        
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
                
                // Query service entries for this month
                firestore.collection(COLLECTION_SERVICE_ENTRIES)
                    .whereEqualTo(FIELD_PROVIDER_ID, providerId)
                    .whereEqualTo("delivered", true)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        int monthlyDeliveries = 0;
                        double monthlyEarnings = 0.0;
                        
                        // Filter by month date range in memory
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Timestamp entryDate = doc.getTimestamp("date");
                            if (entryDate != null) {
                                Date entryDateObj = entryDate.toDate();
                                
                                // Check if entry is this month
                                if (!entryDateObj.before(startOfMonth.toDate()) && 
                                    !entryDateObj.after(endOfMonth.toDate())) {
                                    monthlyDeliveries++;
                                    
                                    // Calculate earnings (rate × quantity)
                                    Double rate = doc.getDouble("rate");
                                    Double quantity = doc.getDouble("quantity");
                                    String customerId = doc.getString("customerId");
                                    
                                    // Fallback to customer rate if entry doesn't have rate or rate is 0
                                    if ((rate == null || rate == 0.0) && customerId != null) {
                                        rate = customerRates.get(customerId);
                                    }
                                    
                                    if (rate != null && quantity != null) {
                                        monthlyEarnings += (rate * quantity);
                                    }
                        }
                    }
                }
                
                // Update UI
                int finalMonthlyDeliveries = monthlyDeliveries;
                double finalMonthlyEarnings = monthlyEarnings;
                runOnUiThread(() -> {
                    txtMonthlyDeliveries.setText(String.valueOf(finalMonthlyDeliveries));
                    txtMonthlyEarnings.setText(CurrencyUtils.formatIndianCurrency(finalMonthlyEarnings));
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
    
    private int loadingTasks = 0;
    private final int TOTAL_TASKS = 3; // Today, Payment, Monthly
    
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
}
