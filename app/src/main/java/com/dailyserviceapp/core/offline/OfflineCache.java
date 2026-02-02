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
import java.util.List;

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
    
    /**
     * Creates an OfflineCache backed by the application's SharedPreferences.
     *
     * @param context the Android Context used to access the private SharedPreferences file for offline storage
     */
    public OfflineCache(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }
    
    /**
     * Persist the given customers for offline access and update the last-sync timestamp.
     *
     * Stores a serialized representation of the provided customer list in preferences and
     * records the current system time as the last successful sync.
     *
     * @param customers list of customers to cache; if empty the stored cache will be replaced with an empty list
     */
    public void cacheCustomers(List<Customer> customers) {
        String json = gson.toJson(customers);
        prefs.edit()
            .putString(KEY_CUSTOMERS, json)
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply();
    }
    
    /**
     * Retrieve the list of customers previously cached in SharedPreferences.
     *
     * @return the list of cached Customer objects, or an empty list if no cached customers exist
     */
    public List<Customer> getCachedCustomers() {
        String json = prefs.getString(KEY_CUSTOMERS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<Customer>>(){}.getType();
        return gson.fromJson(json, type);
    }
    
    /**
     * Adds a service entry to the offline pending queue and persists the updated queue.
     *
     * @param entry the PendingServiceEntry to enqueue for later synchronization
     */
    public void queuePendingEntry(PendingServiceEntry entry) {
        List<PendingServiceEntry> pending = getPendingEntries();
        pending.add(entry);
        String json = gson.toJson(pending);
        prefs.edit().putString(KEY_PENDING_ENTRIES, json).apply();
    }
    
    /**
     * Retrieve the list of pending service entries queued for synchronization.
     *
     * @return a list of PendingServiceEntry objects; an empty list if no entries are stored
     */
    public List<PendingServiceEntry> getPendingEntries() {
        String json = prefs.getString(KEY_PENDING_ENTRIES, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<PendingServiceEntry>>(){}.getType();
        return gson.fromJson(json, type);
    }
    
    /**
     * Remove all queued pending service entries from shared preferences.
     *
     * This deletes the stored JSON list under KEY_PENDING_ENTRIES so subsequent
     * calls to getPendingEntries() will return an empty list.
     */
    public void clearPendingEntries() {
        prefs.edit().remove(KEY_PENDING_ENTRIES).apply();
    }
    
    /**
     * Retrieve the timestamp of the last successful sync.
     *
     * @return the timestamp in milliseconds since the Unix epoch of the last successful sync, or 0 if none is recorded
     */
    public long getLastSyncTime() {
        return prefs.getLong(KEY_LAST_SYNC, 0);
    }
    
    /**
     * Determine whether cached customer data exists in the preferences.
     *
     * @return `true` if cached customer data (the customers key) is present, `false` otherwise.
     */
    public boolean hasCachedData() {
        return prefs.contains(KEY_CUSTOMERS);
    }
    
    /**
     * Determine whether any pending service entries are queued for synchronization.
     *
     * @return `true` if there is at least one pending entry, `false` otherwise.
     */
    public boolean hasPendingEntries() {
        return !getPendingEntries().isEmpty();
    }
    
    /**
     * Removes all entries stored in the offline cache SharedPreferences.
     */
    public void clearAll() {
        prefs.edit().clear().apply();
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
        
        /**
         * Create a PendingServiceEntry with the specified provider, customer, timestamp, quantity, amount, and delivery status.
         *
         * @param providerId the provider's identifier
         * @param customerId the customer's identifier
         * @param timestamp  the event time as epoch milliseconds
         * @param quantity   the quantity of service provided
         * @param amount     the monetary amount associated with the entry
         * @param delivered  `true` if the service was delivered, `false` otherwise
         */
        public PendingServiceEntry(String providerId, String customerId, long timestamp, 
                                  double quantity, double amount, boolean delivered) {
            this.providerId = providerId;
            this.customerId = customerId;
            this.timestamp = timestamp;
            this.quantity = quantity;
            this.amount = amount;
            this.delivered = delivered;
        }
        
        /**
         * Convert this PendingServiceEntry into a ServiceEntry.
         *
         * @return a ServiceEntry with the same providerId, customerId, quantity, and delivered flag,
         *         and a Timestamp created by converting this entry's millisecond `timestamp` to seconds
         *         (nanoseconds set to 0)
         */
        public ServiceEntry toServiceEntry() {
            ServiceEntry entry = new ServiceEntry(providerId, customerId, 
                new Timestamp(timestamp / 1000, 0), quantity, delivered);
            return entry;
        }
    }
}