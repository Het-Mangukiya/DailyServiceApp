package com.dailyserviceapp.sales.models;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import java.util.List;

public class SalesPredictionResponse {
    
    @SerializedName("provider_id")
    private String providerId;
    
    @SerializedName("days")
    private int days;
    
    @SerializedName("total_categories")
    private int totalCategories;
    
    @SerializedName("overall_predicted_quantity")
    private double overallPredictedQuantity;
    
    @SerializedName("overall_predicted_revenue")
    private double overallPredictedRevenue;

    @SerializedName("category_predictions")
    private Map<String, CategoryPrediction> categoryPredictions;

    public String getProviderId() { return providerId; }
    public int getDays() { return days; }
    public int getTotalCategories() { return totalCategories; }
    public double getOverallPredictedQuantity() { return overallPredictedQuantity; }
    public double getOverallPredictedRevenue() { return overallPredictedRevenue; }
    public Map<String, CategoryPrediction> getCategoryPredictions() { return categoryPredictions; }

    public static class CategoryPrediction {
        @SerializedName("active_customers")
        public int activeCustomers;
        
        @SerializedName("daily_quantity")
        public double dailyQuantity;
        
        @SerializedName("daily_revenue")
        public double dailyRevenue;
        
        @SerializedName("total_predicted_quantity")
        public double totalPredictedQuantity;
        
        @SerializedName("total_predicted_revenue")
        public double totalPredictedRevenue;
        
        @SerializedName("customer_breakdown")
        public List<CustomerBreakdown> customerBreakdown;
    }
    
    public static class CustomerBreakdown {
        @SerializedName("name")
        public String name;
        @SerializedName("daily_quantity")
        public double dailyQuantity;
        @SerializedName("rate_per_unit")
        public double ratePerUnit;
    }
}
