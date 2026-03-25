package com.dailyserviceapp.sales.ui;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.sales.models.SalesPredictionResponse;
import com.dailyserviceapp.sales.repository.SalesPredictionRepository;
import com.dailyserviceapp.sales.repository.SalesPredictionRepositoryImpl;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SalesPredictionActivity extends BaseActivity {

    private static final String TAG = "SalesPredictionUI";

    private TextView tvTotalQuantity;
    private TextView tvTotalRevenue;
    private TextView tvTopCategory;
    private TextView tvCategoryBreakdown;
    private TextView tvLastUpdated;
    private NestedScrollView contentScroll;
    private LinearLayout categoryContainer;
    private PieChart pieChart;
    private ProgressBar progressBar;
    private MaterialButton btnRetry;
    private View headerCard;
    private View statsRow;
    private View chartCard;
    private View categoryCard;

    private SalesPredictionRepository repository;
    private Call<SalesPredictionResponse> inFlightCall;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_prediction);

        contentScroll = findViewById(R.id.contentScroll);
        tvTotalQuantity = findViewById(R.id.tvTotalQuantity);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTopCategory = findViewById(R.id.tvTopCategory);
        tvCategoryBreakdown = findViewById(R.id.tvCategoryBreakdown);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        categoryContainer = findViewById(R.id.categoryContainer);
        pieChart = findViewById(R.id.pieChart);
        progressBar = findViewById(R.id.progressBar);
        btnRetry = findViewById(R.id.btnRetry);
        headerCard = findViewById(R.id.headerCard);
        statsRow = findViewById(R.id.statsRow);
        chartCard = findViewById(R.id.chartCard);
        categoryCard = findViewById(R.id.categoryCard);

        tvLastUpdated.setText("Last updated: --");
        tvTopCategory.setText("Top Category: --");

        setupPieChart();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnRetry.setOnClickListener(v -> fetchPredictionsData());

        repository = new SalesPredictionRepositoryImpl();
        fetchPredictionsData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (inFlightCall != null && !inFlightCall.isCanceled()) {
            inFlightCall.cancel();
        }
    }

    private void fetchPredictionsData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String providerId = user.getUid();
        int daysToPredict = 30;

        if (inFlightCall != null && !inFlightCall.isCanceled()) {
            inFlightCall.cancel();
        }

        setLoadingState(true);
        inFlightCall = repository.getGlobalPredictions(providerId, daysToPredict,
                new Callback<SalesPredictionResponse>() {
                    @Override
                    public void onResponse(Call<SalesPredictionResponse> call,
                                           Response<SalesPredictionResponse> response) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        setLoadingState(false);
                        if (response.isSuccessful() && response.body() != null) {
                            populateUI(response.body());
                            updateLastUpdatedNow();
                            return;
                        }

                        String backendMessage = extractBackendErrorMessage(response);
                        String err = (backendMessage != null && !backendMessage.trim().isEmpty())
                                ? backendMessage
                                : "API Error: " + response.code();
                        String userMessage = toUserFriendlyPredictionMessage(err);

                        setSummaryDefaults();
                        pieChart.setVisibility(View.GONE);
                        categoryContainer.removeAllViews();
                        tvCategoryBreakdown.setText(userMessage);
                        btnRetry.setVisibility(View.VISIBLE);
                        Log.e(TAG, "Prediction API failure (" + response.code() + "): " + err);
                        Toast.makeText(SalesPredictionActivity.this,
                                userMessage,
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(Call<SalesPredictionResponse> call, Throwable t) {
                        if (call.isCanceled() || isFinishing() || isDestroyed()) {
                            return;
                        }

                        setLoadingState(false);
                        String errStr = "Network Error: " + t.getMessage()
                                + "\nPlease check your internet connection and try again.";
                        setSummaryDefaults();
                        pieChart.setVisibility(View.GONE);
                        categoryContainer.removeAllViews();
                        tvCategoryBreakdown.setText(errStr);
                        btnRetry.setVisibility(View.VISIBLE);
                        Log.e(TAG, "Retrofit error", t);
                    }
                });
    }

    private void setLoadingState(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (contentScroll != null) {
            contentScroll.setAlpha(loading ? 0.55f : 1f);
        }
        if (loading) {
            btnRetry.setVisibility(View.GONE);
            tvCategoryBreakdown.setText("Loading predictions...");
        }
    }

    private void setSummaryDefaults() {
        tvTotalQuantity.setText("0.0 units");
        tvTotalRevenue.setText(CurrencyUtils.formatIndianCurrency(0));
        tvTopCategory.setText("Top Category: --");
    }

    private void updateLastUpdatedNow() {
        String timestamp = DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT,
                Locale.getDefault()
        ).format(new Date());
        tvLastUpdated.setText("Last updated: " + timestamp);
    }

    private String toUserFriendlyPredictionMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return "Unable to load predictions right now. Please try again.";
        }

        String normalized = rawMessage.toLowerCase(Locale.getDefault());
        if (normalized.contains("no active customers")) {
            return "No active customers found for predictions. Add ACTIVE customers and service entries, then try again.";
        }
        if (normalized.contains("available_categories")) {
            return "No prediction data for the selected service yet. Try another active category or add service entries.";
        }
        if (normalized.contains("api error: 404")) {
            return "Prediction data was not found. Add recent service activity and try again.";
        }
        return rawMessage;
    }

    private String extractBackendErrorMessage(Response<SalesPredictionResponse> response) {
        if (response == null || response.errorBody() == null) {
            return null;
        }

        try {
            String raw = response.errorBody().string();
            if (raw == null || raw.trim().isEmpty()) {
                return null;
            }

            JSONObject json = new JSONObject(raw);
            if (json.has("error")) {
                return json.optString("error", null);
            }
            if (json.has("message")) {
                return json.optString("message", null);
            }
            return raw;
        } catch (Exception e) {
            Log.w(TAG, "Could not parse backend error body", e);
            return null;
        }
    }

    private void setupPieChart() {
        pieChart.setDrawHoleEnabled(true);
        pieChart.setUsePercentValues(true);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setEntryLabelColor(Color.DKGRAY);
        pieChart.setCenterText("Revenue\nby Category");
        pieChart.setCenterTextSize(15f);
        pieChart.setRotationEnabled(true);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setTransparentCircleRadius(58f);
        pieChart.setHoleRadius(45f);
        pieChart.setExtraOffsets(8f, 8f, 8f, 8f);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(true);
    }

    private void populateUI(SalesPredictionResponse data) {
        if (data == null) {
            setSummaryDefaults();
            pieChart.setVisibility(View.GONE);
            categoryContainer.removeAllViews();
            tvCategoryBreakdown.setText("No prediction data available right now.");
            btnRetry.setVisibility(View.VISIBLE);
            return;
        }

        animateQuantityCounter(data.getOverallPredictedQuantity());
        animateRevenueCounter(data.getOverallPredictedRevenue());

        List<CategoryUiItem> categories = new ArrayList<>();
        List<PieEntry> entries = new ArrayList<>();
        double maxRevenue = 0d;
        double totalRevenue = Math.max(0d, data.getOverallPredictedRevenue());

        if (data.getCategoryPredictions() != null) {
            for (Map.Entry<String, SalesPredictionResponse.CategoryPrediction> item
                    : data.getCategoryPredictions().entrySet()) {
                String category = item.getKey();
                SalesPredictionResponse.CategoryPrediction cp = item.getValue();
                if (cp == null) {
                    continue;
                }

                CategoryUiItem uiItem = new CategoryUiItem();
                uiItem.category = category;
                uiItem.activeCustomers = cp.activeCustomers;
                uiItem.dailyRevenue = cp.dailyRevenue;
                uiItem.monthlyRevenue = cp.totalPredictedRevenue;
                categories.add(uiItem);
                maxRevenue = Math.max(maxRevenue, cp.totalPredictedRevenue);

                if (cp.totalPredictedRevenue > 0) {
                    entries.add(new PieEntry((float) cp.totalPredictedRevenue, category));
                }
            }
        }

        categories.sort((a, b) -> Double.compare(b.monthlyRevenue, a.monthlyRevenue));

        if (categories.isEmpty()) {
            tvCategoryBreakdown.setText("No active customers/categories found to run predictions on.");
            pieChart.setVisibility(View.GONE);
            categoryContainer.removeAllViews();
            btnRetry.setVisibility(View.VISIBLE);
            return;
        }

        CategoryUiItem top = categories.get(0);
        tvTopCategory.setText(String.format(
                Locale.getDefault(),
                "Top Category: %s (%s)",
                top.category,
                CurrencyUtils.formatIndianCurrency(top.monthlyRevenue)
        ));

        renderCategoryCards(categories, maxRevenue, totalRevenue);
        tvCategoryBreakdown.setText(String.format(
                Locale.getDefault(),
                "Based on %d categories across next %d days.",
                categories.size(),
                Math.max(1, data.getDays())
        ));
        btnRetry.setVisibility(View.GONE);

        if (entries.isEmpty()) {
            pieChart.setVisibility(View.GONE);
            startSectionRevealAnimation();
            return;
        }

        pieChart.setVisibility(View.VISIBLE);
        PieDataSet dataSet = new PieDataSet(entries, "Categories");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(5f);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.DKGRAY);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart));
        pieChart.setData(pieData);
        pieChart.animateY(950, Easing.EaseInOutQuad);
        pieChart.animateX(700, Easing.EaseOutCubic);
        pieChart.invalidate();

        startSectionRevealAnimation();
    }

    private void animateQuantityCounter(double targetQuantity) {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, (float) targetQuantity);
        animator.setDuration(900);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            tvTotalQuantity.setText(String.format(Locale.US, "%.1f units", value));
        });
        animator.start();
    }

    private void animateRevenueCounter(double targetRevenue) {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, (float) targetRevenue);
        animator.setDuration(1050);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            tvTotalRevenue.setText(CurrencyUtils.formatIndianCurrency(value));
        });
        animator.start();
    }

    private void renderCategoryCards(List<CategoryUiItem> categories,
                                     double maxRevenue,
                                     double totalRevenue) {
        categoryContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < categories.size(); i++) {
            CategoryUiItem item = categories.get(i);
            View row = inflater.inflate(R.layout.row_sales_prediction_category, categoryContainer, false);

            TextView tvName = row.findViewById(R.id.tvCategoryName);
            TextView tvShare = row.findViewById(R.id.tvCategoryShare);
            TextView tvDaily = row.findViewById(R.id.tvDailyRevenue);
            TextView tvMonthly = row.findViewById(R.id.tvMonthlyRevenue);
            TextView tvCustomers = row.findViewById(R.id.tvActiveCustomers);
            LinearProgressIndicator progress = row.findViewById(R.id.progressCategory);

            double sharePct = totalRevenue > 0d ? (item.monthlyRevenue * 100d / totalRevenue) : 0d;
            int revenueStrength = maxRevenue > 0d
                    ? (int) Math.max(0, Math.min(100, (item.monthlyRevenue * 100d / maxRevenue)))
                    : 0;

            tvName.setText(item.category);
            tvShare.setText(String.format(Locale.getDefault(), "%.1f%%", sharePct));
            tvDaily.setText("Daily: " + CurrencyUtils.formatIndianCurrency(item.dailyRevenue));
            tvMonthly.setText("30-day: " + CurrencyUtils.formatIndianCurrency(item.monthlyRevenue));
            tvCustomers.setText(String.format(Locale.getDefault(), "%d active", item.activeCustomers));
            progress.setProgress(0);

            categoryContainer.addView(row);

            long delay = 80L * i;
            row.setAlpha(0f);
            row.setTranslationY(16f);
            row.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(delay)
                    .setDuration(280)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            row.postDelayed(() -> progress.setProgressCompat(revenueStrength, true), delay + 120L);
        }
    }

    private void startSectionRevealAnimation() {
        View[] sections = new View[]{headerCard, statsRow, chartCard, categoryCard};
        for (int i = 0; i < sections.length; i++) {
            View section = sections[i];
            if (section == null) {
                continue;
            }

            section.setAlpha(0f);
            section.setTranslationY(22f);
            section.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(i * 70L)
                    .setDuration(320)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private static class CategoryUiItem {
        String category;
        int activeCustomers;
        double dailyRevenue;
        double monthlyRevenue;
    }
}
