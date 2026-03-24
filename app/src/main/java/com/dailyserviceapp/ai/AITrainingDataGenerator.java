package com.dailyserviceapp.ai;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.dailyserviceapp.core.utils.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generates AI training datasets from complaint/support ticket data.
 * Exports JSONL files into the app's documents directory.
 *
 * <p>Each line is a JSON object with the complaint details.</p>
 */
public class AITrainingDataGenerator {

    private static final String TAG = "AITrainingDataGenerator";

    public interface GenerationCallback {
        void onSuccess(@NonNull File outputFile, int recordCount);
        void onError(@NonNull Exception error);
    }

    private final FirebaseFirestore firestore;
    private final Handler mainHandler;

    public AITrainingDataGenerator() {
        this(FirebaseFirestore.getInstance());
    }

    public AITrainingDataGenerator(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Generates a JSONL training dataset for the provider's support tickets.
     */
    public void generateComplaintTrainingData(
        @NonNull Context context,
        @NonNull String providerId,
        @NonNull GenerationCallback callback
    ) {
        if (providerId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Provider id is required"));
            return;
        }

        firestore.collection(Constants.COLLECTION_SUPPORT_TICKETS)
            .whereEqualTo("providerId", providerId)
            .limit(1000)
            .get()
            .addOnSuccessListener(snapshot -> {
                List<DocumentSnapshot> docs = snapshot != null
                    ? snapshot.getDocuments() : new ArrayList<>();

                new Thread(() -> writeDataset(context, providerId, docs, callback)).start();
            })
            .addOnFailureListener(error -> {
                Log.e(TAG, "Failed to load support tickets", error);
                callback.onError(error);
            });
    }

    private void writeDataset(
        @NonNull Context context,
        @NonNull String providerId,
        @NonNull List<DocumentSnapshot> docs,
        @NonNull GenerationCallback callback
    ) {
        try {
            File docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (docsDir == null) {
                throw new IllegalStateException("Unable to access documents directory");
            }
            if (!docsDir.exists() && !docsDir.mkdirs()) {
                throw new IllegalStateException("Unable to create documents directory");
            }

            String safeProvider = sanitize(providerId);
            String fileName = "ai_training_support_tickets_" + safeProvider + "_"
                + System.currentTimeMillis() + ".jsonl";
            File outFile = new File(docsDir, fileName);

            int recordCount = 0;
            try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {

                for (DocumentSnapshot doc : docs) {
                    if (doc == null || !doc.exists()) continue;
                    JSONObject record = buildTrainingRecord(doc);
                    writer.write(record.toString());
                    writer.newLine();
                    recordCount++;
                }
            }

            int finalCount = recordCount;
            mainHandler.post(() -> callback.onSuccess(outFile, finalCount));
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate training dataset", e);
            mainHandler.post(() -> callback.onError(e));
        }
    }

    private JSONObject buildTrainingRecord(@NonNull DocumentSnapshot doc) {
        JSONObject record = new JSONObject();

        putString(record, "recordType", "support_ticket");
        putString(record, "ticketId", doc.getId());
        putString(record, "providerId", safe(doc.getString("providerId")));
        putString(record, "providerName", safe(doc.getString("providerName")));
        putString(record, "providerEmail", safe(doc.getString("providerEmail")));
        putString(record, "customerId", safe(doc.getString("customerId")));
        putString(record, "customerName", safe(doc.getString("customerName")));
        putString(record, "customerEmail", safe(doc.getString("customerEmail")));
        putString(record, "category", safe(doc.getString("category")));
        putString(record, "subject", safe(doc.getString("subject")));
        putString(record, "message", safe(doc.getString("message")));
        putString(record, "status", safe(doc.getString("status")).toUpperCase(Locale.US));
        putTimestamp(record, "createdAtEpochMs", doc.getTimestamp("createdAt"));
        putTimestamp(record, "updatedAtEpochMs", doc.getTimestamp("updatedAt"));
        putTimestamp(record, "resolvedAtEpochMs", doc.getTimestamp("resolvedAt"));

        return record;
    }

    private void putString(JSONObject record, String key, String value) {
        try {
            record.put(key, value == null ? "" : value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to write JSON field: " + key, e);
        }
    }

    private void putTimestamp(JSONObject record, String key, Timestamp timestamp) {
        try {
            record.put(key, timestamp == null ? JSONObject.NULL : timestamp.toDate().getTime());
        } catch (Exception e) {
            Log.e(TAG, "Failed to write timestamp field: " + key, e);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String sanitize(String raw) {
        String safe = raw == null ? "" : raw.trim();
        if (safe.isEmpty()) {
            return "provider";
        }
        return safe.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
