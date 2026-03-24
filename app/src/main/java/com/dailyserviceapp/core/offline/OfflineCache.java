package com.dailyserviceapp.core.offline;

import android.content.Context;
import android.content.SharedPreferences;

import com.dailyserviceapp.data.local.dao.CustomerDao;
import com.dailyserviceapp.data.local.entity.CustomerEntity;
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
 * Upgraded to use Room Database for Customer caching, improving performance and scale.
 * 
 * @author DailyDrop Team
 * @version 2.0
 */
public class OfflineCache {
    
    private static final String PREF_NAME = "offline_cache";
    private static final String KEY_PENDING_ENTRIES = "pending_entries";
    private static final String KEY_LAST_SYNC = "last_sync_time";
    
    private final SharedPreferences prefs;
    private final Gson gson;
    private final CustomerDao customerDao;
    private final Object syncLock = new Object();
    
    public OfflineCache(Context context, CustomerDao customerDao) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.customerDao = customerDao;
    }
    
    /**
     * Cache customer list for offline access into Room Database
     */
    public void cacheCustomers(List<Customer> customers) {
        synchronized (syncLock) {
            List<CustomerEntity> entities = new ArrayList<>();
            for (Customer c : customers) {
                CustomerEntity e = new CustomerEntity();
                e.id = c.getId() != null ? c.getId() : java.util.UUID.randomUUID().toString();
                e.name = c.getName();
                e.phone = c.getPhone();
                e.address = c.getAddress();
                e.serviceType = c.getServiceType();
                e.ratePerUnit = c.getRatePerUnit();
                e.defaultQuantity = c.getDefaultQuantity();
                e.lentAmount = c.getLentAmount();
                e.providerId = c.getProviderId();
                e.status = c.getStatus();
                e.onVacation = c.isOnVacation();
                if (c.getCreatedAt() != null) {
                    e.createdAtMillis = c.getCreatedAt().toDate().getTime();
                }
                entities.add(e);
            }
            
            customerDao.clearAll();
            customerDao.insertAll(entities);
            
            prefs.edit()
                .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                .apply();
        }
    }
    
    /**
     * Retrieve cached customers from Room Database
     */
    public List<Customer> getCachedCustomers() {
        synchronized (syncLock) {
            List<CustomerEntity> entities = customerDao.getAllCustomers();
            List<Customer> customers = new ArrayList<>();
            for (CustomerEntity e : entities) {
                Customer c = new Customer();
                c.setId(e.id);
                c.setName(e.name);
                c.setPhone(e.phone);
                c.setAddress(e.address);
                c.setServiceType(e.serviceType);
                c.setRatePerUnit(e.ratePerUnit);
                c.setDefaultQuantity(e.defaultQuantity);
                c.setLentAmount(e.lentAmount);
                c.setProviderId(e.providerId);
                c.setStatus(e.status);
                c.setOnVacation(e.onVacation);
                if (e.createdAtMillis > 0) {
                    c.setCreatedAt(new Timestamp(new Date(e.createdAtMillis)));
                } else {
                    c.setCreatedAt(new Timestamp(new Date()));
                }
                customers.add(c);
            }
            return customers;
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
            return prefs.contains(KEY_LAST_SYNC);
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
            customerDao.clearAll();
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
