package com.dailyserviceapp.payment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.core.utils.DateUtils;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Bill;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.Payment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.Date;

/**
 * Payment Activity for recording payment transactions.
 * Allows providers to record payments received from customers against bills.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Record payment amount</li>
 *   <li>Select payment method (Cash, UPI, Bank Transfer, Cheque, Other)</li>
 *   <li>Set payment date</li>
 *   <li>Add optional notes</li>
 *   <li>Automatically update bill payment status</li>
 * </ul>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-09
 */
public class PaymentActivity extends BaseActivity {
    
    private FirestoreRepository repository;
    private String billId;
    private Bill currentBill;
    private Date selectedPaymentDate;
    
    private TextView customerNameText;
    private TextView billPeriodText;
    private TextView billAmountText;
    private TextInputEditText amountInput;
    private AutoCompleteTextView paymentMethodDropdown;
    private TextInputEditText paymentDateInput;
    private TextInputEditText notesInput;
    private MaterialButton recordPaymentButton;
    
    private static final String[] PAYMENT_METHODS = {
        "Cash",
        "UPI",
        "Bank Transfer",
        "Cheque",
        "Other"
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_new);
        
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, "Record Payment", true);
        
        initializeViews();
        initializeData();
        setupClickListeners();
        loadBillDetails();
    }
    
    private void initializeViews() {
        customerNameText = findViewById(R.id.customerNameText);
        billPeriodText = findViewById(R.id.billPeriodText);
        billAmountText = findViewById(R.id.billAmountText);
        amountInput = findViewById(R.id.amountInput);
        paymentMethodDropdown = findViewById(R.id.paymentMethodDropdown);
        paymentDateInput = findViewById(R.id.paymentDateInput);
        notesInput = findViewById(R.id.notesInput);
        recordPaymentButton = findViewById(R.id.recordPaymentButton);
        
        // Setup payment method dropdown
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                PAYMENT_METHODS
        );
        paymentMethodDropdown.setAdapter(adapter);
        paymentMethodDropdown.setText("Cash", false);
    }
    
    private void initializeData() {
        repository = new FirestoreRepository();
        billId = getIntent().getStringExtra("billId");
        
        if (billId == null || billId.isEmpty()) {
            showToast("Error: Bill ID not provided. Please open from Bill Details.");
            finish();
            return;
        }
        
        // Set current date as default payment date
        selectedPaymentDate = new Date();
        updatePaymentDateDisplay();
    }
    
    private void setupClickListeners() {
        paymentDateInput.setOnClickListener(v -> showDatePicker());
        recordPaymentButton.setOnClickListener(v -> recordPayment());
    }
    
    private void loadBillDetails() {
        if (billId == null || billId.isEmpty()) {
            showToast("Cannot load bill: Invalid ID");
            finish();
            return;
        }
        
        repository.getBillById(billId, new FirestoreRepository.OnBillLoadedListener() {
            @Override
            public void onBillLoaded(Bill bill) {
                currentBill = bill;
                displayBillInfo(bill);
                loadCustomerInfo(bill.getCustomerId());
                
                // Pre-fill amount with bill total
                amountInput.setText(String.valueOf(bill.getTotalAmount()));
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
        
        // Set bill amount
        billAmountText.setText(CurrencyUtils.formatCurrency(bill.getTotalAmount()));
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
    
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedPaymentDate);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    selectedPaymentDate = selected.getTime();
                    updatePaymentDateDisplay();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private void updatePaymentDateDisplay() {
        String dateStr = DateUtils.formatShortDate(selectedPaymentDate);
        paymentDateInput.setText(dateStr);
    }
    
    private void recordPayment() {
        // Validate input
        String amountStr = amountInput.getText() != null ? amountInput.getText().toString().trim() : "";
        if (amountStr.isEmpty()) {
            showToast("Please enter payment amount");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                showToast("Amount must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            showToast("Invalid amount");
            return;
        }
        
        String paymentMethod = paymentMethodDropdown.getText().toString().trim();
        if (paymentMethod.isEmpty()) {
            showToast("Please select payment method");
            return;
        }
        
        String notes = notesInput.getText() != null ? notesInput.getText().toString().trim() : "";
        
        // Create payment object
        Payment payment = new Payment(
                billId,
                getCurrentUserId(),
                currentBill.getCustomerId(),
                amount,
                paymentMethod,
                new Timestamp(selectedPaymentDate)
        );
        payment.setNotes(notes);
        
        // Save payment
        recordPaymentButton.setEnabled(false);
        repository.savePayment(payment, new FirestoreRepository.OnSaveCompleteListener() {
            @Override
            public void onSuccess() {
                // Update bill payment status
                updateBillPaymentStatus(amount);
            }

            @Override
            public void onError(String error) {
                recordPaymentButton.setEnabled(true);
                showToast("Error recording payment: " + error);
            }
        });
    }
    
    private void updateBillPaymentStatus(double paidAmount) {
        double billTotal = currentBill.getTotalAmount();
        
        if (paidAmount >= billTotal) {
            currentBill.setPaymentStatus("PAID");
        } else {
            currentBill.setPaymentStatus("PARTIAL");
        }
        
        repository.saveBill(currentBill, new FirestoreRepository.OnSaveCompleteListener() {
            @Override
            public void onSuccess() {
                showToast("Payment recorded successfully");
                finish();
            }

            @Override
            public void onError(String error) {
                showToast("Payment saved but status update failed: " + error);
                finish();
            }
        });
    }
}
