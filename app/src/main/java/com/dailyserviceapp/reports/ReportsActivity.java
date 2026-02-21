package com.dailyserviceapp.reports;

import android.os.Bundle;
import android.app.DatePickerDialog;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.animation.Animator;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Bill;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.Payment;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.dailyserviceapp.databinding.ActivityReportsBinding;
import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;
import android.graphics.Color;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Comparator;

public class ReportsActivity extends BaseActivity {

    private ActivityReportsBinding binding;

    private MaterialButton btnStartDate;
    private MaterialButton btnEndDate;
    private MaterialButton btnApply;
    private ProgressBar progressBar;

    private TextView txtTotalRevenue;
    private TextView txtTotalDeliveries;
    private TextView txtTotalCustomers;
    private TextView txtTotalPayments;
    private TextView txtOverdueBills;

    private LinearLayout serviceBreakdownContainer;
    private LinearLayout topCustomersContainer;
    private BarChart barChart;

    private Date startDate;
    private Date endDate;

    private FirestoreRepository repository;
    private String providerId;
    private Map<String, Customer> customerMap = new HashMap<>();
    private final List<ValueAnimator> activeAnimators = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // CRITICAL: Check session first
        if (!isLoggedIn()) {
            showToast("Please login first");
            navigateToLogin();
            return;
        }

        providerId = getCurrentUserId();
        if (providerId == null || providerId.isEmpty()) {
            showToast("Session expired. Please login again.");
            navigateToLogin();
            return;
        }

        repository = new FirestoreRepository();

        MaterialToolbar toolbar = binding.toolbar;
        setupToolbar(toolbar, "Reports", true);

        initViews();
        initDefaultDates();
        setupListeners();
        initChartDefaults();
        loadReport();
    }

    private void initViews() {
        btnStartDate = binding.btnStartDate;
        btnEndDate = binding.btnEndDate;
        btnApply = binding.btnApply;
        progressBar = binding.progressBar;

        txtTotalRevenue = binding.txtTotalRevenue;
        txtTotalDeliveries = binding.txtTotalDeliveries;
        txtTotalCustomers = binding.txtTotalCustomers;
        txtTotalPayments = binding.txtTotalPayments;
        txtOverdueBills = binding.txtOverdueBills;

        serviceBreakdownContainer = binding.serviceBreakdownContainer;
        topCustomersContainer = binding.topCustomersContainer;
        barChart = binding.barChart;
    }

    private void initDefaultDates() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        startDate = calendar.getTime();

        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);
        endDate = end.getTime();

        updateDateButtons();
    }

    private void setupListeners() {
        btnStartDate.setOnClickListener(v -> showDatePicker(true));
        btnEndDate.setOnClickListener(v -> showDatePicker(false));
        btnApply.setOnClickListener(v -> loadReport());
    }

    private void initChartDefaults() {
        // Initialize bar chart only
    }

    private void showDatePicker(boolean isStart) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(isStart ? startDate : endDate);

        DatePickerDialog dialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth);
                Date candidateDate;
                if (isStart) {
                    selected.set(Calendar.HOUR_OF_DAY, 0);
                    selected.set(Calendar.MINUTE, 0);
                    selected.set(Calendar.SECOND, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    candidateDate = selected.getTime();
                    if (candidateDate.after(endDate)) {
                        showToast("Start date must be before end date");
                        return;
                    }
                    startDate = candidateDate;
                } else {
                    selected.set(Calendar.HOUR_OF_DAY, 23);
                    selected.set(Calendar.MINUTE, 59);
                    selected.set(Calendar.SECOND, 59);
                    selected.set(Calendar.MILLISECOND, 999);
                    candidateDate = selected.getTime();
                    if (startDate.after(candidateDate)) {
                        showToast("End date must be after start date");
                        return;
                    }
                    endDate = candidateDate;
                }

                updateDateButtons();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void updateDateButtons() {
        btnStartDate.setText(formatDate(startDate));
        btnEndDate.setText(formatDate(endDate));
    }

    private String formatDate(Date date) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    private void loadReport() {
        if (!isNetworkAvailable()) {
            showToast("No internet connection");
            return;
        }

        showLoading(true);
        serviceBreakdownContainer.removeAllViews();
        topCustomersContainer.removeAllViews();

        loadCustomers(() -> loadEntriesAndPayments());
    }

    private void loadCustomers(Runnable onComplete) {
        repository.getCustomersByProvider(providerId, new FirestoreRepository.OnCustomersLoadedListener() {
            @Override
            public void onCustomersLoaded(List<Customer> customers) {
                runOnUiThread(() -> {
                    if (!isUiActive()) return;
                    customerMap.clear();
                    if (customers != null) {
                        for (Customer customer : customers) {
                            customerMap.put(customer.getId(), customer);
                        }
                    }
                    onComplete.run();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (!isUiActive()) return;
                    showToast("Failed to load customers: " + error);
                    onComplete.run();
                });
            }
        });
    }

    private void loadEntriesAndPayments() {
        Timestamp start = new Timestamp(startDate);
        Calendar nextDay = Calendar.getInstance();
        nextDay.setTime(endDate);
        nextDay.add(Calendar.MILLISECOND, 1);
        Timestamp endExclusive = new Timestamp(nextDay.getTime());

        repository.getDeliveredServiceEntriesByProviderInRange(
            providerId,
            start,
            endExclusive,
            new FirestoreRepository.OnServiceEntriesLoadedListener() {
                @Override
                public void onServiceEntriesLoaded(List<ServiceEntry> entries) {
                    if (!isUiActive()) return;
                    runOnUiThread(() -> {
                        if (!isUiActive()) return;
                        handleEntries(entries);
                    });
                    loadPayments(start, endExclusive);
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        if (!isUiActive()) return;
                        showLoading(false);
                        showToast("Failed to load entries: " + error);
                    });
                }
            }
        );
    }

    private void loadPayments(Timestamp start, Timestamp endExclusive) {
        repository.getPaymentsByProviderAndDate(providerId, start, endExclusive,
            new FirestoreRepository.OnPaymentsLoadedListener() {
                @Override
                public void onPaymentsLoaded(List<Payment> payments) {
                    double totalPayments = 0.0;
                    if (payments != null) {
                        for (Payment payment : payments) {
                            totalPayments += payment.getAmount();
                        }
                    }
                    double finalPayments = totalPayments;
                    if (!isUiActive()) return;
                    runOnUiThread(() -> {
                        if (!isUiActive()) return;
                        animateCounter(txtTotalPayments, 0, finalPayments, true);
                        loadOverdueBills();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        if (!isUiActive()) return;
                        showToast("Failed to load payments: " + error);
                        loadOverdueBills();
                    });
                }
            }
        );
    }

    private void loadOverdueBills() {
        repository.getBillsByProvider(providerId, new FirestoreRepository.OnBillsLoadedListener() {
            @Override
            public void onBillsLoaded(List<Bill> bills) {
                int overdueCount = 0;
                Date now = new Date();
                if (bills != null) {
                    for (Bill bill : bills) {
                        if (bill.getDueDate() != null && bill.getDueDate().toDate().before(now)) {
                            String status = bill.getPaymentStatus() != null ? bill.getPaymentStatus() : "PENDING";
                            if (!"PAID".equals(status)) {
                                overdueCount++;
                            }
                        }
                    }
                }
                int finalOverdue = overdueCount;
                runOnUiThread(() -> {
                    if (!isUiActive()) return;
                    animateCounter(txtOverdueBills, 0, finalOverdue, false);
                    showLoading(false);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (!isUiActive()) return;
                    txtOverdueBills.setText(getString(R.string.overdue_bills_zero));
                    showLoading(false);
                });
            }
        });
    }

    private void handleEntries(List<ServiceEntry> entries) {
        int totalDeliveries = 0;
        double totalRevenue = 0.0;
        Set<String> uniqueCustomers = new HashSet<>();

        Map<String, Double> revenueByService = new HashMap<>();
        Map<String, Double> revenueByCustomer = new HashMap<>();

        if (entries != null) {
            for (ServiceEntry entry : entries) {
                if (entry == null) continue;
                totalDeliveries++;
                uniqueCustomers.add(entry.getCustomerId());

                double rate = entry.getRate();
                Customer customer = customerMap.get(entry.getCustomerId());
                if (rate == 0.0 && customer != null) {
                    rate = customer.getRatePerUnit();
                }

                double amount = rate * entry.getQuantity();
                totalRevenue += amount;

                if (customer != null && customer.getServiceType() != null) {
                    String service = customer.getServiceType();
                    revenueByService.put(service, revenueByService.getOrDefault(service, 0.0) + amount);
                }

                if (entry.getCustomerId() != null) {
                    revenueByCustomer.put(entry.getCustomerId(),
                        revenueByCustomer.getOrDefault(entry.getCustomerId(), 0.0) + amount);
                }
            }
        }

        final double finalTotalRevenue = totalRevenue;
        final int finalTotalDeliveries = totalDeliveries;
        final int finalUniqueCustomers = uniqueCustomers.size();

        Runnable updateUi = () -> {
            animateCounter(txtTotalRevenue, 0, finalTotalRevenue, true);
            animateCounter(txtTotalDeliveries, 0, finalTotalDeliveries, false);
            animateCounter(txtTotalCustomers, 0, finalUniqueCustomers, false);

            renderServiceBreakdown(revenueByService);
            renderTopCustomers(revenueByCustomer);
            updateBarChart(revenueByCustomer);
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            updateUi.run();
        } else {
            runOnUiThread(updateUi);
        }
    }

    private void renderServiceBreakdown(Map<String, Double> revenueByService) {
        serviceBreakdownContainer.removeAllViews();
        if (revenueByService.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No service data available");
            emptyView.setTextSize(14);
            emptyView.setTextColor(Color.GRAY);
            emptyView.setPadding(0, 20, 0, 20);
            serviceBreakdownContainer.addView(emptyView);
            return;
        }

        List<Map.Entry<String, Double>> items = new ArrayList<>(revenueByService.entrySet());
        Collections.sort(items, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        for (int i = 0; i < items.size(); i++) {
            Map.Entry<String, Double> entry = items.get(i);
            serviceBreakdownContainer.addView(createServiceCard(entry.getKey(), entry.getValue(), i));
        }
    }

    private View createServiceCard(String serviceName, double revenue, int index) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(16, 16, 16, 16);
        card.setBackground(getDrawable(android.R.drawable.dialog_holo_light_frame));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        card.setLayoutParams(params);
        
        // Service emoji based on type
        TextView emojiView = new TextView(this);
        emojiView.setTextSize(24);
        emojiView.setPadding(0, 0, 16, 0);
        String emoji = getServiceEmoji(serviceName);
        emojiView.setText(emoji);
        
        // Service details
        LinearLayout detailsLayout = new LinearLayout(this);
        detailsLayout.setOrientation(LinearLayout.VERTICAL);
        detailsLayout.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        ));
        
        TextView nameView = new TextView(this);
        nameView.setText(serviceName);
        nameView.setTextSize(16);
        nameView.setTextColor(Color.parseColor("#1A1A1A"));
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);
        
        TextView revenueView = new TextView(this);
        revenueView.setText(CurrencyUtils.formatIndianCurrency(revenue));
        revenueView.setTextSize(14);
        revenueView.setTextColor(Color.parseColor("#4CAF50"));
        
        detailsLayout.addView(nameView);
        detailsLayout.addView(revenueView);
        
        // Rank badge
        TextView rankView = new TextView(this);
        rankView.setText("#" + (index + 1));
        rankView.setTextSize(18);
        rankView.setTextColor(Color.parseColor("#2196F3"));
        rankView.setTypeface(null, android.graphics.Typeface.BOLD);
        
        card.addView(emojiView);
        card.addView(detailsLayout);
        card.addView(rankView);
        
        return card;
    }
    
    private String getServiceEmoji(String serviceName) {
        String lower = serviceName.toLowerCase();
        if (lower.contains("milk")) return "🥛";
        if (lower.contains("newspaper")) return "📰";
        if (lower.contains("water")) return "💧";
        if (lower.contains("tiffin")) return "🍱";
        if (lower.contains("laundry")) return "👕";
        if (lower.contains("maid")) return "🧹";
        return "📦";
    }

    private void renderTopCustomers(Map<String, Double> revenueByCustomer) {
        topCustomersContainer.removeAllViews();
        if (revenueByCustomer.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No customer data available");
            emptyView.setTextSize(14);
            emptyView.setTextColor(Color.GRAY);
            emptyView.setPadding(0, 20, 0, 20);
            topCustomersContainer.addView(emptyView);
            return;
        }

        List<Map.Entry<String, Double>> items = new ArrayList<>(revenueByCustomer.entrySet());
        Collections.sort(items, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        int limit = Math.min(5, items.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Double> entry = items.get(i);
            Customer customer = customerMap.get(entry.getKey());
            String name = customer != null ? customer.getName() : "Customer";
            topCustomersContainer.addView(createCustomerCard(i + 1, name, entry.getValue()));
        }
    }
    
    private View createCustomerCard(int rank, String customerName, double revenue) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(16, 16, 16, 16);
        card.setBackground(getDrawable(android.R.drawable.dialog_holo_light_frame));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        card.setLayoutParams(params);
        
        // Rank with medal emoji
        TextView rankView = new TextView(this);
        String medalEmoji = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "🏅";
        rankView.setText(medalEmoji + " #" + rank);
        rankView.setTextSize(20);
        rankView.setPadding(0, 0, 16, 0);
        
        // Customer details
        LinearLayout detailsLayout = new LinearLayout(this);
        detailsLayout.setOrientation(LinearLayout.VERTICAL);
        detailsLayout.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        ));
        
        TextView nameView = new TextView(this);
        nameView.setText(customerName);
        nameView.setTextSize(16);
        nameView.setTextColor(Color.parseColor("#1A1A1A"));
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);
        
        TextView revenueView = new TextView(this);
        revenueView.setText(CurrencyUtils.formatIndianCurrency(revenue));
        revenueView.setTextSize(14);
        revenueView.setTextColor(Color.parseColor("#4CAF50"));
        
        detailsLayout.addView(nameView);
        detailsLayout.addView(revenueView);
        
        card.addView(rankView);
        card.addView(detailsLayout);
        
        return card;
    }

    private void updateBarChart(Map<String, Double> revenueByCustomer) {
        if (barChart == null) return;
        if (revenueByCustomer == null || revenueByCustomer.isEmpty()) {
            barChart.clear();
            barChart.setNoDataText("No customer data available");
            return;
        }

        List<Map.Entry<String, Double>> items = new ArrayList<>(revenueByCustomer.entrySet());
        Collections.sort(items, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        int limit = Math.min(5, items.size());
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        // Modern gradient colors
        int[] colors = new int[]{
            Color.parseColor("#4CAF50"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#F44336")
        };

        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Double> entry = items.get(i);
            Customer customer = customerMap.get(entry.getKey());
            String name = customer != null ? customer.getName() : "Customer";
            entries.add(new BarEntry(i, entry.getValue().floatValue()));
            labels.add(name);
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(13f);
        dataSet.setValueTextColor(Color.parseColor("#1A1A1A"));
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return CurrencyUtils.formatCompactCurrency(value);
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.8f);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setExtraBottomOffset(10f);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(12f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        barChart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return CurrencyUtils.formatCompactCurrency(value);
            }
        });
        barChart.getAxisLeft().setTextSize(11f);
        barChart.getAxisLeft().setDrawGridLines(true);
        barChart.getAxisLeft().setGridColor(Color.parseColor("#E0E0E0"));

        barChart.animateY(1000);
        barChart.invalidate();
    }
    
    private void animateCounter(TextView textView, double from, double to, boolean isCurrency) {
        ValueAnimator animator = ValueAnimator.ofFloat((float) from, (float) to);
        animator.setDuration(1200);
        animator.setInterpolator(new DecelerateInterpolator());
        activeAnimators.add(animator);
        
        animator.addUpdateListener(animation -> {
            if (!isUiActive() || textView == null || !textView.isAttachedToWindow()) {
                return;
            }
            float value = (float) animation.getAnimatedValue();
            if (isCurrency) {
                textView.setText(CurrencyUtils.formatIndianCurrency(value));
            } else {
                textView.setText(String.valueOf((int) value));
            }
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                activeAnimators.remove(animator);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                activeAnimators.remove(animator);
            }
        });
        
        animator.start();
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnApply != null) {
            btnApply.setEnabled(!show);
        }
    }

    private boolean isUiActive() {
        return binding != null && !isFinishing() && !isDestroyed();
    }

    @Override
    protected void onDestroy() {
        List<ValueAnimator> animators = new ArrayList<>(activeAnimators);
        activeAnimators.clear();
        for (ValueAnimator animator : animators) {
            animator.cancel();
        }
        super.onDestroy();
        binding = null;
    }
}
