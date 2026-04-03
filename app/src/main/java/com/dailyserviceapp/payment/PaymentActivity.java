package com.dailyserviceapp.payment;

import dagger.hilt.android.AndroidEntryPoint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.billing.CustomerLedgerCalculator;
import com.dailyserviceapp.billing.CustomerLedgerSummary;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.core.utils.DateUtils;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Bill;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.Payment;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.dailyserviceapp.databinding.ActivityPaymentNewBinding;
import com.dailyserviceapp.notifications.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Payment Activity for recording customer payments.
 */
@AndroidEntryPoint
public class PaymentActivity extends BaseActivity {

    private static final double EPSILON = 0.01;

    private ActivityPaymentNewBinding binding;

    private FirestoreRepository repository;
    private String billId;
    private Bill currentBill;

    private boolean customerMode;
    private String customerId;
    private String customerNameFromIntent;
    private double dueAmountFromIntent;
    private long dueFromMillis;
    private Customer currentCustomer;
    private double currentOutstandingAmount;

    private Date selectedPaymentDate;
    private boolean hasUserEditedAmount;
    private boolean isProgrammaticAmountUpdate;

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
        binding = ActivityPaymentNewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        billId = getIntent().getStringExtra("billId");
        customerId = getIntent().getStringExtra("customerId");
        customerMode = !TextUtils.isEmpty(customerId);

        if (!customerMode && TextUtils.isEmpty(billId)) {
            showToast("Error: payment context missing");
            finish();
            return;
        }

        if (customerMode) {
            customerNameFromIntent = getIntent().getStringExtra("customerName");
            dueAmountFromIntent = getIntent().getDoubleExtra("dueAmount", 0.0);
            dueFromMillis = getIntent().getLongExtra("dueFromMillis", -1L);
            currentOutstandingAmount = Math.max(0.0, dueAmountFromIntent);
        }

        if (!isLoggedIn()) {
            showToast("Please login first");
            navigateToLogin();
            return;
        }

        MaterialToolbar toolbar = binding.toolbar;
        setupToolbar(toolbar, "Record Payment", true);

        initializeViews();
        initializeData();
        setupClickListeners();

        if (customerMode) {
            loadCustomerLedgerContext();
        } else {
            loadBillDetails();
        }
    }

    private void initializeViews() {
        customerNameText = binding.customerNameText;
        billPeriodText = binding.billPeriodText;
        billAmountText = binding.billAmountText;
        amountInput = binding.amountInput;
        paymentMethodDropdown = binding.paymentMethodDropdown;
        paymentDateInput = binding.paymentDateInput;
        notesInput = binding.notesInput;
        recordPaymentButton = binding.recordPaymentButton;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            PAYMENT_METHODS
        );
        paymentMethodDropdown.setAdapter(adapter);
        paymentMethodDropdown.setText("Cash", false);
        amountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isProgrammaticAmountUpdate && amountInput.hasFocus()) {
                    hasUserEditedAmount = true;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op
            }
        });
    }

    private void initializeData() {
        repository = new FirestoreRepository();

        selectedPaymentDate = new Date();
        updatePaymentDateDisplay();
    }

    private void setupClickListeners() {
        paymentDateInput.setOnClickListener(v -> showDatePicker());
        recordPaymentButton.setOnClickListener(v -> recordPayment());
    }

    private void loadCustomerLedgerContext() {
        if (!TextUtils.isEmpty(customerNameFromIntent)) {
            customerNameText.setText(customerNameFromIntent);
        } else {
            customerNameText.setText("Loading...");
        }

        if (dueFromMillis > 0) {
            billPeriodText.setText("Due from " + DateUtils.formatShortDate(new Date(dueFromMillis)));
        } else {
            billPeriodText.setText("Outstanding from service entries");
        }

        billAmountText.setText(CurrencyUtils.formatCurrency(currentOutstandingAmount));
        if (currentOutstandingAmount > EPSILON) {
            setAmountInputValue(String.format(Locale.getDefault(), "%.2f", currentOutstandingAmount));
        }

        repository.getCustomer(customerId, documentSnapshot -> {
            Customer customer = documentSnapshot.toObject(Customer.class);
            if (customer != null) {
                customer.setId(documentSnapshot.getId());
                currentCustomer = customer;
                if (!TextUtils.isEmpty(customer.getName())) {
                    customerNameText.setText(customer.getName());
                }
            }
            refreshOutstandingForCustomer();
        }, e -> {
            refreshOutstandingForCustomer();
        });
    }

    private void refreshOutstandingForCustomer() {
        Timestamp start = new Timestamp(new Date(0));
        Calendar endCal = Calendar.getInstance();
        endCal.add(Calendar.DAY_OF_YEAR, 1);
        Timestamp end = new Timestamp(endCal.getTime());

        final AtomicInteger pending = new AtomicInteger(2);
        final List<ServiceEntry>[] entriesHolder = new List[]{new ArrayList<>()};
        final List<Payment>[] paymentsHolder = new List[]{new ArrayList<>()};

        Runnable calculateWhenReady = () -> {
            if (pending.decrementAndGet() != 0) {
                return;
            }
            List<Payment> providerPayments = filterProviderPayments(paymentsHolder[0]);
            Customer customer = currentCustomer != null ? currentCustomer : new Customer();
            customer.setId(customerId);
            String displayedName = customerNameText.getText() != null
                ? customerNameText.getText().toString().trim() : "";
            if (!TextUtils.isEmpty(displayedName) && !"Loading...".equalsIgnoreCase(displayedName)) {
                customer.setName(displayedName);
            } else if (!TextUtils.isEmpty(customerNameFromIntent)) {
                customer.setName(customerNameFromIntent);
            }

            CustomerLedgerSummary summary = CustomerLedgerCalculator.calculate(
                customer,
                entriesHolder[0],
                providerPayments
            );

            currentOutstandingAmount = summary.getOutstandingAmount();
            billAmountText.setText(CurrencyUtils.formatCurrency(currentOutstandingAmount));

            if (summary.getDueFromDate() != null) {
                billPeriodText.setText("Due from " + DateUtils.formatShortDate(summary.getDueFromDate().toDate()));
            }

            if (!hasUserEditedAmount && currentOutstandingAmount > EPSILON) {
                setAmountInputValue(String.format(Locale.getDefault(), "%.2f", currentOutstandingAmount));
            }
        };

        repository.getServiceEntriesByCustomerAndDate(customerId, start, end,
            new FirestoreRepository.OnServiceEntriesLoadedListener() {
                @Override
                public void onServiceEntriesLoaded(List<ServiceEntry> entries) {
                    entriesHolder[0] = entries != null ? entries : new ArrayList<>();
                    calculateWhenReady.run();
                }

                @Override
                public void onError(String error) {
                    entriesHolder[0] = new ArrayList<>();
                    calculateWhenReady.run();
                }
            });

        repository.getPaymentsByCustomer(customerId, new FirestoreRepository.OnPaymentsLoadedListener() {
            @Override
            public void onPaymentsLoaded(List<Payment> payments) {
                paymentsHolder[0] = payments != null ? payments : new ArrayList<>();
                calculateWhenReady.run();
            }

            @Override
            public void onError(String error) {
                paymentsHolder[0] = new ArrayList<>();
                calculateWhenReady.run();
            }
        });
    }

    private List<Payment> filterProviderPayments(List<Payment> payments) {
        List<Payment> filtered = new ArrayList<>();
        if (payments == null) return filtered;

        String providerId = getCurrentUserId();
        if (providerId == null || providerId.trim().isEmpty()) return filtered;
        for (Payment payment : payments) {
            if (payment == null) continue;
            if (providerId.equals(payment.getProviderId())) {
                filtered.add(payment);
            }
        }
        return filtered;
    }

    private void loadBillDetails() {
        repository.getBillById(billId, new FirestoreRepository.OnBillLoadedListener() {
            @Override
            public void onBillLoaded(Bill bill) {
                currentBill = bill;
                displayBillInfo(bill);
                loadCustomerInfo(bill.getCustomerId());
                currentOutstandingAmount = bill.getTotalAmount();
                setAmountInputValue(String.format(Locale.getDefault(), "%.2f", bill.getTotalAmount()));
            }

            @Override
            public void onError(String error) {
                showToast("Error loading bill: " + error);
                finish();
            }
        });
    }

    private void displayBillInfo(Bill bill) {
        int monthIndex = bill.getMonth();
        if (monthIndex < 0 || monthIndex >= 12) {
            monthIndex = 0;
        }
        Calendar periodCalendar = Calendar.getInstance();
        periodCalendar.set(Calendar.YEAR, bill.getYear());
        periodCalendar.set(Calendar.MONTH, monthIndex);
        periodCalendar.set(Calendar.DAY_OF_MONTH, 1);
        String periodText = new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            .format(periodCalendar.getTime());
        billPeriodText.setText(periodText);
        billAmountText.setText(CurrencyUtils.formatCurrency(bill.getTotalAmount()));
    }

    private void loadCustomerInfo(String customerIdToLoad) {
        repository.getCustomer(customerIdToLoad, documentSnapshot -> {
            Customer customer = documentSnapshot.toObject(Customer.class);
            if (customer != null) {
                customerNameText.setText(customer.getName());
            }
        }, e -> customerNameText.setText("Unknown Customer"));
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
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void updatePaymentDateDisplay() {
        String dateStr = DateUtils.formatShortDate(selectedPaymentDate);
        paymentDateInput.setText(dateStr);
    }

    private void recordPayment() {
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

            if (!amountStr.matches("^\\d*(?:\\.\\d{0,2})?$")) {
                amountInput.setError("Maximum 2 decimal places allowed");
                amountInput.requestFocus();
                return;
            }

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

        if (currentOutstandingAmount > EPSILON && amount > currentOutstandingAmount + EPSILON) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Overpayment Detected")
                .setMessage(String.format(Locale.getDefault(),
                    "Payment amount (%s) exceeds current due (%s). Continue?",
                    CurrencyUtils.formatCurrency(amount),
                    CurrencyUtils.formatCurrency(currentOutstandingAmount)))
                .setPositiveButton("Continue", (dialog, which) -> persistPayment(amount, paymentMethod))
                .setNegativeButton("Cancel", null)
                .show();
        } else {
            persistPayment(amount, paymentMethod);
        }
    }

    private void persistPayment(double amount, String paymentMethod) {
        if (customerMode) {
            saveCustomerPayment(amount, paymentMethod);
        } else {
            saveBillLinkedPayment(amount, paymentMethod);
        }
    }

    private void saveCustomerPayment(double amount, String paymentMethod) {
        String notes = notesInput.getText() != null ? notesInput.getText().toString().trim() : "";

        Payment payment = new Payment(
            null,
            getCurrentUserId(),
            customerId,
            amount,
            paymentMethod,
            new Timestamp(selectedPaymentDate)
        );
        payment.setNotes(notes);

        recordPaymentButton.setEnabled(false);
        repository.savePayment(payment, new FirestoreRepository.OnSaveCompleteListener() {
            @Override
            public void onSuccess() {
                NotificationHelper.saveNotification(
                    customerId,
                    "Payment received",
                    "A payment of " + CurrencyUtils.formatCurrency(amount) + " was recorded.",
                    Constants.NOTIF_PAYMENT_RECEIVED,
                    payment.getBillId()
                );
                showToast("Payment recorded successfully");
                finish();
            }

            @Override
            public void onError(String error) {
                recordPaymentButton.setEnabled(true);
                showToast("Error recording payment: " + error);
            }
        });
    }

    private void saveBillLinkedPayment(double amount, String paymentMethod) {
        if (currentBill == null) {
            showToast("Bill details not loaded yet");
            return;
        }
        String notes = notesInput.getText() != null ? notesInput.getText().toString().trim() : "";

        Payment payment = new Payment(
            billId,
            getCurrentUserId(),
            currentBill.getCustomerId(),
            amount,
            paymentMethod,
            new Timestamp(selectedPaymentDate)
        );
        payment.setNotes(notes);

        recordPaymentButton.setEnabled(false);
        repository.savePayment(payment, new FirestoreRepository.OnSaveCompleteListener() {
            @Override
            public void onSuccess() {
                updateBillPaymentStatus(amount);
            }

            @Override
            public void onError(String error) {
                recordPaymentButton.setEnabled(true);
                showToast("Error recording payment: " + error);
            }
        });
    }

    private void updateBillPaymentStatus(double latestPaymentAmount) {
        double billTotal = currentBill.getTotalAmount();

        repository.getPaymentsByBill(billId, new FirestoreRepository.OnPaymentsLoadedListener() {
            @Override
            public void onPaymentsLoaded(List<Payment> payments) {
                double totalPaid = 0.0;
                if (payments != null) {
                    for (Payment payment : payments) {
                        totalPaid += payment.getAmount();
                    }
                }

                if (totalPaid <= 0) {
                    currentBill.setPaymentStatus("PENDING");
                } else if (totalPaid + EPSILON >= billTotal) {
                    currentBill.setPaymentStatus("PAID");
                } else {
                    currentBill.setPaymentStatus("PARTIAL");
                }

                repository.saveBill(currentBill, new FirestoreRepository.OnSaveCompleteListener() {
                    @Override
                    public void onSuccess() {
                        NotificationHelper.saveNotification(
                            currentBill.getCustomerId(),
                            "Payment received",
                            "A payment of " + CurrencyUtils.formatCurrency(latestPaymentAmount) + " was recorded.",
                            Constants.NOTIF_PAYMENT_RECEIVED,
                            billId
                        );
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

            @Override
            public void onError(String error) {
                showToast("Payment saved but failed to recalculate status: " + error);
                finish();
            }
        });
    }

    private void setAmountInputValue(String value) {
        isProgrammaticAmountUpdate = true;
        amountInput.setText(value);
        isProgrammaticAmountUpdate = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
