package com.dailyserviceapp.sales.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // Hosted URL for the deployed ML Backend
    private static final String BASE_URL = "https://web-production-f0c6d.up.railway.app/";
    private static Retrofit retrofit = null;

    public static SalesPredictionApi getApi() {
        if (retrofit == null || !retrofit.baseUrl().toString().equals(BASE_URL)) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(SalesPredictionApi.class);
    }
}
