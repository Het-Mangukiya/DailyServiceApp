package com.dailyserviceapp.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.Bill;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CustomerDetailActivity extends BaseActivity {

    public static final String EXTRA_CUSTOMER_ID = "customerId";

    private FirestoreRepository repo;
    private String customerId;
    private String providerId;

    private TextView customerName;
    private TextView customerMeta;
    private TextView monthlySummaryBody;
    private TextView paymentStatusBody;
    private MaterialButton markDeliveredButton;

    private Customer customer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);

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

        customerName = findViewById(R.id.customerName);
        customerMeta = findViewById(R.id.customerMeta);
        monthlySummaryBody = findViewById(R.id.monthlySummaryBody);
        paymentStatusBody = findViewById(R.id.paymentStatusBody);
        markDeliveredButton = findViewById(R.id.markDeliveredButton);

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
                    customer = doc.toObject(Customer.class);
                    if (customer == null) {
                        Toast.makeText(this, "Customer not found", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    renderCustomer();
                    loadMonthly();
                },
                e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    private void renderCustomer() {
        customerName.setText(customer.getName());
        String meta = (customer.getServiceType() == null ? "" : customer.getServiceType())
                + "\nRate: " + CurrencyUtils.formatCurrency(customer.getRatePerUnit());
        customerMeta.setText(meta);
    }

    private void loadMonthly() {
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
                        int deliveredCount = 0;
                        double total = 0.0;

                        if (entries != null) {
                            for (ServiceEntry entry : entries) {
                                if (entry != null && entry.isDelivered()) {
                                    deliveredCount++;
                                    double rate = entry.getRate() > 0 ? entry.getRate() : customer.getRatePerUnit();
                                    total += rate * entry.getQuantity();
                                }
                            }
                        }

                        monthlySummaryBody.setText("Delivered days: " + deliveredCount +
                                "\nBill: " + CurrencyUtils.formatCurrency(total));
                        loadPaymentStatus(monthStart.get(Calendar.MONTH), monthStart.get(Calendar.YEAR), total);
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(CustomerDetailActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void loadPaymentStatus(int month, int year, double fallbackTotal) {
        repo.getBillsByCustomer(customerId, new FirestoreRepository.OnBillsLoadedListener() {
            @Override
            public void onBillsLoaded(List<Bill> bills) {
                Bill matchingBill = null;
                if (bills != null) {
                    for (Bill bill : bills) {
                        if (bill.getMonth() == month && bill.getYear() == year) {
                            matchingBill = bill;
                            break;
                        }
                    }
                }

                if (matchingBill == null) {
                    paymentStatusBody.setText("Not billed yet");
                    return;
                }

                String status = matchingBill.getPaymentStatus() != null ? matchingBill.getPaymentStatus() : "PENDING";
                double total = matchingBill.getTotalAmount() > 0 ? matchingBill.getTotalAmount() : fallbackTotal;

                if ("PAID".equals(status)) {
                    paymentStatusBody.setText("Paid\nTotal: " + CurrencyUtils.formatCurrency(total));
                } else if ("PARTIAL".equals(status)) {
                    paymentStatusBody.setText("Partially paid\nTotal: " + CurrencyUtils.formatCurrency(total));
                } else if ("OVERDUE".equals(status)) {
                    paymentStatusBody.setText("Overdue\nDue: " + CurrencyUtils.formatCurrency(total));
                } else {
                    paymentStatusBody.setText("Unpaid\nDue: " + CurrencyUtils.formatCurrency(total));
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CustomerDetailActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
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
                        Toast.makeText(CustomerDetailActivity.this, "Marked delivered", Toast.LENGTH_SHORT).show();
                        loadMonthly();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(CustomerDetailActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
