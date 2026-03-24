package com.dailyserviceapp.core.sync;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.dailyserviceapp.core.utils.Constants;

import java.util.concurrent.TimeUnit;

/**
 * Schedules reliable background sync work for queued offline data.
 */
public final class SyncWorkScheduler {

    private static final String UNIQUE_WORK_SYNC_NOW = "sync_pending_entries_now";
    private static final String UNIQUE_WORK_SYNC_PERIODIC = "sync_pending_entries_periodic";

    private SyncWorkScheduler() {
    }

    public static void enqueueImmediateSync(Context context) {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PendingEntriesSyncWorker.class)
            .setConstraints(constraints)
            .addTag(Constants.WORK_TAG_DATA_SYNC)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build();

        WorkManager.getInstance(context.getApplicationContext())
            .enqueueUniqueWork(UNIQUE_WORK_SYNC_NOW, ExistingWorkPolicy.KEEP, request);
    }

    public static void ensurePeriodicSync(Context context) {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
            PendingEntriesSyncWorker.class,
            6,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(Constants.WORK_TAG_DATA_SYNC)
            .build();

        WorkManager.getInstance(context.getApplicationContext())
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_SYNC_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            );
    }
}
