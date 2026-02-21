package com.dailyserviceapp.billing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.core.utils.DateUtils;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.Payment;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.dailyserviceapp.databinding.ActivityBillListNewBinding;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Customer-wise billing ledger derived from service entries and payments.
 */
public class BillListActivity extends BaseActivity {

    private static final long MANUAL_REFRESH_COOLDOWN_MS = 3000L;
    private static final long CACHE_FRESH_WINDOW_MS = 60000L;
    private static final Map<String, CacheEntry> LEDGER_CACHE = new HashMap<>();

    private ActivityBillListNewBinding binding;

    private TextView selectedMonthText;
    private MaterialButton previousMonthButton;
    private MaterialButton nextMonthButton;
    private RecyclerView billsRecyclerView;
    private LinearLayout emptyStateLayout;
    private ExtendedFloatingActionButton generateBillsFab;
    private android.widget.ProgressBar loadingProgress;

    private FirestoreRepository repository;
    private BillAdapter adapter;
    private String providerId;

    private List<CustomerLedgerSummary> currentSummaries = new ArrayList<>();
    private boolean isLoadingData = false;
    private long lastManualRefreshAt = 0L;

    private static class CacheEntry {
        final List<CustomerLedgerSummary> summaries;
        final long cachedAt;

        CacheEntry(List<CustomerLedgerSummary> summaries, long cachedAt) {
            this.summaries = summaries;
            this.cachedAt = cachedAt;
        }
    }

    private static class LedgerFetchState {
        List<ServiceEntry> entries = new ArrayList<>();
        List<Payment> payments = new ArrayList<>();
        int completedRequests = 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBillListNewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!isLoggedIn()) {
            showToast("Please login first");
            navigateToLogin();
            return;
        }

        MaterialToolbar toolbar = binding.toolbar;
        setupToolbar(toolbar, "Bills", true);

        initializeViews();
        initializeData();
        setupClickListeners();

        restoreCachedSummaries();
        loadData(false);
    }

    private void initializeViews() {
        selectedMonthText = binding.selectedMonthText;
        previousMonthButton = binding.previousMonthButton;
        nextMonthButton = binding.nextMonthButton;
        billsRecyclerView = binding.billsRecyclerView;
        emptyStateLayout = binding.emptyState;
        generateBillsFab = binding.generateBillsFab;
        loadingProgress = binding.loadingProgress;

        billsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        billsRecyclerView.setHasFixedSize(true);

        selectedMonthText.setText("Live customer ledger");
        previousMonthButton.setVisibility(View.GONE);
        nextMonthButton.setVisibility(View.GONE);
        generateBillsFab.setText("Refresh");
    }

    private void initializeData() {
        repository = new FirestoreRepository();
        providerId = getCurrentUserId();

        adapter = new BillAdapter(new BillAdapter.OnBillActionListener() {
            @Override
            public void onViewDetails(CustomerLedgerSummary summary) {
                openBillDetails(summary);
            }

            @Override
            public void onShareBill(CustomerLedgerSummary summary) {
                shareSummary(summary);
            }
        });
        billsRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        generateBillsFab.setOnClickListener(v -> loadData(true));
    }

    private void loadData(boolean manualRefresh) {
        if (providerId == null || providerId.isEmpty()) {
            showToast("Please login again");
            showEmptyState(true);
            adapter.submitData(new ArrayList<>());
            return;
        }

        if (isLoadingData) {
            if (manualRefresh) {
                showToast("Refresh already in progress");
            }
            return;
        }

        long now = System.currentTimeMillis();
        if (manualRefresh && (now - lastManualRefreshAt) < MANUAL_REFRESH_COOLDOWN_MS) {
            showToast("Please wait before refreshing again");
            return;
        }

        if (!manualRefresh && hasFreshCache()) {
            return;
        }

        if (!isNetworkAvailable()) {
            if (currentSummaries.isEmpty()) {
                showToast("No internet connection");
                showEmptyState(true);
            } else {
                showToast("No internet connection. Showing last loaded ledger.");
            }
            return;
        }

        if (manualRefresh) {
            lastManualRefreshAt = now;
        }

        isLoadingData = true;
        showLoading(true);

        repository.getCustomersByProvider(providerId, new FirestoreRepository.OnCustomersLoadedListener() {
            @Override
            public void onCustomersLoaded(List<Customer> customers) {
                if (customers == null || customers.isEmpty()) {
                    isLoadingData = false;
                    showLoading(false);
                    currentSummaries.clear();
                    adapter.submitData(new ArrayList<>());
                    selectedMonthText.setText("No customers found");
                    showEmptyState(true);
                    return;
                }
                fetchLedgerDataInParallel(customers);
            }

            @Override
            public void onError(String error) {
                isLoadingData = false;
                showLoading(false);
                showToast("Error loading customers: " + error);
                if (currentSummaries.isEmpty()) {
                    showEmptyState(true);
                }
            }
        });
    }

    private void fetchLedgerDataInParallel(List<Customer> customers) {
        final LedgerFetchState state = new LedgerFetchState();

        Runnable maybeBuild = () -> {
            state.completedRequests++;
            if (state.completedRequests == 2) {
                buildSummaries(customers, state.entries, state.payments);
            }
        };

        repository.getDeliveredServiceEntriesByProvider(providerId,
            new FirestoreRepository.OnServiceEntriesLoadedListener() {
                @Override
                public void onServiceEntriesLoaded(List<ServiceEntry> entries) {
                    state.entries = entries != null ? entries : new ArrayList<>();
                    maybeBuild.run();
                }

                @Override
                public void onError(String error) {
                    showToast("Warning: service entries could not be loaded (" + error + ")");
                    state.entries = new ArrayList<>();
                    maybeBuild.run();
                }
            });

        repository.getPaymentsByProvider(providerId, new FirestoreRepository.OnPaymentsLoadedListener() {
            @Override
            public void onPaymentsLoaded(List<Payment> payments) {
                state.payments = payments != null ? payments : new ArrayList<>();
                maybeBuild.run();
            }

            @Override
            public void onError(String error) {
                showToast("Warning: payments could not be loaded (" + error + ")");
                state.payments = new ArrayList<>();
                maybeBuild.run();
            }
        });
    }

    private void buildSummaries(List<Customer> customers, List<ServiceEntry> entries, List<Payment> payments) {
        Map<String, List<ServiceEntry>> entriesByCustomer = new HashMap<>();
        if (entries != null) {
            for (ServiceEntry entry : entries) {
                if (entry == null || entry.getCustomerId() == null) continue;
                entriesByCustomer
                    .computeIfAbsent(entry.getCustomerId(), key -> new ArrayList<>())
                    .add(entry);
            }
        }

        Map<String, List<Payment>> paymentsByCustomer = new HashMap<>();
        if (payments != null) {
            for (Payment payment : payments) {
                if (payment == null || payment.getCustomerId() == null) continue;
                paymentsByCustomer
                    .computeIfAbsent(payment.getCustomerId(), key -> new ArrayList<>())
                    .add(payment);
            }
        }

        List<CustomerLedgerSummary> summaries = new ArrayList<>();
        for (Customer customer : customers) {
            if (customer == null || customer.getId() == null) continue;
            CustomerLedgerSummary summary = CustomerLedgerCalculator.calculate(
                customer,
                entriesByCustomer.get(customer.getId()),
                paymentsByCustomer.get(customer.getId())
            );
            summaries.add(summary);
        }

        summaries.sort((s1, s2) -> {
            int dueCompare = Double.compare(s2.getOutstandingAmount(), s1.getOutstandingAmount());
            if (dueCompare != 0) return dueCompare;

            String n1 = s1.getCustomerName() != null ? s1.getCustomerName() : "";
            String n2 = s2.getCustomerName() != null ? s2.getCustomerName() : "";
            return n1.compareToIgnoreCase(n2);
        });

        currentSummaries = summaries;
        cacheSummaries(summaries);
        adapter.submitData(summaries);
        isLoadingData = false;
        showLoading(false);
        showEmptyState(summaries.isEmpty());
        updateHeader(summaries);
    }

    private void updateHeader(List<CustomerLedgerSummary> summaries) {
        double outstanding = 0.0;
        double paid = 0.0;
        for (CustomerLedgerSummary summary : summaries) {
            outstanding += summary.getOutstandingAmount();
            paid += summary.getTotalPaidAmount();
        }
        String header = "Customers: " + summaries.size()
            + " • Due: " + CurrencyUtils.formatCurrency(outstanding)
            + " • Paid: " + CurrencyUtils.formatCurrency(paid);
        selectedMonthText.setText(header);
    }

    private void openBillDetails(CustomerLedgerSummary summary) {
        if (summary == null || summary.getCustomerId() == null) {
            showToast("Unable to open customer ledger");
            return;
        }
        Intent intent = new Intent(this, BillDetailActivity.class);
        intent.putExtra("customerId", summary.getCustomerId());
        intent.putExtra("customerName", summary.getCustomerName());
        startActivity(intent);
    }

    private void shareSummary(CustomerLedgerSummary summary) {
        if (summary == null) return;

        String dueFrom = summary.getDueFromDate() != null
            ? DateUtils.formatShortDate(summary.getDueFromDate().toDate()) : "-";
        String paidTill = summary.getPaidTillDate() != null
            ? DateUtils.formatShortDate(summary.getPaidTillDate().toDate()) : "Not paid yet";

        String text = "Customer: " + summary.getCustomerName() + "\n"
            + "Outstanding: " + CurrencyUtils.formatCurrency(summary.getOutstandingAmount()) + "\n"
            + "Paid Till: " + paidTill + "\n"
            + "Due From: " + dueFrom + "\n"
            + "Delivered Entries: " + summary.getDeliveredEntries() + "\n"
            + "Total Received: " + CurrencyUtils.formatCurrency(summary.getTotalPaidAmount());

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Customer Ledger - " + summary.getCustomerName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(shareIntent, "Share ledger"));
    }

    private void showEmptyState(boolean show) {
        if (show) {
            billsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            billsRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    private void showLoading(boolean show) {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        if (generateBillsFab != null) {
            generateBillsFab.setEnabled(!show);
            generateBillsFab.setText(show ? "Refreshing..." : "Refresh");
        }

        if (show && currentSummaries.isEmpty()) {
            billsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    private void restoreCachedSummaries() {
        CacheEntry cacheEntry = LEDGER_CACHE.get(cacheKey());
        if (cacheEntry == null || cacheEntry.summaries == null || cacheEntry.summaries.isEmpty()) {
            return;
        }

        currentSummaries = new ArrayList<>(cacheEntry.summaries);
        adapter.submitData(currentSummaries);
        showEmptyState(false);
        updateHeader(currentSummaries);
    }

    private boolean hasFreshCache() {
        CacheEntry cacheEntry = LEDGER_CACHE.get(cacheKey());
        if (cacheEntry == null) return false;
        return (System.currentTimeMillis() - cacheEntry.cachedAt) < CACHE_FRESH_WINDOW_MS;
    }

    private void cacheSummaries(List<CustomerLedgerSummary> summaries) {
        if (providerId == null || providerId.trim().isEmpty() || summaries == null) {
            return;
        }
        LEDGER_CACHE.put(cacheKey(), new CacheEntry(new ArrayList<>(summaries), System.currentTimeMillis()));
    }

    private String cacheKey() {
        return providerId == null ? "unknown" : providerId;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
