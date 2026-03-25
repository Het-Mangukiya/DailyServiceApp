package com.dailyserviceapp.sales.repository;

import com.dailyserviceapp.sales.models.SalesPredictionResponse;

import retrofit2.Call;
import retrofit2.Callback;

public interface SalesPredictionRepository {

    Call<SalesPredictionResponse> getGlobalPredictions(
            String providerId,
            int days,
            Callback<SalesPredictionResponse> callback
    );

    Call<SalesPredictionResponse> getProductPredictions(
            String providerId,
            String product,
            int days,
            Callback<SalesPredictionResponse> callback
    );
}
