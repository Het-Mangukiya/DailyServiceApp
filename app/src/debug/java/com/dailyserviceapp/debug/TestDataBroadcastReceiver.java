package com.dailyserviceapp.debug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.dailyserviceapp.utils.TestDataGenerator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class TestDataBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "TestDataReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No logged-in user. Login first, then re-run broadcast.");
            toast(context, "Login first, then run test-data broadcast again.");
            return;
        }

        String providerId = user.getUid();
        Log.i(TAG, "Generating test data for providerId=" + providerId);
        toast(context, "Generating test data...");

        TestDataGenerator generator = new TestDataGenerator(context.getApplicationContext(), providerId);
        generator.generateCompleteTestData(new TestDataGenerator.OnTestDataGeneratedListener() {
            @Override
            public void onTestDataGenerated(List<String> customerIds, int entriesCount) {
                Log.i(TAG, "Generated customers=" + customerIds.size() + ", entries=" + entriesCount);
                toast(context, "Generated " + customerIds.size() + " customers + " + entriesCount + " entries");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Test data generation failed: " + error);
                toast(context, "Test data generation failed: " + error);
            }
        });
    }

    private static void toast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }
}
