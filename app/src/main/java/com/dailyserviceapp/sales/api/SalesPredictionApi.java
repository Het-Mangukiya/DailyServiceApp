package com.dailyserviceapp.sales.api;

import com.dailyserviceapp.sales.models.SalesPredictionResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SalesPredictionApi {

    @GET("predict/all")
    Call<SalesPredictionResponse> getGlobalPredictions(
            @Query("providerId") String providerId,
            @Query("days") int days
    );

    @GET("predict")
    Call<SalesPredictionResponse> getProductPredictions(
            @Query("providerId") String providerId,
            @Query("product") String product,
            @Query("days") int days
    );
}
