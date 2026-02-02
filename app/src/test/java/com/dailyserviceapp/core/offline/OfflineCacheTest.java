package com.dailyserviceapp.core.offline;

import android.content.Context;
import android.content.SharedPreferences;

import com.dailyserviceapp.data.models.Customer;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for OfflineCache.
 * Tests caching functionality, pending entries queue, and data persistence.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class OfflineCacheTest {

    private OfflineCache offlineCache;
    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.application;
        offlineCache = new OfflineCache(context);
        // Clear any existing data
        offlineCache.clearAll();
    }

    @Test
    public void testCacheCustomers() {
        List<Customer> customers = createTestCustomers(3);

        offlineCache.cacheCustomers(customers);

        List<Customer> retrieved = offlineCache.getCachedCustomers();
        assertNotNull("Retrieved customers should not be null", retrieved);
        assertEquals("Should retrieve same number of customers", 3, retrieved.size());
    }

    @Test
    public void testGetCachedCustomersWhenEmpty() {
        List<Customer> retrieved = offlineCache.getCachedCustomers();

        assertNotNull("Should return empty list, not null", retrieved);
        assertEquals("Should return empty list", 0, retrieved.size());
    }

    @Test
    public void testCacheCustomersUpdatesLastSyncTime() {
        long beforeTime = System.currentTimeMillis();

        offlineCache.cacheCustomers(createTestCustomers(1));

        long lastSync = offlineCache.getLastSyncTime();
        assertTrue("Last sync time should be updated", lastSync >= beforeTime);
        assertTrue("Last sync time should not be in future", lastSync <= System.currentTimeMillis());
    }

    @Test
    public void testQueuePendingEntry() {
        OfflineCache.PendingServiceEntry entry = createTestPendingEntry();

        offlineCache.queuePendingEntry(entry);

        List<OfflineCache.PendingServiceEntry> pending = offlineCache.getPendingEntries();
        assertEquals("Should have one pending entry", 1, pending.size());
        assertEquals("Provider ID should match", "provider123", pending.get(0).providerId);
    }

    @Test
    public void testQueueMultiplePendingEntries() {
        offlineCache.queuePendingEntry(createTestPendingEntry());
        offlineCache.queuePendingEntry(createTestPendingEntry());
        offlineCache.queuePendingEntry(createTestPendingEntry());

        List<OfflineCache.PendingServiceEntry> pending = offlineCache.getPendingEntries();
        assertEquals("Should have three pending entries", 3, pending.size());
    }

    @Test
    public void testGetPendingEntriesWhenEmpty() {
        List<OfflineCache.PendingServiceEntry> pending = offlineCache.getPendingEntries();

        assertNotNull("Should return empty list, not null", pending);
        assertEquals("Should return empty list", 0, pending.size());
    }

    @Test
    public void testClearPendingEntries() {
        offlineCache.queuePendingEntry(createTestPendingEntry());
        offlineCache.queuePendingEntry(createTestPendingEntry());

        offlineCache.clearPendingEntries();

        List<OfflineCache.PendingServiceEntry> pending = offlineCache.getPendingEntries();
        assertEquals("Pending entries should be cleared", 0, pending.size());
    }

    @Test
    public void testHasCachedData() {
        assertFalse("Should not have cached data initially", offlineCache.hasCachedData());

        offlineCache.cacheCustomers(createTestCustomers(1));

        assertTrue("Should have cached data after caching", offlineCache.hasCachedData());
    }

    @Test
    public void testHasPendingEntries() {
        assertFalse("Should not have pending entries initially", offlineCache.hasPendingEntries());

        offlineCache.queuePendingEntry(createTestPendingEntry());

        assertTrue("Should have pending entries after queueing", offlineCache.hasPendingEntries());
    }

    @Test
    public void testClearAll() {
        offlineCache.cacheCustomers(createTestCustomers(2));
        offlineCache.queuePendingEntry(createTestPendingEntry());

        offlineCache.clearAll();

        assertFalse("Should not have cached data after clear", offlineCache.hasCachedData());
        assertFalse("Should not have pending entries after clear", offlineCache.hasPendingEntries());
        assertEquals("Last sync time should be 0", 0, offlineCache.getLastSyncTime());
    }

    @Test
    public void testPendingServiceEntryToServiceEntry() {
        OfflineCache.PendingServiceEntry pendingEntry = createTestPendingEntry();

        com.dailyserviceapp.data.models.ServiceEntry serviceEntry = pendingEntry.toServiceEntry();

        assertNotNull("ServiceEntry should not be null", serviceEntry);
        assertEquals("Provider ID should match", pendingEntry.providerId, serviceEntry.getProviderId());
        assertEquals("Customer ID should match", pendingEntry.customerId, serviceEntry.getCustomerId());
        assertEquals("Quantity should match", pendingEntry.quantity, serviceEntry.getQuantity(), 0.001);
        assertEquals("Delivered status should match", pendingEntry.delivered, serviceEntry.isDelivered());
    }

    @Test
    public void testCacheCustomersPreservesCustomerData() {
        List<Customer> original = createTestCustomers(1);
        Customer originalCustomer = original.get(0);
        originalCustomer.setName("John Doe");
        originalCustomer.setPhone("1234567890");
        originalCustomer.setServiceType("Milk");
        originalCustomer.setRatePerUnit(50.0);

        offlineCache.cacheCustomers(original);

        List<Customer> retrieved = offlineCache.getCachedCustomers();
        Customer retrievedCustomer = retrieved.get(0);

        assertEquals("Name should be preserved", "John Doe", retrievedCustomer.getName());
        assertEquals("Phone should be preserved", "1234567890", retrievedCustomer.getPhone());
        assertEquals("Service type should be preserved", "Milk", retrievedCustomer.getServiceType());
        assertEquals("Rate should be preserved", 50.0, retrievedCustomer.getRatePerUnit(), 0.001);
    }

    @Test
    public void testPendingEntryConstructor() {
        OfflineCache.PendingServiceEntry entry = new OfflineCache.PendingServiceEntry(
            "provider1", "customer1", 1000L, 2.5, 125.0, true
        );

        assertEquals("Provider ID should match", "provider1", entry.providerId);
        assertEquals("Customer ID should match", "customer1", entry.customerId);
        assertEquals("Timestamp should match", 1000L, entry.timestamp);
        assertEquals("Quantity should match", 2.5, entry.quantity, 0.001);
        assertEquals("Amount should match", 125.0, entry.amount, 0.001);
        assertTrue("Delivered should be true", entry.delivered);
    }

    @Test
    public void testGetLastSyncTimeWhenNeverSynced() {
        long lastSync = offlineCache.getLastSyncTime();
        assertEquals("Last sync time should be 0 when never synced", 0, lastSync);
    }

    @Test
    public void testCacheEmptyCustomerList() {
        offlineCache.cacheCustomers(new ArrayList<>());

        List<Customer> retrieved = offlineCache.getCachedCustomers();
        assertNotNull("Should not return null", retrieved);
        assertEquals("Should return empty list", 0, retrieved.size());
    }

    @Test
    public void testMultipleCacheOperationsOverwritePrevious() {
        offlineCache.cacheCustomers(createTestCustomers(3));
        offlineCache.cacheCustomers(createTestCustomers(5));

        List<Customer> retrieved = offlineCache.getCachedCustomers();
        assertEquals("Should have customers from latest cache operation", 5, retrieved.size());
    }

    // Helper methods
    private List<Customer> createTestCustomers(int count) {
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Customer customer = new Customer();
            customer.setName("Customer " + i);
            customer.setPhone("123456789" + i);
            customer.setServiceType("Milk");
            customer.setRatePerUnit(50.0);
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