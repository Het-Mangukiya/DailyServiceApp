package com.dailyserviceapp.sales.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class SalesPredictionResponse {

    @SerializedName(value = "provider_id", alternate = {"providerId"})
    private String providerId;

    @SerializedName("days")
    private int days;

    @SerializedName(value = "total_categories", alternate = {"totalCategories"})
    private int totalCategories;

    @SerializedName(value = "overall_predicted_quantity", alternate = {"overallPredictedQuantity"})
    private double overallPredictedQuantity;

    @SerializedName(value = "overall_predicted_revenue", alternate = {"overallPredictedRevenue"})
    private double overallPredictedRevenue;

    @SerializedName(value = "category_predictions", alternate = {"categoryPredictions"})
    private Map<String, CategoryPrediction> categoryPredictions;

    public String getProviderId() {
        return providerId;
    }

    public int getDays() {
        return days;
    }

    public int getTotalCategories() {
        return totalCategories;
    }

    public double getOverallPredictedQuantity() {
        return overallPredictedQuantity;
    }

    public double getOverallPredictedRevenue() {
        return overallPredictedRevenue;
    }

    public Map<String, CategoryPrediction> getCategoryPredictions() {
        return categoryPredictions;
    }

    public static class CategoryPrediction {
        @SerializedName(value = "active_customers", alternate = {"activeCustomers"})
        public int activeCustomers;

        @SerializedName(value = "daily_quantity", alternate = {"dailyQuantity"})
        public double dailyQuantity;

        @SerializedName(value = "daily_revenue", alternate = {"dailyRevenue"})
        public double dailyRevenue;

        @SerializedName(value = "total_predicted_quantity", alternate = {"totalPredictedQuantity"})
        public double totalPredictedQuantity;

        @SerializedName(value = "total_predicted_revenue", alternate = {"totalPredictedRevenue"})
        public double totalPredictedRevenue;

        @SerializedName(value = "customer_breakdown", alternate = {"customerBreakdown"})
        public List<CustomerBreakdown> customerBreakdown;
    }

    public static class CustomerBreakdown {
        @SerializedName(value = "name", alternate = {"customerName"})
        public String name;

        @SerializedName(value = "daily_quantity", alternate = {"dailyQuantity"})
        public double dailyQuantity;

        @SerializedName(value = "rate_per_unit", alternate = {"ratePerUnit"})
        public double ratePerUnit;
    }
}
