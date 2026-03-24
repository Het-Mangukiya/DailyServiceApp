package com.dailyserviceapp.sales.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.sales.models.SalesPredictionResponse;
import com.dailyserviceapp.sales.repository.SalesPredictionRepository;
import com.dailyserviceapp.sales.repository.SalesPredictionRepositoryImpl;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SalesPredictionActivity extends AppCompatActivity {

    private final String TAG = "SalesPredictionUI";
    private TextView tvTotalQuantity, tvTotalRevenue, tvCategoryBreakdown;
    private PieChart pieChart;
    private ProgressBar progressBar;
    private SalesPredictionRepository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_prediction);

        tvTotalQuantity = findViewById(R.id.tvTotalQuantity);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvCategoryBreakdown = findViewById(R.id.tvCategoryBreakdown);
        pieChart = findViewById(R.id.pieChart);
        progressBar = findViewById(R.id.progressBar);

        setupPieChart();

        // Standard setup: hook up back button
        findViewById(R.id.topAppBar).setOnClickListener(v -> finish());
        
        repository = new SalesPredictionRepositoryImpl();
        fetchPredictionsData();
    }

    private void fetchPredictionsData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        String providerId = user.getUid();
        int daysToPredict = 30; // 30-day outlook

        progressBar.setVisibility(View.VISIBLE);
        repository.getGlobalPredictions(providerId, daysToPredict, new Callback<SalesPredictionResponse>() {
            @Override
            public void onResponse(Call<SalesPredictionResponse> call, Response<SalesPredictionResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    populateUI(response.body());
                } else {
                    String err = "API Error: " + response.code();
                    tvCategoryBreakdown.setText(err);
                    Log.e(TAG, err);
                    Toast.makeText(SalesPredictionActivity.this, "Failed to load ML data. Is local server running?", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<SalesPredictionResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                String errStr = "Network Error: " + t.getMessage() + "\nEnsure ML Backend is running.";
                tvCategoryBreakdown.setText(errStr);
                Log.e(TAG, "Retrofit Error", t);
            }
        });
    }

    private void setupPieChart() {
        pieChart.setDrawHoleEnabled(true);
        pieChart.setUsePercentValues(true);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setCenterText("Revenue\nby Category");
        pieChart.setCenterTextSize(14f);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(true);
    }

    private void populateUI(SalesPredictionResponse data) {
        tvTotalQuantity.setText(String.format("%.1f units", data.getOverallPredictedQuantity()));
        tvTotalRevenue.setText(CurrencyUtils.formatIndianCurrency(data.getOverallPredictedRevenue()));

        StringBuilder sb = new StringBuilder();
        List<PieEntry> entries = new ArrayList<>();
        
        if (data.getCategoryPredictions() != null) {
            for (String category : data.getCategoryPredictions().keySet()) {
                SalesPredictionResponse.CategoryPrediction cp = data.getCategoryPredictions().get(category);
                sb.append("• ").append(category).append("\n");
                sb.append("   Active Customers: ").append(cp.activeCustomers).append("\n");
                sb.append("   Daily Revenue: ").append(CurrencyUtils.formatIndianCurrency(cp.dailyRevenue)).append("\n");
                sb.append("   30-Day Subtotal: ").append(CurrencyUtils.formatIndianCurrency(cp.totalPredictedRevenue)).append("\n\n");
                
                if (cp.totalPredictedRevenue > 0) {
                    entries.add(new PieEntry((float) cp.totalPredictedRevenue, category));
                }
            }
        }
        
        if (sb.length() == 0) {
            tvCategoryBreakdown.setText("No active customers/categories found to run predictions on.");
            pieChart.setVisibility(View.GONE);
        } else {
            tvCategoryBreakdown.setText(sb.toString());
            if (!entries.isEmpty()) {
                pieChart.setVisibility(View.VISIBLE);
                PieDataSet dataSet = new PieDataSet(entries, "Categories");
                dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                dataSet.setValueTextSize(12f);
                dataSet.setValueTextColor(Color.BLACK);
                
                PieData pieData = new PieData(dataSet);
                pieChart.setData(pieData);
                pieChart.invalidate(); // refresh
            } else {
                pieChart.setVisibility(View.GONE);
            }
        }
    }
}
