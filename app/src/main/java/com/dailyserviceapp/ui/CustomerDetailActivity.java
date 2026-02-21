package com.dailyserviceapp.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.dailyserviceapp.R;
import com.dailyserviceapp.billing.CustomerLedgerCalculator;
import com.dailyserviceapp.billing.CustomerLedgerSummary;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.core.utils.DateUtils;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.Payment;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.dailyserviceapp.databinding.ActivityCustomerDetailBinding;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomerDetailActivity extends BaseActivity {

    public static final String EXTRA_CUSTOMER_ID = "customerId";

    private ActivityCustomerDetailBinding binding;

    private FirestoreRepository repo;
    private String customerId;
    private String providerId;

    private TextView customerName;
    private TextView customerMeta;
    private TextView customerPhone;
    private TextView customerAddress;
    private TextView profileInitial;
    private TextView monthRingLabel;
    private TextView monthValue;
    private TextView deliveredEntriesValue;
    private TextView totalQuantityValue;
    private TextView serviceValueValue;
    private TextView paymentTotalServiceValue;
    private TextView paymentTotalPaidValue;
    private TextView paymentOutstandingValue;
    private TextView paymentPaidTillValue;
    private TextView paymentDueFromValue;
    private Chip customerStatusChip;
    private Chip customerVacationChip;
    private MaterialButton markDeliveredButton;

    private Customer customer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!isLoggedIn()) {
            showToast("Please login first");
            navigateToLogin();
            return;
        }

        customerId = getIntent().getStringExtra(EXTRA_CUSTOMER_ID);
        if (customerId == null || customerId.trim().isEmpty()) {
            finish();
            return;
        }

        providerId = getCurrentUserId();
        if (providerId == null || providerId.isEmpty()) {
            showToast("Session expired. Please login again.");
            navigateToLogin();
            return;
        }

        repo = new FirestoreRepository();

        MaterialToolbar toolbar = binding.toolbar;
        setupToolbar(toolbar, "Customer Profile", true);

        customerName = binding.customerName;
        customerMeta = binding.customerMeta;
        customerPhone = binding.customerPhone;
        customerAddress = binding.customerAddress;
        profileInitial = binding.profileInitial;
        monthRingLabel = binding.monthRingLabel;
        monthValue = binding.monthValue;
        deliveredEntriesValue = binding.deliveredEntriesValue;
        totalQuantityValue = binding.totalQuantityValue;
        serviceValueValue = binding.serviceValueValue;
        paymentTotalServiceValue = binding.paymentTotalServiceValue;
        paymentTotalPaidValue = binding.paymentTotalPaidValue;
        paymentOutstandingValue = binding.paymentOutstandingValue;
        paymentPaidTillValue = binding.paymentPaidTillValue;
        paymentDueFromValue = binding.paymentDueFromValue;
        customerStatusChip = binding.customerStatusChip;
        customerVacationChip = binding.customerVacationChip;
        markDeliveredButton = binding.markDeliveredButton;

        markDeliveredButton.setOnClickListener(v -> markDeliveredToday());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomer();
    }

    private void loadCustomer() {
        repo.getCustomer(customerId,
            doc -> {
                if (!isUiActive()) return;
                customer = doc.toObject(Customer.class);
                if (customer == null) {
                    Toast.makeText(this, "Customer not found", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                customer.setId(doc.getId());
                renderCustomer();
                loadMonthlySummary();
                loadLedgerStatus();
            },
            e -> {
                if (!isUiActive()) return;
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        );
    }

    private void renderCustomer() {
        if (customer == null) return;

        String name = safeText(customer.getName(), "Customer");
        customerName.setText(name);
        profileInitial.setText(name.substring(0, 1).toUpperCase());
        profileInitial.setContentDescription(getString(R.string.profile_initial_for, name));

        String service = safeText(customer.getServiceType(), "Service not set");
        String rateText = CurrencyUtils.formatCurrency(customer.getRatePerUnit());
        customerMeta.setText(service + " • " + rateText + "/unit");

        customerPhone.setText(getString(R.string.phone_label, safeText(customer.getPhone(), "-")));

        StringBuilder address = new StringBuilder();
        if (!TextUtils.isEmpty(customer.getAddress())) {
            address.append(customer.getAddress());
        }
        if (!TextUtils.isEmpty(customer.getArea())) {
            if (address.length() > 0) address.append(", ");
            address.append(customer.getArea());
        }
        customerAddress.setText(getString(
            R.string.address_label,
            address.length() > 0 ? address.toString() : "-"
        ));

        String status = safeText(customer.getStatus(), "ACTIVE").toUpperCase();
        customerStatusChip.setText(status);

        if (customer.isOnVacation()) {
            customerVacationChip.setText(R.string.on_vacation);
            customerVacationChip.setVisibility(android.view.View.VISIBLE);
        } else {
            customerVacationChip.setVisibility(android.view.View.GONE);
        }
    }

    private void loadMonthlySummary() {
        if (customer == null) return;

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
        monthEnd.set(Calendar.MILLISECOND, 999);

        repo.getServiceEntriesByCustomerAndDate(
            customerId,
            new Timestamp(monthStart.getTime()),
            new Timestamp(monthEnd.getTime()),
            new FirestoreRepository.OnServiceEntriesLoadedListener() {
                @Override
                public void onServiceEntriesLoaded(List<ServiceEntry> entries) {
                    if (!isUiActive()) return;
                    int deliveredCount = 0;
                    double totalQty = 0.0;
                    double totalAmount = 0.0;

                    if (entries != null) {
                        for (ServiceEntry entry : entries) {
                            if (entry != null && entry.isDelivered()) {
                                deliveredCount++;
                                double quantity = entry.getQuantity() > 0 ? entry.getQuantity() : 1.0;
                                double rate = entry.getRate() > 0 ? entry.getRate() : customer.getRatePerUnit();
                                totalQty += quantity;
                                totalAmount += quantity * rate;
                            }
                        }
                    }

                    String monthText = DateUtils.formatMonthYear(new Date());
                    monthRingLabel.setText(getString(R.string.month_ring_format, monthText));
                    monthValue.setText(monthText);
                    deliveredEntriesValue.setText(String.valueOf(deliveredCount));
                    totalQuantityValue.setText(String.format(Locale.US, "%.1f", totalQty));
                    serviceValueValue.setText(CurrencyUtils.formatCurrency(totalAmount));
                }

                @Override
                public void onError(String error) {
                    if (!isUiActive()) return;
                    Toast.makeText(CustomerDetailActivity.this, error, Toast.LENGTH_LONG).show();
                    setMonthlySummaryFallback();
                }
            }
        );
    }

    private void loadLedgerStatus() {
        if (customer == null) return;

        Timestamp start = new Timestamp(new Date(0));
        Calendar endCal = Calendar.getInstance();
        endCal.add(Calendar.DAY_OF_YEAR, 1);
        Timestamp end = new Timestamp(endCal.getTime());

        repo.getServiceEntriesByCustomerAndDate(customerId, start, end,
            new FirestoreRepository.OnServiceEntriesLoadedListener() {
                @Override
                public void onServiceEntriesLoaded(List<ServiceEntry> entries) {
                    if (!isUiActive()) return;
                    repo.getPaymentsByCustomer(customerId, new FirestoreRepository.OnPaymentsLoadedListener() {
                        @Override
                        public void onPaymentsLoaded(List<Payment> payments) {
                            if (!isUiActive()) return;
                            List<Payment> providerPayments = filterProviderPayments(payments);
                            CustomerLedgerSummary summary = CustomerLedgerCalculator.calculate(
                                customer,
                                entries != null ? entries : new ArrayList<>(),
                                providerPayments
                            );

                            String paidTill = summary.getPaidTillDate() != null
                                ? DateUtils.formatShortDate(summary.getPaidTillDate().toDate())
                                : "Not paid yet";
                            String dueFrom = summary.getDueFromDate() != null
                                ? DateUtils.formatShortDate(summary.getDueFromDate().toDate())
                                : "-";

                            paymentTotalServiceValue.setText(CurrencyUtils.formatCurrency(summary.getTotalServiceAmount()));
                            paymentTotalPaidValue.setText(CurrencyUtils.formatCurrency(summary.getTotalPaidAmount()));
                            paymentOutstandingValue.setText(CurrencyUtils.formatCurrency(summary.getOutstandingAmount()));
                            paymentPaidTillValue.setText(paidTill);
                            paymentDueFromValue.setText(dueFrom);
                        }

                        @Override
                        public void onError(String error) {
                            if (!isUiActive()) return;
                            Toast.makeText(CustomerDetailActivity.this, error, Toast.LENGTH_LONG).show();
                            setPaymentStatusFallback();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isUiActive()) return;
                    Toast.makeText(CustomerDetailActivity.this, error, Toast.LENGTH_LONG).show();
                    setPaymentStatusFallback();
                }
            });
    }

    private void setMonthlySummaryFallback() {
        String monthText = DateUtils.formatMonthYear(new Date());
        monthRingLabel.setText(getString(R.string.month_ring_format, monthText));
        monthValue.setText(monthText);
        deliveredEntriesValue.setText("-");
        totalQuantityValue.setText("-");
        serviceValueValue.setText("-");
    }

    private void setPaymentStatusFallback() {
        paymentTotalServiceValue.setText("-");
        paymentTotalPaidValue.setText("-");
        paymentOutstandingValue.setText("-");
        paymentPaidTillValue.setText("-");
        paymentDueFromValue.setText("-");
    }

    private List<Payment> filterProviderPayments(List<Payment> payments) {
        List<Payment> filtered = new ArrayList<>();
        if (payments == null) return filtered;
        if (providerId == null || providerId.trim().isEmpty()) return filtered;

        for (Payment payment : payments) {
            if (payment == null) continue;
            if (providerId.equals(payment.getProviderId())) {
                filtered.add(payment);
            }
        }
        return filtered;
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }

    private void markDeliveredToday() {
        if (customer == null) {
            showToast("Customer not loaded");
            return;
        }

        if (!isNetworkAvailable()) {
            showToast("Offline mode: please use Service Entry screen");
            return;
        }

        double quantity = customer.getDefaultQuantity() > 0 ? customer.getDefaultQuantity() : 1.0;
        ServiceEntry entry = new ServiceEntry(providerId, customerId, new Timestamp(new Date()), quantity, true);
        entry.setRate(customer.getRatePerUnit());

        double deliveryCost = quantity * customer.getRatePerUnit();
        repo.saveServiceEntryWithTransaction(entry, customerId, deliveryCost,
            new FirestoreRepository.OnSaveCompleteListener() {
                @Override
                public void onSuccess() {
                    if (!isUiActive()) return;
                    Toast.makeText(CustomerDetailActivity.this, "Marked delivered", Toast.LENGTH_SHORT).show();
                    loadMonthlySummary();
                    loadLedgerStatus();
                }

                @Override
                public void onError(String error) {
                    if (!isUiActive()) return;
                    Toast.makeText(CustomerDetailActivity.this, error, Toast.LENGTH_LONG).show();
                }
            });
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
