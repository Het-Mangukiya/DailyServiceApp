package com.dailyserviceapp.sales.api;

import com.dailyserviceapp.sales.models.SalesPredictionResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface definition for Sales Prediction ML API.
 * The implementation and Retrofit client setup should be handled by Codex.
 */
public interface SalesPredictionApi {

    /**
     * Gets category-wise aggregate predictions for all services.
     */
    @GET("predict/all")
    Call<SalesPredictionResponse> getGlobalPredictions(
            @Query("providerId") String providerId,
            @Query("days") int days
    );

    /**
     * Gets predictions for a specific product/service.
     */
    @GET("predict")
    Call<SalesPredictionResponse> getProductPredictions(
            @Query("providerId") String providerId,
            @Query("product") String product,
            @Query("days") int days
    );
}
