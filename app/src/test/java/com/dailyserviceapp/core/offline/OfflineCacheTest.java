package com.dailyserviceapp.core.offline;

import android.content.Context;
import android.content.SharedPreferences;

import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for OfflineCache.
 * Tests offline data caching and synchronization functionality.
 */
@RunWith(RobolectricTestRunner.class)
public class OfflineCacheTest {

    private OfflineCache offlineCache;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        offlineCache = new OfflineCache(context);
        // Clear any existing cache
        offlineCache.clearAll();
    }

    @Test
    public void testOfflineCacheCreation() {
        assertNotNull("OfflineCache should be created", offlineCache);
    }

    @Test
    public void testInitiallyHasNoCachedData() {
        assertFalse("Should not have cached data initially", offlineCache.hasCachedData());
    }

    @Test
    public void testInitiallyHasNoPendingEntries() {
        assertFalse("Should not have pending entries initially", offlineCache.hasPendingEntries());
    }

    @Test
    public void testCacheCustomers() {
        List<Customer> customers = createTestCustomers(3);

        offlineCache.cacheCustomers(customers);

        assertTrue("Should have cached data after caching", offlineCache.hasCachedData());
    }

    @Test
    public void testGetCachedCustomers() {
        List<Customer> customers = createTestCustomers(3);
        customers.get(0).setName("Alice");
        customers.get(1).setName("Bob");
        customers.get(2).setName("Charlie");

        offlineCache.cacheCustomers(customers);

        List<Customer> cachedCustomers = offlineCache.getCachedCustomers();

        assertNotNull("Cached customers should not be null", cachedCustomers);
        assertEquals("Should return same number of customers", 3, cachedCustomers.size());
        assertEquals("First customer name should match", "Alice", cachedCustomers.get(0).getName());
        assertEquals("Second customer name should match", "Bob", cachedCustomers.get(1).getName());
        assertEquals("Third customer name should match", "Charlie", cachedCustomers.get(2).getName());
    }

    @Test
    public void testGetCachedCustomersWhenEmpty() {
        List<Customer> cachedCustomers = offlineCache.getCachedCustomers();

        assertNotNull("Should return empty list, not null", cachedCustomers);
        assertEquals("Should return empty list", 0, cachedCustomers.size());
    }

    @Test
    public void testCacheEmptyCustomerList() {
        List<Customer> emptyList = new ArrayList<>();

        offlineCache.cacheCustomers(emptyList);

        List<Customer> cachedCustomers = offlineCache.getCachedCustomers();
        assertEquals("Should cache empty list", 0, cachedCustomers.size());
    }

    @Test
    public void testQueuePendingEntry() {
        OfflineCache.PendingServiceEntry entry = createTestPendingEntry();

        offlineCache.queuePendingEntry(entry);

        assertTrue("Should have pending entries after queuing", offlineCache.hasPendingEntries());
    }

    @Test
    public void testGetPendingEntries() {
        OfflineCache.PendingServiceEntry entry1 = createTestPendingEntry();
        OfflineCache.PendingServiceEntry entry2 = createTestPendingEntry();

        offlineCache.queuePendingEntry(entry1);
        offlineCache.queuePendingEntry(entry2);

        List<OfflineCache.PendingServiceEntry> pendingEntries = offlineCache.getPendingEntries();

        assertNotNull("Pending entries should not be null", pendingEntries);
        assertEquals("Should have 2 pending entries", 2, pendingEntries.size());
    }

    @Test
    public void testGetPendingEntriesWhenEmpty() {
        List<OfflineCache.PendingServiceEntry> pendingEntries = offlineCache.getPendingEntries();

        assertNotNull("Should return empty list, not null", pendingEntries);
        assertEquals("Should return empty list", 0, pendingEntries.size());
    }

    @Test
    public void testClearPendingEntries() {
        OfflineCache.PendingServiceEntry entry = createTestPendingEntry();
        offlineCache.queuePendingEntry(entry);

        assertTrue("Should have pending entries initially", offlineCache.hasPendingEntries());

        offlineCache.clearPendingEntries();

        assertFalse("Should not have pending entries after clearing", offlineCache.hasPendingEntries());
    }

    @Test
    public void testLastSyncTimeInitiallyZero() {
        long lastSync = offlineCache.getLastSyncTime();
        assertEquals("Last sync time should be 0 initially", 0, lastSync);
    }

    @Test
    public void testLastSyncTimeUpdatedOnCache() {
        long beforeCache = System.currentTimeMillis();

        List<Customer> customers = createTestCustomers(1);
        offlineCache.cacheCustomers(customers);

        long lastSync = offlineCache.getLastSyncTime();

        assertTrue("Last sync time should be updated", lastSync >= beforeCache);
    }

    @Test
    public void testClearAllRemovesEverything() {
        // Add some data
        List<Customer> customers = createTestCustomers(2);
        offlineCache.cacheCustomers(customers);

        OfflineCache.PendingServiceEntry entry = createTestPendingEntry();
        offlineCache.queuePendingEntry(entry);

        assertTrue("Should have cached data", offlineCache.hasCachedData());
        assertTrue("Should have pending entries", offlineCache.hasPendingEntries());

        // Clear all
        offlineCache.clearAll();

        assertFalse("Should not have cached data after clearAll", offlineCache.hasCachedData());
        assertFalse("Should not have pending entries after clearAll", offlineCache.hasPendingEntries());
        assertEquals("Last sync time should be 0 after clearAll", 0, offlineCache.getLastSyncTime());
    }

    @Test
    public void testPendingServiceEntryCreation() {
        String providerId = "provider123";
        String customerId = "customer456";
        long timestamp = System.currentTimeMillis();
        double quantity = 2.5;
        double amount = 50.0;
        boolean delivered = true;

        OfflineCache.PendingServiceEntry entry = new OfflineCache.PendingServiceEntry(
            providerId, customerId, timestamp, quantity, amount, delivered
        );

        assertNotNull("Entry should be created", entry);
        assertEquals("Provider ID should match", providerId, entry.providerId);
        assertEquals("Customer ID should match", customerId, entry.customerId);
        assertEquals("Timestamp should match", timestamp, entry.timestamp);
        assertEquals("Quantity should match", quantity, entry.quantity, 0.01);
        assertEquals("Amount should match", amount, entry.amount, 0.01);
        assertEquals("Delivered should match", delivered, entry.delivered);
    }

    @Test
    public void testPendingEntryToServiceEntry() {
        String providerId = "provider123";
        String customerId = "customer456";
        long timestamp = System.currentTimeMillis();
        double quantity = 2.5;
        double amount = 50.0;
        boolean delivered = true;

        OfflineCache.PendingServiceEntry pendingEntry = new OfflineCache.PendingServiceEntry(
            providerId, customerId, timestamp, quantity, amount, delivered
        );

        ServiceEntry serviceEntry = pendingEntry.toServiceEntry();

        assertNotNull("Service entry should be created", serviceEntry);
        assertEquals("Provider ID should match", providerId, serviceEntry.getProviderId());
        assertEquals("Customer ID should match", customerId, serviceEntry.getCustomerId());
        assertEquals("Quantity should match", quantity, serviceEntry.getQuantity(), 0.01);
        assertEquals("Delivered should match", delivered, serviceEntry.isDelivered());
    }

    @Test
    public void testMultiplePendingEntriesPreserveOrder() {
        OfflineCache.PendingServiceEntry entry1 = new OfflineCache.PendingServiceEntry(
            "p1", "c1", 1000, 1.0, 10.0, true
        );
        OfflineCache.PendingServiceEntry entry2 = new OfflineCache.PendingServiceEntry(
            "p2", "c2", 2000, 2.0, 20.0, false
        );
        OfflineCache.PendingServiceEntry entry3 = new OfflineCache.PendingServiceEntry(
            "p3", "c3", 3000, 3.0, 30.0, true
        );

        offlineCache.queuePendingEntry(entry1);
        offlineCache.queuePendingEntry(entry2);
        offlineCache.queuePendingEntry(entry3);

        List<OfflineCache.PendingServiceEntry> pending = offlineCache.getPendingEntries();

        assertEquals("Should have 3 entries", 3, pending.size());
        assertEquals("First entry should match", "p1", pending.get(0).providerId);
        assertEquals("Second entry should match", "p2", pending.get(1).providerId);
        assertEquals("Third entry should match", "p3", pending.get(2).providerId);
    }

    @Test
    public void testCacheOverwritesPreviousData() {
        List<Customer> customers1 = createTestCustomers(3);
        customers1.get(0).setName("Alice");

        offlineCache.cacheCustomers(customers1);

        List<Customer> customers2 = createTestCustomers(2);
        customers2.get(0).setName("Bob");

        offlineCache.cacheCustomers(customers2);

        List<Customer> cached = offlineCache.getCachedCustomers();

        assertEquals("Should have new count", 2, cached.size());
        assertEquals("Should have new name", "Bob", cached.get(0).getName());
    }

    @Test
    public void testCustomerDataPersistence() {
        Customer customer = new Customer("provider1", "Test Customer", "1234567890",
            "Test Address", "Milk", 50.0);
        customer.setId("customer1");

        List<Customer> customers = new ArrayList<>();
        customers.add(customer);

        offlineCache.cacheCustomers(customers);

        List<Customer> cached = offlineCache.getCachedCustomers();

        assertEquals("Customer ID should persist", "customer1", cached.get(0).getId());
        assertEquals("Customer name should persist", "Test Customer", cached.get(0).getName());
        assertEquals("Customer phone should persist", "1234567890", cached.get(0).getPhone());
        assertEquals("Customer address should persist", "Test Address", cached.get(0).getAddress());
        assertEquals("Customer service type should persist", "Milk", cached.get(0).getServiceType());
        assertEquals("Customer rate should persist", 50.0, cached.get(0).getRatePerUnit(), 0.01);
    }

    @Test
    public void testPendingEntryDoubleValues() {
        OfflineCache.PendingServiceEntry entry = new OfflineCache.PendingServiceEntry(
            "p1", "c1", 1000, 2.75, 137.50, true
        );

        offlineCache.queuePendingEntry(entry);

        List<OfflineCache.PendingServiceEntry> pending = offlineCache.getPendingEntries();

        assertEquals("Quantity should preserve decimals", 2.75, pending.get(0).quantity, 0.001);
        assertEquals("Amount should preserve decimals", 137.50, pending.get(0).amount, 0.001);
    }

    @Test
    public void testHasCachedDataAfterCachingEmptyList() {
        offlineCache.cacheCustomers(new ArrayList<>());

        // Even empty list is cached data
        assertTrue("Should have cached data even if empty", offlineCache.hasCachedData());
    }

    @Test
    public void testPendingEntryTimestampPrecision() {
        long exactTimestamp = 1672531200000L; // Specific timestamp

        OfflineCache.PendingServiceEntry entry = new OfflineCache.PendingServiceEntry(
            "p1", "c1", exactTimestamp, 1.0, 10.0, true
        );

        offlineCache.queuePendingEntry(entry);

        List<OfflineCache.PendingServiceEntry> pending = offlineCache.getPendingEntries();

        assertEquals("Timestamp should be exact", exactTimestamp, pending.get(0).timestamp);
    }

    @Test
    public void testClearPendingEntriesDoesNotAffectCachedCustomers() {
        List<Customer> customers = createTestCustomers(2);
        offlineCache.cacheCustomers(customers);

        OfflineCache.PendingServiceEntry entry = createTestPendingEntry();
        offlineCache.queuePendingEntry(entry);

        offlineCache.clearPendingEntries();

        assertTrue("Should still have cached customers", offlineCache.hasCachedData());
        assertEquals("Should still have 2 cached customers", 2,
            offlineCache.getCachedCustomers().size());
    }

    @Test
    public void testMultipleOfflineCacheInstances() {
        OfflineCache cache1 = new OfflineCache(context);
        OfflineCache cache2 = new OfflineCache(context);

        List<Customer> customers = createTestCustomers(1);
        cache1.cacheCustomers(customers);

        // Data should be accessible from second instance
        assertTrue("Second instance should see cached data", cache2.hasCachedData());
        assertEquals("Second instance should get same data", 1,
            cache2.getCachedCustomers().size());
    }

    // Helper methods
    private List<Customer> createTestCustomers(int count) {
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Customer customer = new Customer(
                "provider" + i,
                "Customer " + i,
                "123456789" + i,
                "Address " + i,
                "Milk",
                50.0
            );
            customer.setId("customer" + i);
            customers.add(customer);
        }
        return customers;
    }

    private OfflineCache.PendingServiceEntry createTestPendingEntry() {
        return new OfflineCache.PendingServiceEntry(
            "provider123",
            "customer456",
            System.currentTimeMillis(),
            2.0,
            100.0,
            true
        );
    }
}