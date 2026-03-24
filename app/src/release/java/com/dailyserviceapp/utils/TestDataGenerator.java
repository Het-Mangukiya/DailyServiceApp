package com.dailyserviceapp.utils;

import android.content.Context;

import java.util.List;

/**
 * Release-safe placeholder for debug-only test data generation.
 */
public class TestDataGenerator {

    public TestDataGenerator(Context context, String providerId) {
        // No-op in release builds.
    }

    public void generateCompleteTestData(OnTestDataGeneratedListener listener) {
        if (listener != null) {
            listener.onError("Test data generation is available only in debug builds.");
        }
    }

    public interface OnCustomersGeneratedListener {
        void onCustomersGenerated(List<String> customerIds);
        void onError(String error);
    }

    public interface OnTestDataGeneratedListener {
        void onTestDataGenerated(List<String> customerIds, int entriesCount);
        void onError(String error);
    }
}
