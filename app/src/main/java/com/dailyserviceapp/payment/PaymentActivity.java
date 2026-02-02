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
    
    /**
     * Initializes the activity UI and prepares the flow for recording a payment.
     *
     * Performs critical startup validation (ensures a bill ID is present in the Intent and that the user is logged in), sets the content view and toolbar, initializes views, data, and event listeners, and begins loading the bill details.
     *
     * @param savedInstanceState state used to restore the activity's previous state, if any
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_new);
        
        // CRITICAL: Validate billId FIRST before any initialization
        billId = getIntent().getStringExtra("billId");
        if (billId == null || billId.isEmpty()) {
            showToast("Error: Bill ID not provided. Please open from Bill Details.");
            finish();
            return;
        }
        
        // CRITICAL: Check session
        if (!isLoggedIn()) {
            showToast("Please login first");
            navigateToLogin();
            return;
        }
        
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, "Record Payment", true);
        
        initializeViews();
        initializeData();
        setupClickListeners();
        loadBillDetails();
    }
    
    /**
     * Binds activity UI components to fields and configures the payment method dropdown.
     *
     * Initializes references for customer name, bill period, bill amount, amount input, payment method dropdown,
     * payment date input, notes input, and the record payment button, then sets up the dropdown with available
     * payment methods and preselects "Cash".
     */
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
    
    /**
     * Initializes data sources and default values used by the activity.
     *
     * Sets up the FirestoreRepository instance, sets the payment date to the current date,
     * and refreshes the payment date display in the UI.
     */
    private void initializeData() {
        repository = new FirestoreRepository();
        
        // Set current date as default payment date
        selectedPaymentDate = new Date();
        updatePaymentDateDisplay();
    }
    
    /**
     * Attaches click handlers for the payment UI controls.
     *
     * Clicking the payment date input opens a date picker. Clicking the record payment
     * button triggers validation and attempts to record the payment.
     */
    private void setupClickListeners() {
        paymentDateInput.setOnClickListener(v -> showDatePicker());
        recordPaymentButton.setOnClickListener(v -> recordPayment());
    }
    
    /**
     * Loads bill data for the current billId and updates the UI and activity state accordingly.
     *
     * If billId is invalid, shows an error toast and finishes the activity. Otherwise retrieves the
     * Bill from the repository, sets {@code currentBill}, displays bill information, loads the
     * associated customer, and pre-fills the amount input with the bill's total. If loading fails,
     * shows an error toast and finishes the activity.
     */
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
    
    /**
     * Updates the UI to show the bill's period (month and year) and the bill's total formatted as currency.
     *
     * @param bill the Bill whose month, year, and total amount should be displayed; expects {@code bill.getMonth()} to be a zero-based month index (0 = January)
     */
    private void displayBillInfo(Bill bill) {
        // Set bill period
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                               "July", "August", "September", "October", "November", "December"};
        billPeriodText.setText(monthNames[bill.getMonth()] + " " + bill.getYear());
        
        // Set bill amount
        billAmountText.setText(CurrencyUtils.formatCurrency(bill.getTotalAmount()));
    }
    
    /**
     * Loads the customer record for the given customer ID and updates the customer name text view.
     *
     * If a customer document is found, sets customerNameText to the customer's name; on error or if
     * the document cannot be converted to a Customer, sets customerNameText to "Unknown Customer".
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
     * Displays a date picker initialized to the current selected payment date, restricted to today or earlier, and updates the activity's selected payment date and its displayed value when the user chooses a date.
     */
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
        // Restrict to past and today only (no future payments)
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }
    
    /**
     * Formats the currently selected payment date and updates the payment date input field.
     */
    private void updatePaymentDateDisplay() {
        String dateStr = DateUtils.formatShortDate(selectedPaymentDate);
        paymentDateInput.setText(dateStr);
    }
    
    /**
     * Validates the payment form inputs and proceeds to record the payment or prompt for confirmation when the entered amount exceeds the bill total.
     *
     * <p>Performs validation of the amount (presence, numeric format, > 0, at most two decimal places, and an upper bound) and the selected payment method.
     * On validation failures it sets field errors or shows a toast as appropriate. If the amount exceeds the bill total, displays a confirmation dialog
     * informing the user that the excess will be credited; on confirmation the payment is saved. Otherwise the payment is saved immediately.</p>
     */
    private void recordPayment() {
        // Validate input
        String amountStr = amountInput.getText() != null ? amountInput.getText().toString().trim() : "";
        if (amountStr.isEmpty()) {
            amountInput.setError("Payment amount is required");
            amountInput.requestFocus();
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                amountInput.setError("Amount must be greater than 0");
                amountInput.requestFocus();
                return;
            }
            
            // Validate decimal places (max 2)
            String[] parts = amountStr.split("\\.");
            if (parts.length > 1 && parts[1].length() > 2) {
                amountInput.setError("Maximum 2 decimal places allowed");
                amountInput.requestFocus();
                return;
            }
            
            // Validate reasonable maximum (prevent typos like 999999)
            if (amount > 1000000) {
                amountInput.setError("Amount seems unusually large. Please verify.");
                amountInput.requestFocus();
                return;
            }
            
        } catch (NumberFormatException e) {
            amountInput.setError("Invalid amount format");
            amountInput.requestFocus();
            return;
        }
        
        String paymentMethod = paymentMethodDropdown.getText().toString().trim();
        if (paymentMethod.isEmpty()) {
            showToast("Please select payment method");
            return;
        }
        
        // Check for overpayment and warn user
        if (amount > currentBill.getTotalAmount()) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Overpayment Detected")
                .setMessage(String.format("Payment amount (₹%.2f) exceeds bill total (₹%.2f). The excess will be credited. Continue?", 
                    amount, currentBill.getTotalAmount()))
                .setPositiveButton("Continue", (dialog, which) -> savePayment(amount, paymentMethod))
                .setNegativeButton("Cancel", null)
                .show();
        } else {
            savePayment(amount, paymentMethod);
        }
    }
    
    /**
     * Persist the validated payment and update the associated bill's payment status.
     *
     * @param amount        the monetary amount to record for this payment
     * @param paymentMethod the payment method label (e.g., "Cash", "Card") used for this payment
     */
    private void savePayment(double amount, String paymentMethod) {
        
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
            /**
             * Update the associated bill's payment status based on the recorded payment amount.
             *
             * <p>If the recorded amount meets or exceeds the bill total, the bill will be marked paid;
             * otherwise it will be marked partial.</p>
             */
            @Override
            public void onSuccess() {
                // Update bill payment status
                updateBillPaymentStatus(amount);
            }

            /**
             * Re-enables the record payment button and displays an error message when saving a payment fails.
             *
             * @param error the error message describing why the payment could not be recorded
             */
            @Override
            public void onError(String error) {
                recordPaymentButton.setEnabled(true);
                showToast("Error recording payment: " + error);
            }
        });
    }
    
    /**
     * Update the current bill's payment status based on the provided paid amount and persist the change.
     *
     * Sets the bill's payment status to "PAID" when the paid amount is greater than or equal to the bill total,
     * otherwise sets it to "PARTIAL". Persists the updated bill; on successful save shows a success toast and
     * finishes the activity, and on save error shows an error toast (including the repository error) and finishes.
     *
     * @param paidAmount the amount that was applied to the bill
     */
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