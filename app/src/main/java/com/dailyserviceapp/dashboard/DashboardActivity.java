package com.dailyserviceapp.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.auth.LoginActivity;
import com.dailyserviceapp.billing.BillListActivity;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.payment.PaymentActivity;
import com.dailyserviceapp.profile.ProfileActivity;
import com.dailyserviceapp.reports.ReportsActivity;
import com.dailyserviceapp.service.ServiceEntryActivity;
import com.dailyserviceapp.ui.CustomerListActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * Main Dashboard Activity for DailyDrop application.
 * Displays key statistics and provides navigation to all major features.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Modern toolbar with action icons</li>
 *   <li>Statistics cards showing total customers and revenue</li>
 *   <li>Search bar for quick navigation</li>
 *   <li>Feature cards grid for Customers, Service Entry, Bills, Payments, Reports, Profile</li>
 *   <li>Real-time data loading from Firestore</li>
 * </ul>
 * 
 * @author DailyDrop Team
 * @version 2.0
 * @since 2026-01-09
 */
public class DashboardActivity extends BaseActivity {
    
    /** Dashboard title */
    private TextView dashboardTitle;
    
    /** Statistics text views */
    private TextView totalCustomersCount, totalRevenueAmount;
    
    /** Navigation cards for main features */
    private CardView customersCard, serviceEntryCard, billsCard, paymentsCard, reportsCard, profileCard;
    
    /** Firestore database instance */
    private FirebaseFirestore firestore;
    
    /** Current logged-in user ID */
    private String currentUserId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        if (!isLoggedIn()) {
            navigateToLogin();
            return;
        }
        
        currentUserId = getCurrentUserId();
        firestore = FirebaseFirestore.getInstance();
        
        initializeViews();
        setupClickListeners();
        loadDashboardData();
    }
    
    private void initializeViews() {
        dashboardTitle = findViewById(R.id.dashboardTitle);
        totalCustomersCount = findViewById(R.id.totalCustomersCount);
        totalRevenueAmount = findViewById(R.id.totalRevenueAmount);
        
        customersCard = findViewById(R.id.customersCard);
        serviceEntryCard = findViewById(R.id.serviceEntryCard);
        billsCard = findViewById(R.id.billsCard);
        paymentsCard = findViewById(R.id.paymentsCard);
        reportsCard = findViewById(R.id.reportsCard);
        profileCard = findViewById(R.id.profileCard);
    }
    
    private void setupClickListeners() {
        customersCard.setOnClickListener(v -> {
            startActivity(new Intent(this, CustomerListActivity.class));
        });
        
        serviceEntryCard.setOnClickListener(v -> {
            startActivity(new Intent(this, ServiceEntryActivity.class));
        });
        
        billsCard.setOnClickListener(v -> {
            startActivity(new Intent(this, BillListActivity.class));
        });
        
        paymentsCard.setOnClickListener(v -> {
            startActivity(new Intent(this, PaymentActivity.class));
        });
        
        reportsCard.setOnClickListener(v -> {
            startActivity(new Intent(this, ReportsActivity.class));
        });
        
        profileCard.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });
    }
    
    private void loadDashboardData() {
        if (isProvider()) {
            loadProviderStats();
        } else {
            loadCustomerStats();
        }
    }
    
    private void loadProviderStats() {
        // Load total customers
        firestore.collection(Constants.COLLECTION_CUSTOMERS)
            .whereEqualTo("providerId", currentUserId)
            .whereEqualTo("status", "ACTIVE")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                int totalCustomers = querySnapshot.size();
                totalCustomersCount.setText(String.valueOf(totalCustomers));
            });
        
        // Load total revenue from bills
        firestore.collection(Constants.COLLECTION_BILLS)
            .whereEqualTo("providerId", currentUserId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                double totalRevenue = 0;
                for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Double amount = doc.getDouble("totalAmount");
                    if (amount != null) {
                        totalRevenue += amount;
                    }
                }
                totalRevenueAmount.setText("₹" + String.format("%.0f", totalRevenue));
            });
    }
    
    private void loadCustomerStats() {
        // Customer-specific stats
        totalCustomersCount.setText("N/A");
        totalRevenueAmount.setText("₹0");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void logout() {
        FirebaseAuth.getInstance().signOut();
        preferenceManager.clearAllData();
        navigateToLogin();
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
