package com.dailyserviceapp.sales.repository;

import com.dailyserviceapp.sales.api.RetrofitClient;
import com.dailyserviceapp.sales.api.SalesPredictionApi;
import com.dailyserviceapp.sales.models.SalesPredictionResponse;
import retrofit2.Callback;

public class SalesPredictionRepositoryImpl implements SalesPredictionRepository {

    private final SalesPredictionApi api;

    public SalesPredictionRepositoryImpl() {
        this.api = RetrofitClient.getApi();
    }

    @Override
    public void getGlobalPredictions(String providerId, int days, Callback<SalesPredictionResponse> callback) {
        api.getGlobalPredictions(providerId, days).enqueue(callback);
    }

    @Override
    public void getProductPredictions(String providerId, String product, int days, Callback<SalesPredictionResponse> callback) {
        api.getProductPredictions(providerId, product, days).enqueue(callback);
    }
}
