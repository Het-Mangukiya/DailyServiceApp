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

public class DashboardActivity extends BaseActivity {
    
    private TextView welcomeText;
    private TextView totalCustomersText, pendingDeliveriesText, outstandingAmountText;
    private CardView customersCard, serviceEntryCard, billsCard, paymentsCard, reportsCard;
    
    private FirebaseFirestore firestore;
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
        welcomeText = findViewById(R.id.welcomeText);
        totalCustomersText = findViewById(R.id.totalCustomersText);
        pendingDeliveriesText = findViewById(R.id.pendingDeliveriesText);
        outstandingAmountText = findViewById(R.id.outstandingAmountText);
        
        customersCard = findViewById(R.id.customersCard);
        serviceEntryCard = findViewById(R.id.serviceEntryCard);
        billsCard = findViewById(R.id.billsCard);
        paymentsCard = findViewById(R.id.paymentsCard);
        reportsCard = findViewById(R.id.reportsCard);
        
        String userName = preferenceManager.getUserName();
        welcomeText.setText("Welcome, " + (userName != null ? userName : "User"));
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
                totalCustomersText.setText(String.valueOf(totalCustomers));
            });
        
        // Load pending deliveries (today's entries not marked)
        // This would need more complex date filtering
        pendingDeliveriesText.setText("0");
        
        // Load outstanding amount
        firestore.collection(Constants.COLLECTION_BILLS)
            .whereEqualTo("providerId", currentUserId)
            .whereIn("paymentStatus", java.util.Arrays.asList("PENDING", "PARTIAL", "OVERDUE"))
            .get()
            .addOnSuccessListener(querySnapshot -> {
                double totalOutstanding = 0;
                for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Double amount = doc.getDouble("totalAmount");
                    if (amount != null) {
                        totalOutstanding += amount;
                    }
                }
                outstandingAmountText.setText("₹" + String.format("%.2f", totalOutstanding));
            });
    }
    
    private void loadCustomerStats() {
        // Customer-specific stats
        totalCustomersText.setText("N/A");
        pendingDeliveriesText.setText("N/A");
        outstandingAmountText.setText("₹0.00");
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
