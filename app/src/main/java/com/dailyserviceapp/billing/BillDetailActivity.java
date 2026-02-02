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
    
    /**
     * Initializes the activity UI and starts loading bill details.
     *
     * Sets the content view, configures the toolbar title and navigation, binds view components,
     * initializes data sources and listeners, and triggers the initial load of the bill details.
     *
     * @param savedInstanceState bundle containing activity state restored from a previous instance, or null
     */
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
    
    /**
     * Refreshes bill details when the activity resumes.
     *
     * If a valid `billId` is present, reloads the bill data to reflect any updates (for example after returning from PaymentActivity).
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Reload bill details when returning from PaymentActivity
        if (billId != null && !billId.isEmpty()) {
            loadBillDetails();
        }
    }
    
    /**
     * Binds the activity's UI fields to their corresponding view instances.
     *
     * Initializes view references for customerNameText, billPeriodText, daysServedText,
     * totalAmountText, paymentStatusChip, markAsPaidButton, and shareBillButton.
     */
    private void initializeViews() {
        customerNameText = findViewById(R.id.customerName);
        billPeriodText = findViewById(R.id.billPeriod);
        daysServedText = findViewById(R.id.daysServed);
        totalAmountText = findViewById(R.id.totalAmount);
        paymentStatusChip = findViewById(R.id.paymentStatusChip);
        markAsPaidButton = findViewById(R.id.markAsPaidButton);
        shareBillButton = findViewById(R.id.shareBillButton);
    }
    
    /**
     * Initializes data dependencies and UI action handlers for the activity.
     *
     * Retrieves the bill ID from the intent, instantiates the Firestore repository,
     * validates the bill ID (shows a toast and finishes the activity if invalid),
     * and attaches click listeners to the "Mark as Paid" and "Share" buttons.
     */
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
    
    /**
     * Loads details for the activity's billId and updates the UI with the bill and its customer.
     *
     * On success the loaded bill is stored, displayed, and the associated customer information is requested.
     * On failure a toast with the error is shown and the activity is finished.
     */
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
    
    /**
     * Populate UI fields with the bill's period, days served, total amount, and payment status.
     *
     * Sets the period text to the bill's month name and year, displays the number of days served
     * and the formatted total amount, sets the payment status (defaults to "PENDING" when null),
     * and updates the mark-as-paid button's enabled state and label based on whether the bill is paid.
     *
     * @param bill the Bill whose information will be displayed; month is expected to be 0-based (0 = January)
     */
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
    
    /**
     * Fetches customer data for the given customer ID and updates the customer name TextView.
     *
     * If the customer is found, sets the customer's name in {@code customerNameText}; on fetch
     * error sets {@code customerNameText} to "Unknown Customer".
     *
     * @param customerId the Firestore document ID of the customer to load
     */
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
    
    /**
     * Initiates recording a payment for the currently loaded bill by launching PaymentActivity.
     *
     * If no bill is loaded or its identifier is missing, shows an error toast and does not start the activity.
     */
    private void markBillAsPaid() {
        if (currentBill == null || currentBill.getId() == null || currentBill.getId().isEmpty()) {
            showToast("Error: Bill data not loaded");
            return;
        }
        
        // Open PaymentActivity to record payment
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("billId", currentBill.getId());
        startActivity(intent);
    }
    
    /**
     * Notify the user that PDF sharing is not yet implemented.
     *
     * Displays a short toast informing the user that the PDF sharing feature is coming soon.
     */
    private void shareBill() {
        showToast("PDF sharing coming soon");
    }
}