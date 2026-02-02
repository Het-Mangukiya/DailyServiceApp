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

    /**
     * Handles the incoming broadcast to trigger generation of app test data for the currently signed-in Firebase user.
     *
     * If no user is signed in, displays a toast prompting the user to sign in and returns. If a user is signed in,
     * initiates generation of complete test data for that user's UID and reports progress/results via toasts and log messages;
     * on success, shows the number of generated customers and entries, and on error shows the error message.
     *
     * @param context the Context in which the receiver is running
     * @param intent the Intent that triggered this broadcast
     */
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

    /**
     * Displays a Toast with the given message on the application's main thread.
     *
     * @param context the Context used to create the Toast
     * @param message the text to show in the Toast
     */
    private static void toast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }
}