package com.dailyserviceapp.core.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.dailyserviceapp.core.offline.OfflineCache;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Background worker that syncs queued offline service entries to Firestore.
 */
public class PendingEntriesSyncWorker extends Worker {

    private static final String TAG = "PendingEntriesSync";

    private final FirebaseFirestore firestore;
    private final OfflineCache offlineCache;

    public PendingEntriesSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.firestore = FirebaseFirestore.getInstance();
        this.offlineCache = new OfflineCache(context.getApplicationContext());
    }

    @NonNull
    @Override
    public Result doWork() {
        List<OfflineCache.PendingServiceEntry> pendingEntries = offlineCache.getPendingEntries();
        if (pendingEntries.isEmpty()) {
            return Result.success();
        }

        List<OfflineCache.PendingServiceEntry> retryEntries = new ArrayList<>();
        int syncedCount = 0;
        int droppedCount = 0;

        for (OfflineCache.PendingServiceEntry pending : pendingEntries) {
            SyncOutcome outcome = syncSingleEntry(pending);
            if (outcome == SyncOutcome.RETRY) {
                retryEntries.add(pending);
            } else if (outcome == SyncOutcome.SYNCED) {
                syncedCount++;
            } else {
                droppedCount++;
            }
        }

        offlineCache.replacePendingEntries(retryEntries);
        if (syncedCount > 0 || droppedCount > 0) {
            offlineCache.markSyncCompleted();
        }

        Log.d(TAG, "Sync pass complete. synced=" + syncedCount
            + ", dropped=" + droppedCount + ", retry=" + retryEntries.size());

        return retryEntries.isEmpty() ? Result.success() : Result.retry();
    }

    private SyncOutcome syncSingleEntry(OfflineCache.PendingServiceEntry pending) {
        if (!isValid(pending)) {
            Log.w(TAG, "Dropping invalid pending entry");
            return SyncOutcome.DROPPED;
        }

        try {
            persistEntryWithTransaction(pending);
            return SyncOutcome.SYNCED;
        } catch (Exception e) {
            Throwable root = unwrap(e);
            if (isPermanentFailure(root)) {
                Log.w(TAG, "Dropping permanent-failure entry: " + root.getMessage());
                return SyncOutcome.DROPPED;
            }
            Log.w(TAG, "Retrying transient sync failure", root);
            return SyncOutcome.RETRY;
        }
    }

    private boolean isValid(OfflineCache.PendingServiceEntry pending) {
        return pending != null
            && pending.providerId != null && !pending.providerId.trim().isEmpty()
            && pending.customerId != null && !pending.customerId.trim().isEmpty()
            && pending.timestamp > 0
            && pending.quantity > 0
            && pending.amount >= 0;
    }

    private void persistEntryWithTransaction(OfflineCache.PendingServiceEntry pending)
        throws ExecutionException, InterruptedException, TimeoutException {
        DocumentReference customerRef = firestore.collection(Constants.COLLECTION_CUSTOMERS)
            .document(pending.customerId);

        ServiceEntry entry = pending.toServiceEntry();
        if (pending.quantity > 0) {
            entry.setRate(pending.amount / pending.quantity);
        }

        Calendar targetDay = Calendar.getInstance();
        targetDay.setTimeInMillis(pending.timestamp);
        String entryId = buildEntryId(pending.customerId, targetDay);
        DocumentReference entryRef = firestore.collection(Constants.COLLECTION_SERVICE_ENTRIES)
            .document(entryId);

        Tasks.await(firestore.runTransaction(transaction -> {
            DocumentSnapshot customerDoc = transaction.get(customerRef);
            if (!customerDoc.exists()) {
                throw new IllegalStateException("Customer missing for pending sync");
            }

            DocumentSnapshot existingEntry = transaction.get(entryRef);
            if (existingEntry != null && existingEntry.exists()) {
                throw new IllegalStateException("Duplicate service entry for day");
            }

            Double currentLent = customerDoc.getDouble("lentAmount");
            if (currentLent == null) {
                currentLent = 0.0;
            }

            transaction.update(customerRef, "lentAmount", currentLent + pending.amount);

            transaction.set(entryRef, entry);
            return null;
        }), 30, TimeUnit.SECONDS);
    }

    private String buildEntryId(String customerId, Calendar day) {
        int year = day.get(Calendar.YEAR);
        int month = day.get(Calendar.MONTH) + 1;
        int dayOfMonth = day.get(Calendar.DAY_OF_MONTH);
        return customerId + "_" + String.format(java.util.Locale.US, "%04d%02d%02d", year, month, dayOfMonth);
    }

    private boolean isPermanentFailure(Throwable throwable) {
        if (throwable instanceof IllegalStateException || throwable instanceof IllegalArgumentException) {
            return true;
        }

        if (throwable instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) throwable).getCode();
            return code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                || code == FirebaseFirestoreException.Code.UNAUTHENTICATED
                || code == FirebaseFirestoreException.Code.INVALID_ARGUMENT
                || code == FirebaseFirestoreException.Code.NOT_FOUND;
        }

        return false;
    }

    private Throwable unwrap(Throwable throwable) {
        if (throwable instanceof ExecutionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

    private enum SyncOutcome {
        SYNCED,
        RETRY,
        DROPPED
    }
}
