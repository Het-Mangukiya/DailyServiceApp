package com.dailyserviceapp.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dailyserviceapp.R;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.PaymentStatus;
import com.google.android.material.button.MaterialButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CustomerDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CUSTOMER_ID = "customerId";

    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyyMM");

    private FirestoreRepository repo;
    private String customerId;

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

        customerId = getIntent().getStringExtra(EXTRA_CUSTOMER_ID);
        if (customerId == null || customerId.trim().isEmpty()) {
            finish();
            return;
        }

        repo = new FirestoreRepository();

        customerName = findViewById(R.id.customerName);
        customerMeta = findViewById(R.id.customerMeta);
        monthlySummaryBody = findViewById(R.id.monthlySummaryBody);
        paymentStatusBody = findViewById(R.id.paymentStatusBody);
        markDeliveredButton = findViewById(R.id.markDeliveredButton);

        markDeliveredButton.setOnClickListener(v -> repo.markDeliveredToday(
                customerId,
                unused -> {
                    Toast.makeText(this, "Marked delivered", Toast.LENGTH_SHORT).show();
                    loadMonthly();
                },
                e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
        ));
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
                + "\nRate: " + customer.getRatePerUnit();
        customerMeta.setText(meta);
    }

    private void loadMonthly() {
        if (customer == null) return;

        String monthKey = LocalDate.now().format(MONTH_KEY);

        repo.countDeliveredInMonth(customerId, monthKey,
                deliveredCount -> {
                    double total = deliveredCount * customer.getRatePerUnit();
                    monthlySummaryBody.setText("Delivered days: " + deliveredCount + "\nBill: " + total);
                    loadPayment(monthKey, total);
                },
                e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    private void loadPayment(String monthKey, double billTotal) {
        repo.getPaymentStatus(customerId, monthKey,
                status -> renderPayment(status, billTotal),
                e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    private void renderPayment(PaymentStatus status, double billTotal) {
        if (status.isPaid()) {
            paymentStatusBody.setText("Paid: " + status.getPaidAmount());
        } else {
            paymentStatusBody.setText("Unpaid\nDue: " + billTotal);
        }
    }
}
