package com.dailyserviceapp.sales.repository;

import com.dailyserviceapp.sales.models.SalesPredictionResponse;
import retrofit2.Callback;

/**
 * Interface for Sales Prediction Repository.
 * Codex needs to implement this interface.
 */
public interface SalesPredictionRepository {

    /**
     * Fetches predicted data for all services combined.
     */
    void getGlobalPredictions(String providerId, int days, Callback<SalesPredictionResponse> callback);

    /**
     * Fetches predictions for a specific product.
     */
    void getProductPredictions(String providerId, String product, int days, Callback<SalesPredictionResponse> callback);
}
