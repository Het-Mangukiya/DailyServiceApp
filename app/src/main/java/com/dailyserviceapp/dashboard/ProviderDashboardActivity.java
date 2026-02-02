package com.dailyserviceapp.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.billing.BillListActivity;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.service.ServiceEntryActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
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
    
    /**
     * Initializes the provider dashboard: inflates the layout, validates login and provider ID, initializes Firestore,
     * binds views, sets up listeners, and begins loading dashboard data. If the user is not logged in or the provider ID
     * is missing, navigates to the login screen and aborts initialization.
     *
     * @param savedInstanceState bundle containing the activity's previously saved state, if any
     */
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
    
    /**
     * Bind activity UI components to their layout views and configure the toolbar.
     *
     * Initializes fields for today's summary, payment overview, monthly overview, and quick-action buttons,
     * then sets the toolbar as the support action bar and enables the Up (back) button when available.
     */
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        
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
        
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    /**
     * Attach click handlers for the toolbar and dashboard quick-action buttons.
     *
     * Sets the toolbar's navigation to go back, and maps:
     * - Service Entry button → ServiceEntryActivity
     * - Bills button → BillListActivity
     * - Customers button → DashboardActivity
     */
    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        
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
    
    /**
     * Starts loading all dashboard data and shows the progress indicator.
     *
     * Initiates parallel loading of today's summary, the payment overview, and the monthly overview
     * while displaying the loading UI.
     */
    private void loadDashboardData() {
        showLoading(true);
        
        // Load all data in parallel
        loadTodaysSummary();
        loadPaymentOverview();
        loadMonthlyOverview();
    }
    
    /**
     * Loads and displays today's delivery count and earnings for the current provider.
     *
     * Queries Firestore's customers collection to determine the total active customer count and queries delivered service entries dated today to compute today's delivered count and sum of earnings; updates txtTodayDelivered and txtTodayEarnings on the main thread and advances the activity's loading tracker via checkLoadingComplete(). On query failures it logs the error and updates the UI with sensible default values.
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
                
                // Query service entries for today
                firestore.collection("serviceEntries")
                    .whereEqualTo("providerId", providerId)
                    .whereEqualTo("delivered", true)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        int deliveredCount = 0;
                        double todayEarnings = 0.0;
                        
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
                                    
                                    if (rate != null && quantity != null) {
                                        todayEarnings += (rate * quantity);
                                    }
                                }
                            }
                        }
                        
                        // Update UI
                        int finalDeliveredCount = deliveredCount;
                        double finalTodayEarnings = todayEarnings;
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
     * Aggregate customer lent amounts and provider payments, compute the pending balance, and update the dashboard UI.
     *
     * <p>Queries the customers collection to sum `lentAmount` and the payments collection to sum `amount`, computes
     * pending as (total lent − total received), and updates txtTotalLent, txtTotalReceived, and txtPendingAmount
     * with values formatted for Indian currency. On query failures, populates the UI with appropriate default values
     * and signals completion via {@code checkLoadingComplete()}.</p>
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
     * Loads and computes the current calendar month's deliveries and earnings from the service entries collection.
     *
     * Queries the provider's delivered service entries for the current month, computes the total number of deliveries
     * and the sum of (rate × quantity) as monthly earnings, updates txtMonthlyDeliveries and txtMonthlyEarnings,
     * and signals completion via checkLoadingComplete().
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
                android.util.Log.e("Dashboard", "Error loading monthly overview: " + e.getMessage());
                runOnUiThread(() -> {
                    txtMonthlyDeliveries.setText("0");
                    txtMonthlyEarnings.setText(CurrencyUtils.formatIndianCurrency(0.0));
                    checkLoadingComplete();
                });
            });
    }
    
    private int loadingTasks = 0;
    private final int TOTAL_TASKS = 3; /**
     * Tracks completed dashboard loading subtasks and hides the progress indicator when all tasks finish.
     *
     * Increments the internal completed-task counter and calls showLoading(false) once the counter
     * reaches or exceeds TOTAL_TASKS.
     */
    
    private void checkLoadingComplete() {
        loadingTasks++;
        if (loadingTasks >= TOTAL_TASKS) {
            showLoading(false);
        }
    }
    
    /**
     * Toggle the dashboard's loading indicator visibility.
     *
     * @param show true to display the loading indicator, false to hide it
     */
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    /**
     * Refreshes dashboard data when the activity becomes visible again.
     *
     * Resets the internal loading task counter and triggers a reload of all dashboard metrics.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this screen
        loadingTasks = 0;
        loadDashboardData();
    }
}