package com.dailyserviceapp.reports;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Bill;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.Payment;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.dailyserviceapp.databinding.ActivityReportsBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ReportsActivity extends BaseActivity {

    private ActivityReportsBinding binding;

    private MaterialButton btnStartDate;
    private MaterialButton btnEndDate;
    private MaterialButton btnApply;
    private MaterialButton btnChartBar;
    private MaterialButton btnChartPie;
    private ProgressBar progressBar;

    private TextView txtTotalRevenue;
    private TextView txtTotalDeliveries;
    private TextView txtTotalCustomers;
    private TextView txtTotalPayments;
    private TextView txtOverdueBills;

    private LinearLayout serviceBreakdownContainer;
    private LinearLayout topCustomersContainer;
    private BarChart barChart;
    private PieChart pieChart;

    private Date startDate;
    private Date endDate;

    private FirestoreRepository repository;
    private String providerId;
    private final Map<String, Customer> customerMap = new HashMap<>();
    private final List<ValueAnimator> activeAnimators = new ArrayList<>();
    private int pendingLoadOperations;
    private boolean showPieChart = false;
    private Map<String, Double> latestRevenueByCustomer = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        btnChartBar = binding.btnChartBar;
        btnChartPie = binding.btnChartPie;
        progressBar = binding.progressBar;

        txtTotalRevenue = binding.txtTotalRevenue;
        txtTotalDeliveries = binding.txtTotalDeliveries;
        txtTotalCustomers = binding.txtTotalCustomers;
        txtTotalPayments = binding.txtTotalPayments;
        txtOverdueBills = binding.txtOverdueBills;

        serviceBreakdownContainer = binding.serviceBreakdownContainer;
        topCustomersContainer = binding.topCustomersContainer;
        barChart = binding.barChart;
        pieChart = binding.pieChart;
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

        binding.chartToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || !isUiActive()) {
                return;
            }
            showPieChart = checkedId == R.id.btnChartPie;
            applyChartMode(showPieChart, true);
        });
    }

    private void initChartDefaults() {
        configureBarChart();
        configurePieChart();
        applyChartMode(false, false);
    }

    private void configureBarChart() {
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setFitBars(true);
        barChart.setNoDataText("No revenue data for selected range");
        barChart.setNoDataTextColor(ContextCompat.getColor(this, R.color.md_theme_on_surface_variant));

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.md_theme_on_surface_variant));
        xAxis.setTextSize(11f);

        barChart.getAxisLeft().setTextColor(ContextCompat.getColor(this, R.color.md_theme_on_surface_variant));
        barChart.getAxisLeft().setTextSize(10f);
        barChart.getAxisLeft().setGridColor(ContextCompat.getColor(this, R.color.md_theme_outline));
        barChart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return CurrencyUtils.formatCompactCurrency(value);
            }
        });
    }

    private void configurePieChart() {
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setNoDataText("No revenue data for selected range");
        pieChart.setNoDataTextColor(ContextCompat.getColor(this, R.color.md_theme_on_surface_variant));
        pieChart.setCenterText("Customer\nShare");
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(ContextCompat.getColor(this, R.color.md_theme_on_surface));
        pieChart.setHoleRadius(62f);
        pieChart.setTransparentCircleRadius(66f);
        pieChart.setEntryLabelColor(ContextCompat.getColor(this, R.color.md_theme_on_surface));
        pieChart.setEntryLabelTextSize(11f);

        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(11f);
        legend.setTextColor(ContextCompat.getColor(this, R.color.md_theme_on_surface_variant));
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
    }

    private void applyChartMode(boolean pie, boolean animate) {
        if (!isUiActive()) return;
        barChart.setVisibility(pie ? View.GONE : View.VISIBLE);
        pieChart.setVisibility(pie ? View.VISIBLE : View.GONE);

        btnChartBar.setEnabled(pie);
        btnChartPie.setEnabled(!pie);

        if (animate) {
            if (pie) {
                pieChart.animateY(700);
            } else {
                barChart.animateY(700);
            }
        }
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
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    private void loadReport() {
        if (!isNetworkAvailable()) {
            showToast("No internet connection");
            return;
        }

        cancelActiveAnimators();
        showLoading(true);
        serviceBreakdownContainer.removeAllViews();
        topCustomersContainer.removeAllViews();
        latestRevenueByCustomer = new HashMap<>();
        barChart.clear();
        pieChart.clear();

        loadCustomers(this::loadReportDataInParallel);
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

    private void loadReportDataInParallel() {
        Timestamp start = new Timestamp(startDate);
        Calendar nextDay = Calendar.getInstance();
        nextDay.setTime(endDate);
        nextDay.add(Calendar.MILLISECOND, 1);
        Timestamp endExclusive = new Timestamp(nextDay.getTime());

        beginLoadOperations(3);

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
                        finishLoadOperation();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        if (!isUiActive()) return;
                        showToast("Failed to load entries: " + error);
                        handleEntries(new ArrayList<>());
                        finishLoadOperation();
                    });
                }
            }
        );

        loadPayments(start, endExclusive);
        loadOverdueBills();
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
                        finishLoadOperation();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        if (!isUiActive()) return;
                        showToast("Failed to load payments: " + error);
                        animateCounter(txtTotalPayments, 0, 0, true);
                        finishLoadOperation();
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
                    finishLoadOperation();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (!isUiActive()) return;
                    txtOverdueBills.setText(getString(R.string.overdue_bills_zero));
                    finishLoadOperation();
                });
            }
        });
    }

    private void beginLoadOperations(int count) {
        pendingLoadOperations = Math.max(0, count);
        if (pendingLoadOperations == 0) {
            showLoading(false);
        }
    }

    private void finishLoadOperation() {
        if (pendingLoadOperations <= 0) {
            showLoading(false);
            return;
        }
        pendingLoadOperations--;
        if (pendingLoadOperations == 0) {
            showLoading(false);
        }
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
                    revenueByCustomer.put(
                        entry.getCustomerId(),
                        revenueByCustomer.getOrDefault(entry.getCustomerId(), 0.0) + amount
                    );
                }
            }
        }

        final double finalTotalRevenue = totalRevenue;
        final int finalTotalDeliveries = totalDeliveries;
        final int finalUniqueCustomers = uniqueCustomers.size();

        latestRevenueByCustomer = new HashMap<>(revenueByCustomer);

        Runnable updateUi = () -> {
            animateCounter(txtTotalRevenue, 0, finalTotalRevenue, true);
            animateCounter(txtTotalDeliveries, 0, finalTotalDeliveries, false);
            animateCounter(txtTotalCustomers, 0, finalUniqueCustomers, false);

            renderServiceBreakdown(revenueByService);
            renderTopCustomers(revenueByCustomer);
            updateBarChart(revenueByCustomer);
            updatePieChart(revenueByCustomer);
            applyChartMode(showPieChart, false);
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
            addEmptyListRow(serviceBreakdownContainer, "No service data available");
            return;
        }

        List<Map.Entry<String, Double>> items = new ArrayList<>(revenueByService.entrySet());
        Collections.sort(items, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        double total = 0.0;
        for (Map.Entry<String, Double> item : items) {
            total += item.getValue();
        }

        for (Map.Entry<String, Double> entry : items) {
            double percent = total > 0 ? (entry.getValue() * 100.0 / total) : 0.0;
            String title = entry.getKey();
            String subtitle = String.format(Locale.getDefault(), "Contribution: %.1f%%", percent);
            String value = CurrencyUtils.formatIndianCurrency(entry.getValue());
            serviceBreakdownContainer.addView(createInsightRow(serviceBreakdownContainer, title, subtitle, value));
        }
    }

    private void renderTopCustomers(Map<String, Double> revenueByCustomer) {
        topCustomersContainer.removeAllViews();
        if (revenueByCustomer.isEmpty()) {
            addEmptyListRow(topCustomersContainer, "No customer data available");
            return;
        }

        List<Map.Entry<String, Double>> items = new ArrayList<>(revenueByCustomer.entrySet());
        Collections.sort(items, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        int limit = Math.min(5, items.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Double> entry = items.get(i);
            Customer customer = customerMap.get(entry.getKey());
            String name = customer != null ? customer.getName() : "Customer";
            String title = (i + 1) + ". " + name;
            String subtitle = "Top customer revenue";
            String value = CurrencyUtils.formatIndianCurrency(entry.getValue());
            topCustomersContainer.addView(createInsightRow(topCustomersContainer, title, subtitle, value));
        }
    }

    private View createInsightRow(LinearLayout parent, String title, String subtitle, String value) {
        View row = getLayoutInflater().inflate(R.layout.row_customer_history, parent, false);

        TextView txtTitle = row.findViewById(R.id.txtHistoryTitle);
        TextView txtSubtitle = row.findViewById(R.id.txtHistorySubtitle);
        TextView txtValue = row.findViewById(R.id.txtHistoryValue);

        txtTitle.setText(title);
        txtSubtitle.setText(subtitle);
        txtValue.setText(value);
        txtValue.setTextColor(ContextCompat.getColor(this, R.color.md_theme_primary_dark));

        return row;
    }

    private void addEmptyListRow(LinearLayout container, String message) {
        TextView emptyView = new TextView(this);
        emptyView.setText(message);
        emptyView.setTextSize(14);
        emptyView.setTextColor(ContextCompat.getColor(this, R.color.md_theme_on_surface_variant));
        emptyView.setPadding(0, 14, 0, 14);
        container.addView(emptyView);
    }

    private void updateBarChart(Map<String, Double> revenueByCustomer) {
        if (barChart == null) return;
        if (revenueByCustomer == null || revenueByCustomer.isEmpty()) {
            barChart.clear();
            barChart.invalidate();
            return;
        }

        List<Map.Entry<String, Double>> items = new ArrayList<>(revenueByCustomer.entrySet());
        Collections.sort(items, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        int limit = Math.min(5, items.size());
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int[] colors = new int[] {
            ContextCompat.getColor(this, R.color.chart_pending),
            ContextCompat.getColor(this, R.color.chart_paid),
            ContextCompat.getColor(this, R.color.brand_sky_500),
            ContextCompat.getColor(this, R.color.chart_overdue),
            ContextCompat.getColor(this, R.color.color_reports)
        };

        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Double> entry = items.get(i);
            Customer customer = customerMap.get(entry.getKey());
            String name = customer != null ? customer.getName() : "Customer";
            entries.add(new BarEntry(i, entry.getValue().floatValue()));
            labels.add(ellipsizeLabel(name));
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.md_theme_on_surface));
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return CurrencyUtils.formatCompactCurrency(value);
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.75f);
        barChart.setData(data);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        barChart.animateY(700);
        barChart.invalidate();
    }

    private void updatePieChart(Map<String, Double> revenueByCustomer) {
        if (pieChart == null) return;
        if (revenueByCustomer == null || revenueByCustomer.isEmpty()) {
            pieChart.clear();
            pieChart.invalidate();
            return;
        }

        List<Map.Entry<String, Double>> items = new ArrayList<>(revenueByCustomer.entrySet());
        Collections.sort(items, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        int limit = Math.min(4, items.size());
        List<PieEntry> entries = new ArrayList<>();
        float others = 0f;
        for (int i = 0; i < items.size(); i++) {
            Map.Entry<String, Double> item = items.get(i);
            Customer customer = customerMap.get(item.getKey());
            String label = customer != null ? customer.getName() : "Customer";
            float value = item.getValue().floatValue();
            if (i < limit) {
                entries.add(new PieEntry(value, ellipsizeLabel(label)));
            } else {
                others += value;
            }
        }
        if (others > 0f) {
            entries.add(new PieEntry(others, "Others"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(4f);
        dataSet.setColors(new int[] {
            ContextCompat.getColor(this, R.color.chart_pending),
            ContextCompat.getColor(this, R.color.chart_paid),
            ContextCompat.getColor(this, R.color.brand_sky_500),
            ContextCompat.getColor(this, R.color.chart_overdue),
            ContextCompat.getColor(this, R.color.color_reports)
        });

        PieData data = new PieData(dataSet);
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(11f);
        data.setValueFormatter(new PercentFormatter(pieChart));

        pieChart.setData(data);
        pieChart.animateY(700);
        pieChart.invalidate();
    }

    private String ellipsizeLabel(String value) {
        if (value == null) return "-";
        String trimmed = value.trim();
        if (trimmed.length() <= 10) {
            return trimmed;
        }
        return trimmed.substring(0, 10) + "…";
    }

    private void animateCounter(TextView textView, double from, double to, boolean isCurrency) {
        ValueAnimator animator = ValueAnimator.ofFloat((float) from, (float) to);
        animator.setDuration(900);
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

    private void cancelActiveAnimators() {
        List<ValueAnimator> snapshot = new ArrayList<>(activeAnimators);
        activeAnimators.clear();
        for (ValueAnimator animator : snapshot) {
            animator.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        cancelActiveAnimators();
        super.onDestroy();
        binding = null;
    }
}
