package com.dailyserviceapp.core.offline;

import android.content.Context;
import android.content.SharedPreferences;

import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.google.firebase.Timestamp;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Offline cache manager for storing data locally when network is unavailable.
 * Uses SharedPreferences and Gson for simple key-value persistence.
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-28
 */
public class OfflineCache {
    
    private static final String PREF_NAME = "offline_cache";
    private static final String KEY_CUSTOMERS = "cached_customers";
    private static final String KEY_PENDING_ENTRIES = "pending_entries";
    private static final String KEY_LAST_SYNC = "last_sync_time";
    
    private final SharedPreferences prefs;
    private final Gson gson;
    private final Object syncLock = new Object();
    
    public OfflineCache(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }
    
    /**
     * Cache customer list for offline access
     */
    public void cacheCustomers(List<Customer> customers) {
        synchronized (syncLock) {
            String json = gson.toJson(customers);
            prefs.edit()
                .putString(KEY_CUSTOMERS, json)
                .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                .apply();
        }
    }
    
    /**
     * Retrieve cached customers
     */
    public List<Customer> getCachedCustomers() {
        synchronized (syncLock) {
            String json = prefs.getString(KEY_CUSTOMERS, null);
            if (json == null) {
                return new ArrayList<>();
            }
            Type type = new TypeToken<List<Customer>>(){}.getType();
            return gson.fromJson(json, type);
        }
    }
    
    /**
     * Queue a service entry for later sync when offline
     */
    public void queuePendingEntry(PendingServiceEntry entry) {
        synchronized (syncLock) {
            List<PendingServiceEntry> pending = getPendingEntries();
            pending.add(entry);
            setPendingEntries(pending);
        }
    }
    
    /**
     * Get all pending service entries waiting to be synced
     */
    public List<PendingServiceEntry> getPendingEntries() {
        synchronized (syncLock) {
            String json = prefs.getString(KEY_PENDING_ENTRIES, null);
            if (json == null) {
                return new ArrayList<>();
            }
            Type type = new TypeToken<List<PendingServiceEntry>>(){}.getType();
            List<PendingServiceEntry> entries = gson.fromJson(json, type);
            return entries != null ? entries : new ArrayList<>();
        }
    }

    /**
     * Replace all pending entries after a sync pass.
     */
    public void replacePendingEntries(List<PendingServiceEntry> entries) {
        synchronized (syncLock) {
            setPendingEntries(entries);
        }
    }

    /**
     * Reconciles a sync pass without clobbering entries queued concurrently.
     * Keeps entries added after snapshot retrieval and appends retry entries.
     */
    public void reconcilePendingEntriesAfterSync(List<PendingServiceEntry> processedSnapshot,
                                                 List<PendingServiceEntry> retryEntries) {
        synchronized (syncLock) {
            List<PendingServiceEntry> current = getPendingEntries();
            Set<String> processedKeys = new HashSet<>();
            if (processedSnapshot != null) {
                for (PendingServiceEntry entry : processedSnapshot) {
                    processedKeys.add(pendingKey(entry));
                }
            }

            List<PendingServiceEntry> merged = new ArrayList<>();
            Set<String> mergedKeys = new HashSet<>();
            for (PendingServiceEntry entry : current) {
                String key = pendingKey(entry);
                if (processedKeys.contains(key)) {
                    continue;
                }
                if (mergedKeys.add(key)) {
                    merged.add(entry);
                }
            }

            if (retryEntries != null) {
                for (PendingServiceEntry entry : retryEntries) {
                    String key = pendingKey(entry);
                    if (mergedKeys.add(key)) {
                        merged.add(entry);
                    }
                }
            }

            setPendingEntries(merged);
        }
    }
    
    /**
     * Clear pending entries after successful sync
     */
    public void clearPendingEntries() {
        synchronized (syncLock) {
            prefs.edit().remove(KEY_PENDING_ENTRIES).apply();
        }
    }
    
    /**
     * Get timestamp of last successful sync
     */
    public long getLastSyncTime() {
        synchronized (syncLock) {
            return prefs.getLong(KEY_LAST_SYNC, 0);
        }
    }

    /**
     * Mark sync completion timestamp.
     */
    public void markSyncCompleted() {
        synchronized (syncLock) {
            prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply();
        }
    }
    
    /**
     * Check if cached data exists
     */
    public boolean hasCachedData() {
        synchronized (syncLock) {
            return prefs.contains(KEY_CUSTOMERS);
        }
    }
    
    /**
     * Check if there are pending entries to sync
     */
    public boolean hasPendingEntries() {
        return !getPendingEntries().isEmpty();
    }
    
    /**
     * Clear all cached data
     */
    public void clearAll() {
        synchronized (syncLock) {
            prefs.edit().clear().apply();
        }
    }
    
    /**
     * Simple data class for pending service entries
     */
    public static class PendingServiceEntry {
        public String providerId;
        public String customerId;
        public long timestamp; // milliseconds
        public double quantity;
        public double amount;
        public boolean delivered;
        
        public PendingServiceEntry(String providerId, String customerId, long timestamp, 
                                  double quantity, double amount, boolean delivered) {
            this.providerId = providerId;
            this.customerId = customerId;
            this.timestamp = timestamp;
            this.quantity = quantity;
            this.amount = amount;
            this.delivered = delivered;
        }
        
        public ServiceEntry toServiceEntry() {
            ServiceEntry entry = new ServiceEntry(providerId, customerId, 
                new Timestamp(new Date(timestamp)), quantity, delivered);
            return entry;
        }
    }

    private void setPendingEntries(List<PendingServiceEntry> entries) {
        String json = gson.toJson(entries == null ? new ArrayList<>() : entries);
        prefs.edit().putString(KEY_PENDING_ENTRIES, json).apply();
    }

    private String pendingKey(PendingServiceEntry entry) {
        if (entry == null) return "";
        return safe(entry.providerId) + "|"
            + safe(entry.customerId) + "|"
            + entry.timestamp + "|"
            + entry.quantity + "|"
            + entry.amount + "|"
            + entry.delivered;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
