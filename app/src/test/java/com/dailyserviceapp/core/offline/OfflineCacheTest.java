package com.dailyserviceapp.core.offline;

import android.content.Context;
import android.content.SharedPreferences;

import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.ServiceEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for OfflineCache.
 * Tests customer caching, pending entry queue management, sync time tracking,
 * and data persistence.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class OfflineCacheTest {

    private OfflineCache offlineCache;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        offlineCache = new OfflineCache(context);
        offlineCache.clearAll();
    }

    @Test
    public void testOfflineCacheCreation() {
        assertNotNull("OfflineCache should be created", offlineCache);
    }

    @Test
    public void testCacheCustomersEmpty() {
        List<Customer> customers = new ArrayList<>();

        offlineCache.cacheCustomers(customers);

        List<Customer> cached = offlineCache.getCachedCustomers();
        assertNotNull("Cached customers should not be null", cached);
        assertEquals("Cached customers should be empty", 0, cached.size());
    }

    @Test
    public void testCacheCustomersSingle() {
        Customer customer = createCustomer("1", "John Doe", "john@example.com");
        List<Customer> customers = Arrays.asList(customer);

        offlineCache.cacheCustomers(customers);

        List<Customer> cached = offlineCache.getCachedCustomers();
        assertEquals("Should cache one customer", 1, cached.size());
        assertEquals("Customer name should match", "John Doe", cached.get(0).getName());
        assertEquals("Customer email should match", "john@example.com", cached.get(0).getEmail());
    }

    @Test
    public void testCacheCustomersMultiple() {
        List<Customer> customers = Arrays.asList(
            createCustomer("1", "John Doe", "john@example.com"),
            createCustomer("2", "Jane Smith", "jane@example.com"),
            createCustomer("3", "Bob Johnson", "bob@example.com")
        );

        offlineCache.cacheCustomers(customers);

        List<Customer> cached = offlineCache.getCachedCustomers();
        assertEquals("Should cache three customers", 3, cached.size());
    }

    @Test
    public void testCacheCustomersOverwrite() {
        // Cache first set
        List<Customer> customers1 = Arrays.asList(
            createCustomer("1", "John Doe", "john@example.com")
        );
        offlineCache.cacheCustomers(customers1);

        // Cache second set (should overwrite)
        List<Customer> customers2 = Arrays.asList(
            createCustomer("2", "Jane Smith", "jane@example.com"),
            createCustomer("3", "Bob Johnson", "bob@example.com")
        );
        offlineCache.cacheCustomers(customers2);

        List<Customer> cached = offlineCache.getCachedCustomers();
        assertEquals("Should have only second set", 2, cached.size());
        assertEquals("First customer should be from second set", "Jane Smith", cached.get(0).getName());
    }

    @Test
    public void testGetCachedCustomersWhenEmpty() {
        List<Customer> cached = offlineCache.getCachedCustomers();

        assertNotNull("Should return empty list, not null", cached);
        assertEquals("Should return empty list", 0, cached.size());
    }

    @Test
    public void testQueuePendingEntrySingle() {
        OfflineCache.PendingServiceEntry entry = new OfflineCache.PendingServiceEntry(
            "provider1", "customer1", System.currentTimeMillis(), 2.0, 100.0, true
        );

        offlineCache.queuePendingEntry(entry);

        List<OfflineCache.PendingServiceEntry> pending = offlineCache.getPendingEntries();
        assertEquals("Should have one pending entry", 1, pending.size());
        assertEquals("Provider ID should match", "provider1", pending.get(0).providerId);
        assertEquals("Customer ID should match", "customer1", pending.get(0).customerId);
    }

    @Test
    public void testQueuePendingEntriesMultiple() {
        OfflineCache.PendingServiceEntry entry1 = new OfflineCache.PendingServiceEntry(
            "provider1", "customer1", System.currentTimeMillis(), 2.0, 100.0, true
        );
        OfflineCache.PendingServiceEntry entry2 = new OfflineCache.PendingServiceEntry(
            "provider1", "customer2", System.currentTimeMillis(), 3.0, 150.0, false
        );

        offlineCache.queuePendingEntry(entry1);
        offlineCache.queuePendingEntry(entry2);

        List<OfflineCache.PendingServiceEntry> pending = offlineCache.getPendingEntries();
        assertEquals("Should have two pending entries", 2, pending.size());
    }

    @Test
    public void testGetPendingEntriesWhenEmpty() {
        List<OfflineCache.PendingServiceEntry> pending = offlineCache.getPendingEntries();

        assertNotNull("Should return empty list, not null", pending);
        assertEquals("Should return empty list", 0, pending.size());
    }

    @Test
    public void testClearPendingEntries() {
        OfflineCache.PendingServiceEntry entry = new OfflineCache.PendingServiceEntry(
            "provider1", "customer1", System.currentTimeMillis(), 2.0, 100.0, true
        );
        offlineCache.queuePendingEntry(entry);

        assertEquals("Should have one entry before clear", 1, offlineCache.getPendingEntries().size());

        offlineCache.clearPendingEntries();

        assertEquals("Should have no entries after clear", 0, offlineCache.getPendingEntries().size());
    }

    @Test
    public void testHasPendingEntriesTrue() {
        OfflineCache.PendingServiceEntry entry = new OfflineCache.PendingServiceEntry(
            "provider1", "customer1", System.currentTimeMillis(), 2.0, 100.0, true
        );
        offlineCache.queuePendingEntry(entry);

        assertTrue("Should have pending entries", offlineCache.hasPendingEntries());
    }

    @Test
    public void testHasPendingEntriesFalse() {
        assertFalse("Should not have pending entries", offlineCache.hasPendingEntries());
    }

    @Test
    public void testHasCachedDataTrue() {
        List<Customer> customers = Arrays.asList(
            createCustomer("1", "John Doe", "john@example.com")
        );
        offlineCache.cacheCustomers(customers);

        assertTrue("Should have cached data", offlineCache.hasCachedData());
    }

    @Test
    public void testHasCachedDataFalse() {
        assertFalse("Should not have cached data", offlineCache.hasCachedData());
    }

    @Test
    public void testGetLastSyncTime() {
        long beforeCache = System.currentTimeMillis();

        List<Customer> customers = Arrays.asList(
            createCustomer("1", "John Doe", "john@example.com")
        );
        offlineCache.cacheCustomers(customers);

        long syncTime = offlineCache.getLastSyncTime();
        long afterCache = System.currentTimeMillis();

        assertTrue("Sync time should be after or equal to before time", syncTime >= beforeCache);
        assertTrue("Sync time should be before or equal to after time", syncTime <= afterCache);
    }

    @Test
    public void testGetLastSyncTimeWhenNoCache() {
        long syncTime = offlineCache.getLastSyncTime();

        assertEquals("Sync time should be 0 when no cache", 0, syncTime);
    }

    @Test
    public void testClearAll() {
        // Add data
        List<Customer> customers = Arrays.asList(
            createCustomer("1", "John Doe", "john@example.com")
        );
        offlineCache.cacheCustomers(customers);

        OfflineCache.PendingServiceEntry entry = new OfflineCache.PendingServiceEntry(
            "provider1", "customer1", System.currentTimeMillis(), 2.0, 100.0, true
        );
        offlineCache.queuePendingEntry(entry);

        assertTrue("Should have cached data before clear", offlineCache.hasCachedData());
        assertTrue("Should have pending entries before clear", offlineCache.hasPendingEntries());

        // Clear all
        offlineCache.clearAll();

        assertFalse("Should not have cached data after clear", offlineCache.hasCachedData());
        assertFalse("Should not have pending entries after clear", offlineCache.hasPendingEntries());
        assertEquals("Sync time should be 0 after clear", 0, offlineCache.getLastSyncTime());
    }

    @Test
    public void testPendingEntryToServiceEntry() {
        long timestamp = System.currentTimeMillis();
        OfflineCache.PendingServiceEntry pendingEntry = new OfflineCache.PendingServiceEntry(
            "provider1", "customer1", timestamp, 2.5, 125.0, true
        );

        ServiceEntry serviceEntry = pendingEntry.toServiceEntry();

        assertNotNull("Service entry should not be null", serviceEntry);
        assertEquals("Provider ID should match", "provider1", serviceEntry.getProviderId());
        assertEquals("Customer ID should match", "customer1", serviceEntry.getCustomerId());
        assertEquals("Quantity should match", 2.5, serviceEntry.getQuantity(), 0.001);
        assertTrue("Delivered status should match", serviceEntry.isDelivered());
    }

    @Test
    public void testPendingEntryWithZeroQuantity() {
        OfflineCache.PendingServiceEntry entry = new OfflineCache.PendingServiceEntry(
            "provider1", "customer1", System.currentTimeMillis(), 0.0, 0.0, false
        );

        offlineCache.queuePendingEntry(entry);

        List<OfflineCache.PendingServiceEntry> pending = offlineCache.getPendingEntries();
        assertEquals("Should handle zero quantity", 0.0, pending.get(0).quantity, 0.001);
        assertEquals("Should handle zero amount", 0.0, pending.get(0).amount, 0.001);
    }

    @Test
    public void testPendingEntryWithNegativeValues() {
        OfflineCache.PendingServiceEntry entry = new OfflineCache.PendingServiceEntry(
            "provider1", "customer1", System.currentTimeMillis(), -1.0, -50.0, true
        );

        offlineCache.queuePendingEntry(entry);

        List<OfflineCache.PendingServiceEntry> pending = offlineCache.getPendingEntries();
        assertEquals("Should handle negative quantity", -1.0, pending.get(0).quantity, 0.001);
        assertEquals("Should handle negative amount", -50.0, pending.get(0).amount, 0.001);
    }

    @Test
    public void testPendingEntryWithLargeValues() {
        OfflineCache.PendingServiceEntry entry = new OfflineCache.PendingServiceEntry(
            "provider1", "customer1", System.currentTimeMillis(), 999999.99, 999999.99, true
        );

        offlineCache.queuePendingEntry(entry);

        List<OfflineCache.PendingServiceEntry> pending = offlineCache.getPendingEntries();
        assertEquals("Should handle large quantity", 999999.99, pending.get(0).quantity, 0.001);
        assertEquals("Should handle large amount", 999999.99, pending.get(0).amount, 0.001);
    }

    @Test
    public void testMultipleCacheOperations() {
        // Operation 1: Cache customers
        offlineCache.cacheCustomers(Arrays.asList(
            createCustomer("1", "John", "john@test.com")
        ));

        // Operation 2: Queue entry
        offlineCache.queuePendingEntry(new OfflineCache.PendingServiceEntry(
            "p1", "c1", System.currentTimeMillis(), 1.0, 50.0, true
        ));

        // Operation 3: Cache more customers
        offlineCache.cacheCustomers(Arrays.asList(
            createCustomer("2", "Jane", "jane@test.com"),
            createCustomer("3", "Bob", "bob@test.com")
        ));

        // Operation 4: Queue more entries
        offlineCache.queuePendingEntry(new OfflineCache.PendingServiceEntry(
            "p1", "c2", System.currentTimeMillis(), 2.0, 100.0, false
        ));

        assertEquals("Should have 2 customers (overwritten)", 2, offlineCache.getCachedCustomers().size());
        assertEquals("Should have 2 pending entries (accumulated)", 2, offlineCache.getPendingEntries().size());
    }

    @Test
    public void testCacheCustomersWithSpecialCharacters() {
        List<Customer> customers = Arrays.asList(
            createCustomer("1", "José García", "jose@example.com"),
            createCustomer("2", "François Müller", "francois@example.com"),
            createCustomer("3", "李明", "li@example.com")
        );

        offlineCache.cacheCustomers(customers);

        List<Customer> cached = offlineCache.getCachedCustomers();
        assertEquals("Should cache customers with special characters", 3, cached.size());
        assertEquals("Should preserve special characters", "José García", cached.get(0).getName());
    }

    @Test
    public void testPendingEntryDeliveredStatus() {
        OfflineCache.PendingServiceEntry deliveredEntry = new OfflineCache.PendingServiceEntry(
            "p1", "c1", System.currentTimeMillis(), 1.0, 50.0, true
        );
        OfflineCache.PendingServiceEntry notDeliveredEntry = new OfflineCache.PendingServiceEntry(
            "p1", "c2", System.currentTimeMillis(), 1.0, 50.0, false
        );

        offlineCache.queuePendingEntry(deliveredEntry);
        offlineCache.queuePendingEntry(notDeliveredEntry);

        List<OfflineCache.PendingServiceEntry> pending = offlineCache.getPendingEntries();
        assertTrue("First entry should be delivered", pending.get(0).delivered);
        assertFalse("Second entry should not be delivered", pending.get(1).delivered);
    }

    @Test
    public void testCachePersistence() {
        // Cache data in first instance
        offlineCache.cacheCustomers(Arrays.asList(
            createCustomer("1", "John Doe", "john@example.com")
        ));

        // Create new instance (simulates app restart)
        OfflineCache newCache = new OfflineCache(context);

        List<Customer> cached = newCache.getCachedCustomers();
        assertEquals("Data should persist across instances", 1, cached.size());
        assertEquals("Customer data should match", "John Doe", cached.get(0).getName());

        newCache.clearAll();
    }

    @Test
    public void testPendingEntriesPersistence() {
        offlineCache.queuePendingEntry(new OfflineCache.PendingServiceEntry(
            "p1", "c1", System.currentTimeMillis(), 1.0, 50.0, true
        ));

        // Create new instance
        OfflineCache newCache = new OfflineCache(context);

        List<OfflineCache.PendingServiceEntry> pending = newCache.getPendingEntries();
        assertEquals("Pending entries should persist", 1, pending.size());

        newCache.clearAll();
    }

    @Test
    public void testLargeDatasetCache() {
        List<Customer> largeCustomerList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeCustomerList.add(createCustomer(
                String.valueOf(i),
                "Customer " + i,
                "customer" + i + "@example.com"
            ));
        }

        offlineCache.cacheCustomers(largeCustomerList);

        List<Customer> cached = offlineCache.getCachedCustomers();
        assertEquals("Should cache large dataset", 1000, cached.size());
    }

    @Test
    public void testManyPendingEntries() {
        for (int i = 0; i < 100; i++) {
            offlineCache.queuePendingEntry(new OfflineCache.PendingServiceEntry(
                "provider1", "customer" + i, System.currentTimeMillis(), 1.0, 50.0, true
            ));
        }

        List<OfflineCache.PendingServiceEntry> pending = offlineCache.getPendingEntries();
        assertEquals("Should queue many pending entries", 100, pending.size());
    }

    // Helper method
    private Customer createCustomer(String id, String name, String email) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName(name);
        customer.setEmail(email);
        customer.setPhone("1234567890");
        customer.setProviderId("provider1");
        customer.setServiceType("Milk");
        customer.setRatePerUnit(50.0);
        return customer;
    }
}