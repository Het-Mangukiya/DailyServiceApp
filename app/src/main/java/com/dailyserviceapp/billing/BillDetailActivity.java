package com.dailyserviceapp.billing;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Bill;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.payment.PaymentActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

/**
 * Bill Detail Activity for viewing detailed bill information.
 * Shows customer info, bill items, total amount, and payment options.
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-09
 */
public class BillDetailActivity extends BaseActivity {
    
    private FirestoreRepository repository;
    private String billId;
    private Bill currentBill;
    
    private TextView customerNameText;
    private TextView billPeriodText;
    private TextView daysServedText;
    private TextView totalAmountText;
    private Chip paymentStatusChip;
    private MaterialButton markAsPaidButton;
    private MaterialButton shareBillButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_detail);
        
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, "Bill Details", true);
        
        initializeViews();
        initializeData();
        loadBillDetails();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload bill details when returning from PaymentActivity
        if (billId != null && !billId.isEmpty()) {
            loadBillDetails();
        }
    }
    
    private void initializeViews() {
        customerNameText = findViewById(R.id.customerName);
        billPeriodText = findViewById(R.id.billPeriod);
        daysServedText = findViewById(R.id.daysServed);
        totalAmountText = findViewById(R.id.totalAmount);
        paymentStatusChip = findViewById(R.id.paymentStatusChip);
        markAsPaidButton = findViewById(R.id.markAsPaidButton);
        shareBillButton = findViewById(R.id.shareBillButton);
    }
    
    private void initializeData() {
        repository = new FirestoreRepository();
        billId = getIntent().getStringExtra("billId");
        
        if (billId == null || billId.isEmpty()) {
            showToast("Invalid bill ID");
            finish();
            return;
        }
        
        markAsPaidButton.setOnClickListener(v -> markBillAsPaid());
        shareBillButton.setOnClickListener(v -> shareBill());
    }
    
    private void loadBillDetails() {
        repository.getBillById(billId, new FirestoreRepository.OnBillLoadedListener() {
            @Override
            public void onBillLoaded(Bill bill) {
                currentBill = bill;
                displayBillInfo(bill);
                loadCustomerInfo(bill.getCustomerId());
            }

            @Override
            public void onError(String error) {
                showToast("Error loading bill: " + error);
                finish();
            }
        });
    }
    
    private void displayBillInfo(Bill bill) {
        // Set bill period
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                               "July", "August", "September", "October", "November", "December"};
        billPeriodText.setText(monthNames[bill.getMonth()] + " " + bill.getYear());
        
        // Set days served and amount
        daysServedText.setText(String.valueOf(bill.getDaysServed()));
        totalAmountText.setText(CurrencyUtils.formatCurrency(bill.getTotalAmount()));
        
        // Set payment status
        String status = bill.getPaymentStatus() != null ? bill.getPaymentStatus() : "PENDING";
        paymentStatusChip.setText(status);
        
        // Update button based on payment status
        if ("PAID".equals(status)) {
            markAsPaidButton.setEnabled(false);
            markAsPaidButton.setText("Payment Completed");
        } else {
            markAsPaidButton.setEnabled(true);
            markAsPaidButton.setText("Record Payment");
        }
    }
    
    private void loadCustomerInfo(String customerId) {
        repository.getCustomer(customerId, documentSnapshot -> {
            Customer customer = documentSnapshot.toObject(Customer.class);
            if (customer != null) {
                customerNameText.setText(customer.getName());
            }
        }, e -> {
            customerNameText.setText("Unknown Customer");
        });
    }
    
    private void markBillAsPaid() {
        if (currentBill == null) return;
        
        // Open PaymentActivity to record payment
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("billId", currentBill.getId());
        startActivity(intent);
    }
    
    private void shareBill() {
        showToast("PDF sharing coming soon");
    }
}
