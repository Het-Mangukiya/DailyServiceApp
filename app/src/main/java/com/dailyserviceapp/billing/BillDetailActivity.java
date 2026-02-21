package com.dailyserviceapp.billing;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.core.utils.DateUtils;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.Payment;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.dailyserviceapp.databinding.ActivityBillDetailBinding;
import com.dailyserviceapp.payment.PaymentActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Customer ledger detail screen.
 */
public class BillDetailActivity extends BaseActivity {

    private static final double EPSILON = 0.01;

    private ActivityBillDetailBinding binding;

    private FirestoreRepository repository;
    private String customerId;
    private Customer currentCustomer;

    private List<ServiceEntry> currentEntries = new ArrayList<>();
    private List<Payment> currentPayments = new ArrayList<>();
    private CustomerLedgerSummary currentSummary;

    private TextView customerNameText;
    private TextView billPeriodText;
    private TextView daysServedText;
    private TextView totalAmountText;
    private TextView paymentHistoryText;
    private TextView serviceHistoryText;
    private Chip paymentStatusChip;
    private MaterialButton markAsPaidButton;
    private MaterialButton shareBillButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBillDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MaterialToolbar toolbar = binding.toolbar;
        setupToolbar(toolbar, "Customer Ledger", true);

        initializeViews();
        initializeData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!TextUtils.isEmpty(customerId)) {
            loadLedgerData();
        }
    }

    private void initializeViews() {
        customerNameText = binding.customerName;
        billPeriodText = binding.billPeriod;
        daysServedText = binding.daysServed;
        totalAmountText = binding.totalAmount;
        paymentStatusChip = binding.paymentStatusChip;
        markAsPaidButton = binding.markAsPaidButton;
        shareBillButton = binding.shareBillButton;
        paymentHistoryText = binding.paymentHistoryText;
        serviceHistoryText = binding.serviceHistoryText;
    }

    private void initializeData() {
        repository = new FirestoreRepository();

        customerId = getIntent().getStringExtra("customerId");
        String customerName = getIntent().getStringExtra("customerName");
        if (!TextUtils.isEmpty(customerName)) {
            customerNameText.setText(customerName);
        }

        String billId = getIntent().getStringExtra("billId");
        if (!TextUtils.isEmpty(customerId)) {
            loadCustomerAndLedger();
        } else if (!TextUtils.isEmpty(billId)) {
            resolveCustomerFromBill(billId);
        } else {
            showToast("Invalid customer");
            finish();
            return;
        }

        markAsPaidButton.setOnClickListener(v -> openPaymentScreen());
        shareBillButton.setOnClickListener(v -> shareLedger());
    }

    private void resolveCustomerFromBill(String billId) {
        repository.getBillById(billId, new FirestoreRepository.OnBillLoadedListener() {
            @Override
            public void onBillLoaded(com.dailyserviceapp.data.models.Bill bill) {
                if (!isUiActive()) return;
                if (bill == null || TextUtils.isEmpty(bill.getCustomerId())) {
                    showToast("Invalid bill data");
                    finish();
                    return;
                }
                customerId = bill.getCustomerId();
                loadCustomerAndLedger();
            }

            @Override
            public void onError(String error) {
                showToast("Error loading ledger: " + error);
                finish();
            }
        });
    }

    private void loadCustomerAndLedger() {
        if (TextUtils.isEmpty(customerId)) {
            showToast("Invalid customer");
            finish();
            return;
        }

        repository.getCustomer(customerId, documentSnapshot -> {
            if (!isUiActive()) return;
            Customer customer = documentSnapshot.toObject(Customer.class);
            if (customer != null) {
                customer.setId(documentSnapshot.getId());
                currentCustomer = customer;
                if (!TextUtils.isEmpty(customer.getName())) {
                    customerNameText.setText(customer.getName());
                }
            }
            loadLedgerData();
        }, e -> {
            if (!isUiActive()) return;
            if (TextUtils.isEmpty(customerNameText.getText())) {
                customerNameText.setText("Unknown Customer");
            }
            loadLedgerData();
        });
    }

    private void loadLedgerData() {
        Timestamp start = new Timestamp(new Date(0));
        Calendar endCal = Calendar.getInstance();
        endCal.add(Calendar.DAY_OF_YEAR, 1);
        Timestamp end = new Timestamp(endCal.getTime());

        repository.getServiceEntriesByCustomerAndDate(customerId, start, end,
            new FirestoreRepository.OnServiceEntriesLoadedListener() {
                @Override
                public void onServiceEntriesLoaded(List<ServiceEntry> entries) {
                    if (!isUiActive()) return;
                    currentEntries = entries != null ? entries : new ArrayList<>();
                    loadPaymentsAndRender();
                }

                @Override
                public void onError(String error) {
                    if (!isUiActive()) return;
                    currentEntries = new ArrayList<>();
                    showToast("Warning: service history unavailable (" + error + ")");
                    loadPaymentsAndRender();
                }
            });
    }

    private void loadPaymentsAndRender() {
        repository.getPaymentsByCustomer(customerId, new FirestoreRepository.OnPaymentsLoadedListener() {
            @Override
            public void onPaymentsLoaded(List<Payment> payments) {
                if (!isUiActive()) return;
                currentPayments = filterProviderPayments(payments);
                renderLedger();
            }

            @Override
            public void onError(String error) {
                if (!isUiActive()) return;
                currentPayments = new ArrayList<>();
                showToast("Warning: payment history unavailable (" + error + ")");
                renderLedger();
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

    private void renderLedger() {
        currentSummary = CustomerLedgerCalculator.calculate(getSafeCustomer(), currentEntries, currentPayments);
        displaySummary(currentSummary);
        displayPaymentHistory(currentPayments);
        displayServiceHistory(currentEntries, getSafeCustomer());
    }

    private Customer getSafeCustomer() {
        if (currentCustomer != null) return currentCustomer;

        Customer fallback = new Customer();
        fallback.setId(customerId);
        fallback.setName(customerNameText.getText() != null ? customerNameText.getText().toString() : "Unknown");
        return fallback;
    }

    private void displaySummary(CustomerLedgerSummary summary) {
        String paidTillText = formatDate(summary.getPaidTillDate(), "Not paid yet");
        String dueFromText = formatDate(summary.getDueFromDate(), "-");

        if (summary.getOutstandingAmount() <= EPSILON) {
            billPeriodText.setText("Paid till: " + paidTillText + " • No pending dues");
        } else {
            billPeriodText.setText("Paid till: " + paidTillText + " • Due from: " + dueFromText);
        }

        daysServedText.setText(String.valueOf(summary.getDeliveredEntries()));
        totalAmountText.setText(CurrencyUtils.formatCurrency(summary.getOutstandingAmount()));

        if (summary.getDeliveredEntries() == 0) {
            paymentStatusChip.setText("No Service");
            paymentStatusChip.setChipBackgroundColorResource(com.dailyserviceapp.R.color.md_theme_surface_variant);
            paymentStatusChip.setTextColor(getColor(com.dailyserviceapp.R.color.md_theme_on_surface_variant));
            markAsPaidButton.setEnabled(false);
            markAsPaidButton.setText("No Due Amount");
        } else if (summary.getOutstandingAmount() <= EPSILON) {
            paymentStatusChip.setText("Clear");
            paymentStatusChip.setChipBackgroundColorResource(com.dailyserviceapp.R.color.md_theme_secondary);
            paymentStatusChip.setTextColor(getColor(com.dailyserviceapp.R.color.md_theme_on_secondary));
            markAsPaidButton.setEnabled(false);
            markAsPaidButton.setText("All Cleared");
        } else if (summary.getTotalPaidAmount() > EPSILON) {
            paymentStatusChip.setText("Partial");
            paymentStatusChip.setChipBackgroundColorResource(com.dailyserviceapp.R.color.color_service_entry);
            paymentStatusChip.setTextColor(getColor(android.R.color.white));
            markAsPaidButton.setEnabled(true);
            markAsPaidButton.setText("Record Payment");
        } else {
            paymentStatusChip.setText("Pending");
            paymentStatusChip.setChipBackgroundColorResource(com.dailyserviceapp.R.color.color_payments);
            paymentStatusChip.setTextColor(getColor(android.R.color.white));
            markAsPaidButton.setEnabled(true);
            markAsPaidButton.setText("Record Payment");
        }
    }

    private void displayPaymentHistory(List<Payment> payments) {
        if (payments == null || payments.isEmpty()) {
            paymentHistoryText.setText("No payments recorded yet.");
            return;
        }

        List<Payment> sortedPayments = new ArrayList<>(payments);
        sortedPayments.sort((p1, p2) -> {
            Date d1 = p1 != null && p1.getPaymentDate() != null ? p1.getPaymentDate().toDate() : new Date(0);
            Date d2 = p2 != null && p2.getPaymentDate() != null ? p2.getPaymentDate().toDate() : new Date(0);
            return d2.compareTo(d1);
        });

        StringBuilder builder = new StringBuilder();
        double totalPaid = 0.0;
        for (Payment payment : sortedPayments) {
            totalPaid += payment.getAmount();
        }
        int limit = Math.min(sortedPayments.size(), 25);

        for (int i = 0; i < limit; i++) {
            Payment payment = sortedPayments.get(i);
            String date = payment.getPaymentDate() != null
                ? DateUtils.formatShortDate(payment.getPaymentDate().toDate())
                : "-";
            String method = payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "Unknown";
            builder.append(date)
                .append(" • ")
                .append(CurrencyUtils.formatCurrency(payment.getAmount()))
                .append(" • ")
                .append(method);
            if (!TextUtils.isEmpty(payment.getNotes())) {
                builder.append(" • ").append(payment.getNotes());
            }
            if (i < limit - 1) {
                builder.append("\n");
            }
        }

        if (sortedPayments.size() > limit) {
            builder.append("\n...")
                .append("\nShowing ")
                .append(limit)
                .append(" of ")
                .append(sortedPayments.size())
                .append(" records");
        }

        builder.insert(0, "Total paid: " + CurrencyUtils.formatCurrency(totalPaid) + "\n\n");
        paymentHistoryText.setText(builder.toString());
    }

    private void displayServiceHistory(List<ServiceEntry> entries, Customer customer) {
        if (entries == null || entries.isEmpty()) {
            serviceHistoryText.setText("No delivered service entries found.");
            return;
        }

        List<ServiceEntry> delivered = new ArrayList<>();
        for (ServiceEntry entry : entries) {
            if (entry != null && entry.isDelivered() && entry.getDate() != null) {
                delivered.add(entry);
            }
        }

        if (delivered.isEmpty()) {
            serviceHistoryText.setText("No delivered service entries found.");
            return;
        }

        delivered.sort(Comparator.comparing((ServiceEntry e) -> e.getDate().toDate()).reversed());

        StringBuilder builder = new StringBuilder();
        double total = 0.0;
        for (ServiceEntry entry : delivered) {
            total += CustomerLedgerCalculator.calculateEntryAmount(entry, customer);
        }
        int limit = Math.min(delivered.size(), 30);

        for (int i = 0; i < limit; i++) {
            ServiceEntry entry = delivered.get(i);
            double lineAmount = CustomerLedgerCalculator.calculateEntryAmount(entry, customer);

            String date = DateUtils.formatShortDate(entry.getDate().toDate());
            double rate = entry.getRate() > 0 ? entry.getRate() : customer.getRatePerUnit();

            builder.append(date)
                .append(" • Qty ")
                .append(entry.getQuantity())
                .append(" x ")
                .append(CurrencyUtils.formatCurrency(rate))
                .append(" = ")
                .append(CurrencyUtils.formatCurrency(lineAmount));

            if (!TextUtils.isEmpty(entry.getNotes())) {
                builder.append(" • ").append(entry.getNotes());
            }

            if (i < limit - 1) {
                builder.append("\n");
            }
        }

        if (delivered.size() > limit) {
            builder.append("\n...")
                .append("\nShowing ")
                .append(limit)
                .append(" of ")
                .append(delivered.size())
                .append(" records");
        }

        builder.insert(0, "Service value: " + CurrencyUtils.formatCurrency(total) + "\n\n");
        serviceHistoryText.setText(builder.toString());
    }

    private String formatDate(Timestamp timestamp, String fallback) {
        if (timestamp == null) return fallback;
        return DateUtils.formatShortDate(timestamp.toDate());
    }

    private void openPaymentScreen() {
        if (currentSummary == null) {
            showToast("Ledger not loaded yet");
            return;
        }

        if (currentSummary.getOutstandingAmount() <= EPSILON) {
            showToast("No pending amount for this customer");
            return;
        }

        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("customerId", customerId);
        intent.putExtra("customerName", customerNameText.getText() != null
            ? customerNameText.getText().toString() : "Unknown Customer");
        intent.putExtra("dueAmount", currentSummary.getOutstandingAmount());
        if (currentSummary.getDueFromDate() != null) {
            intent.putExtra("dueFromMillis", currentSummary.getDueFromDate().toDate().getTime());
        }
        startActivity(intent);
    }

    private void shareLedger() {
        if (currentSummary == null) {
            showToast("Ledger not loaded yet");
            return;
        }

        String paidTill = formatDate(currentSummary.getPaidTillDate(), "Not paid yet");
        String dueFrom = formatDate(currentSummary.getDueFromDate(), "-");

        String text = "Customer: " + currentSummary.getCustomerName() + "\n"
            + "Outstanding: " + CurrencyUtils.formatCurrency(currentSummary.getOutstandingAmount()) + "\n"
            + "Paid Till: " + paidTill + "\n"
            + "Due From: " + dueFrom + "\n"
            + "Delivered Entries: " + currentSummary.getDeliveredEntries() + "\n"
            + "Total Paid: " + CurrencyUtils.formatCurrency(currentSummary.getTotalPaidAmount());

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Customer Ledger - " + currentSummary.getCustomerName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(shareIntent, "Share ledger"));
    }

    private boolean isUiActive() {
        return binding != null && !isFinishing() && !isDestroyed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
