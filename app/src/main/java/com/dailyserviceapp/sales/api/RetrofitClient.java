package com.dailyserviceapp.sales.api;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {

    private static final String BASE_URL = "https://web-production-f0c6d.up.railway.app/";
    private static final int CONNECT_TIMEOUT_SEC = 15;
    private static final int READ_TIMEOUT_SEC = 20;
    private static final int WRITE_TIMEOUT_SEC = 20;

    private static Retrofit retrofit;

    private RetrofitClient() {
        // Utility class.
    }

    public static SalesPredictionApi getApi() {
        if (retrofit == null || !BASE_URL.equals(retrofit.baseUrl().toString())) {
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(httpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(SalesPredictionApi.class);
    }
}
