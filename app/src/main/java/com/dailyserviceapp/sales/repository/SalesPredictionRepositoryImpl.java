package com.dailyserviceapp.sales.repository;

import com.dailyserviceapp.sales.api.RetrofitClient;
import com.dailyserviceapp.sales.api.SalesPredictionApi;
import com.dailyserviceapp.sales.models.SalesPredictionResponse;

import retrofit2.Call;
import retrofit2.Callback;

public class SalesPredictionRepositoryImpl implements SalesPredictionRepository {

    private final SalesPredictionApi api;

    public SalesPredictionRepositoryImpl() {
        this.api = RetrofitClient.getApi();
    }

    @Override
    public Call<SalesPredictionResponse> getGlobalPredictions(
            String providerId,
            int days,
            Callback<SalesPredictionResponse> callback
    ) {
        Call<SalesPredictionResponse> call = api.getGlobalPredictions(providerId, days);
        call.enqueue(callback);
        return call;
    }

    @Override
    public Call<SalesPredictionResponse> getProductPredictions(
            String providerId,
            String product,
            int days,
            Callback<SalesPredictionResponse> callback
    ) {
        Call<SalesPredictionResponse> call = api.getProductPredictions(providerId, product, days);
        call.enqueue(callback);
        return call;
    }
}
