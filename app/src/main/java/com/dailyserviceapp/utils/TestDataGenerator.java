package com.dailyserviceapp.utils;

import android.content.Context;

import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Test Data Generator for creating sample data in the app.
 * Use this to populate the database with test customers, service entries, and bills.
 */
public class TestDataGenerator {
    
    private final FirestoreRepository repository;
    private final String providerId;
    private final Context context;
    private final List<String> createdCustomerIds = new ArrayList<>();
    
    public TestDataGenerator(Context context, String providerId) {
        this.context = context;
        this.providerId = providerId;
        this.repository = new FirestoreRepository();
    }
    
    /**
     * Generate complete test dataset: customers + service entries for January 2026
     */
    public void generateCompleteTestData(OnTestDataGeneratedListener listener) {
        generateTestCustomers(new OnCustomersGeneratedListener() {
            @Override
            public void onCustomersGenerated(List<String> customerIds) {
                createdCustomerIds.addAll(customerIds);
                generateServiceEntriesForJanuary(customerIds, listener);
            }
            
            @Override
            public void onError(String error) {
                listener.onError("Failed to create customers: " + error);
            }
        });
    }
    
    /**
     * Create 5 test customers with different service types
     */
    private void generateTestCustomers(OnCustomersGeneratedListener listener) {
        String[][] customerData = {
            {"Rajesh Kumar", "9876543210", "House #123, MG Road, Bangalore", "Milk", "50"},
            {"Priya Sharma", "9876543211", "Flat 45, Koramangala, Bangalore", "Newspaper", "30"},
            {"Amit Patel", "9876543212", "Villa 12, Whitefield, Bangalore", "Milk", "60"},
            {"Sneha Reddy", "9876543213", "Apartment 78, Indiranagar, Bangalore", "Milk", "45"},
            {"Vijay Singh", "9876543214", "House 90, Jayanagar, Bangalore", "Newspaper", "25"}
        };
        
        List<String> customerIds = new ArrayList<>();
        final int[] completed = {0};
        
        for (String[] data : customerData) {
            Customer customer = new Customer(
                data[0], // name
                data[1], // phone  
                data[2], // address
                data[3], // serviceType
                Double.parseDouble(data[4]), // ratePerUnit
                Timestamp.now() // createdAt
            );
            customer.setProviderId(providerId);
            customer.setStatus("ACTIVE");
            
            repository.addCustomer(customer,
                    ref -> {
                        String customerId = ref.getId();
                        customerIds.add(customerId);
                        completed[0]++;
                        
                        if (completed[0] == customerData.length) {
                            listener.onCustomersGenerated(customerIds);
                        }
                    },
                    e -> listener.onError(e.getMessage())
            );
        }
    }
    
    /**
     * Create service entries for each customer for January 2026
     * Varying patterns: some daily, some alternate days
     */
    private void generateServiceEntriesForJanuary(List<String> customerIds, OnTestDataGeneratedListener listener) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2026, Calendar.JANUARY, 1, 8, 0, 0); // Start from Jan 1, 2026
        
        List<ServiceEntry> allEntries = new ArrayList<>();
        
        // Customer 0 (Rajesh): Daily entries (28 days)
        for (int day = 1; day <= 28; day++) {
            calendar.set(Calendar.DAY_OF_MONTH, day);
            allEntries.add(createServiceEntry(customerIds.get(0), calendar.getTime(), 2.0));
        }
        
        // Customer 1 (Priya): 25 days (missed 3 days)
        for (int day = 1; day <= 28; day++) {
            if (day != 7 && day != 15 && day != 23) { // Skip 3 days
                calendar.set(Calendar.DAY_OF_MONTH, day);
                allEntries.add(createServiceEntry(customerIds.get(1), calendar.getTime(), 1.0));
            }
        }
        
        // Customer 2 (Amit): 26 days (missed 2 days)
        for (int day = 1; day <= 28; day++) {
            if (day != 10 && day != 20) { // Skip 2 days
                calendar.set(Calendar.DAY_OF_MONTH, day);
                allEntries.add(createServiceEntry(customerIds.get(2), calendar.getTime(), 2.5));
            }
        }
        
        // Customer 3 (Sneha): 20 days (alternate + some gaps)
        for (int day = 1; day <= 28; day += 2) { // Every alternate day
            calendar.set(Calendar.DAY_OF_MONTH, day);
            allEntries.add(createServiceEntry(customerIds.get(3), calendar.getTime(), 1.5));
        }
        
        // Customer 4 (Vijay): 24 days
        for (int day = 1; day <= 28; day++) {
            if (day % 7 != 0) { // Skip every 7th day (Sundays)
                calendar.set(Calendar.DAY_OF_MONTH, day);
                allEntries.add(createServiceEntry(customerIds.get(4), calendar.getTime(), 1.0));
            }
        }
        
        // Save all entries
        saveServiceEntries(allEntries, 0, listener);
    }
    
    private ServiceEntry createServiceEntry(String customerId, Date date, double quantity) {
        ServiceEntry entry = new ServiceEntry();
        entry.setProviderId(providerId);
        entry.setCustomerId(customerId);
        entry.setDate(new Timestamp(date));
        entry.setQuantity(quantity);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        entry.setNotes("Test delivery - " + sdf.format(date));
        entry.setCreatedAt(Timestamp.now());
        return entry;
    }
    
    private void saveServiceEntries(List<ServiceEntry> entries, int index, OnTestDataGeneratedListener listener) {
        if (index >= entries.size()) {
            listener.onTestDataGenerated(createdCustomerIds, entries.size());
            return;
        }
        
        repository.saveServiceEntry(entries.get(index), new FirestoreRepository.OnSaveCompleteListener() {
            @Override
            public void onSuccess() {
                // Continue with next entry
                saveServiceEntries(entries, index + 1, listener);
            }
            
            @Override
            public void onError(String error) {
                listener.onError("Failed to save entry " + (index + 1) + ": " + error);
            }
        });
    }
    
    public interface OnCustomersGeneratedListener {
        void onCustomersGenerated(List<String> customerIds);
        void onError(String error);
    }
    
    public interface OnTestDataGeneratedListener {
        void onTestDataGenerated(List<String> customerIds, int entriesCount);
        void onError(String error);
    }
}
